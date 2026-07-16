<template>
  <section class="page">
    <div class="page-title">
      <strong>关联规则</strong>
      <span>Apriori 规则与热力图</span>
    </div>

    <el-card shadow="never" class="toolbar-card">
      <el-form inline label-width="86px">
        <el-form-item label="数据操作">
          <el-button type="primary" @click="loadData">载入数据</el-button>
          <el-upload :show-file-list="false" accept=".csv" :before-upload="handleUpload">
            <el-button>上传 CSV</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="支持度">
          <el-input-number v-model="minSupport" :min="0.05" :max="1" :step="0.05" />
        </el-form-item>
        <el-form-item label="置信度">
          <el-input-number v-model="minConfidence" :min="0.05" :max="1" :step="0.05" />
        </el-form-item>
        <el-form-item>
          <el-button type="success" @click="analyze">分析数据</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="two-column">
      <el-card shadow="never" class="work-card">
        <template #header>
          <div class="card-header">
            <strong>交易数据表格</strong>
            <el-tag type="info">{{ transactions.length }} 条</el-tag>
          </div>
        </template>
        <el-table :data="transactionRows" height="360" border size="small">
          <el-table-column prop="id" label="TID" width="80" />
          <el-table-column prop="itemsText" label="商品项" min-width="240" show-overflow-tooltip />
        </el-table>
      </el-card>

      <el-card shadow="never" class="work-card">
        <template #header>
          <div class="card-header">
            <strong>2 阶关联规则置信度热力图</strong>
            <el-tag type="info">
              minSupport={{ minSupport }}，minConfidence={{ minConfidence }}
            </el-tag>
          </div>
        </template>
        <div ref="chartRef" class="chart association-chart"></div>
      </el-card>
    </div>

    <el-card shadow="never" class="work-card">
      <template #header>
        <div class="card-header">
          <strong>关联规则结果</strong>
          <el-tag type="success">{{ twoItemRules.length }} 条规则</el-tag>
        </div>
      </template>
      <el-table :data="twoItemRules" border size="small" height="300">
        <el-table-column label="前项" min-width="180">
          <template #default="{ row }">{{ row.left.join('，') }}</template>
        </el-table-column>
        <el-table-column label="后项" min-width="180">
          <template #default="{ row }">{{ row.right.join('，') }}</template>
        </el-table-column>
        <el-table-column prop="support" label="支持度" width="110" />
        <el-table-column prop="confidence" label="置信度" width="110" />
        <el-table-column prop="lift" label="提升度" width="110" />
      </el-table>
    </el-card>
  </section>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { fetchTransactions, runAssociation, uploadTransactions } from '../api/request'

const transactions = ref([])
const rules = ref([])
const frequentPairs = ref([])
const minSupport = ref(0.2)
const minConfidence = ref(0.6)
const chartRef = ref(null)
let chart = null

// 把后端返回的二维数组事务转换成表格行，TID 从 101 开始用于贴近样例数据。
const transactionRows = computed(() => transactions.value.map((items, index) => ({
  id: 101 + index,
  itemsText: items.join('，'),
})))

// 当前热力图只展示一对一规则，因此过滤掉非二项规则。
const twoItemRules = computed(() => (
  rules.value.filter((rule) => rule.left.length === 1 && rule.right.length === 1)
))

// 从频繁二项集中收集所有商品项，并使用中文排序保证坐标轴顺序稳定。
const heatmapItems = computed(() => {
  const itemSet = new Set()
  frequentPairs.value.forEach((pair) => pair.items.forEach((item) => itemSet.add(item)))
  return Array.from(itemSet).sort((left, right) => left.localeCompare(right, 'zh-CN'))
})

function buildHeatmap() {
  // 构造 ECharts heatmap 数据：[xIndex, yIndex, confidence, pair, rule]。
  // pair/rule 作为附加数据放进去，tooltip 中可以直接读取支持度、提升度等信息。
  const items = heatmapItems.value
  const indexMap = Object.fromEntries(items.map((item, index) => [item, index]))
  const itemCounts = new Map()

  // 先统计每个单项出现的事务数，后面用 pair.count / leftCount 计算方向置信度。
  transactions.value.forEach((itemsInTransaction) => {
    Array.from(new Set(itemsInTransaction)).forEach((item) => {
      itemCounts.set(item, (itemCounts.get(item) || 0) + 1)
    })
  })

  // 后端返回的二项集没有方向，这里把 A-B 和 B-A 都放入 Map 方便查找。
  const pairMap = new Map()
  frequentPairs.value.forEach((pair) => {
    const [left, right] = pair.items
    pairMap.set(`${left}\u0000${right}`, pair)
    pairMap.set(`${right}\u0000${left}`, pair)
  })

  // 生成完整矩阵。没有出现过的组合也补 0，避免热力图坐标缺格。
  const data = []
  items.forEach((left) => {
    items.forEach((right) => {
      const pair = left === right ? null : pairMap.get(`${left}\u0000${right}`)
      const rule = twoItemRules.value.find((item) => item.left[0] === left && item.right[0] === right)
      const value = pair ? pair.count / Math.max(1, itemCounts.get(left) || 0) : 0
      data.push([indexMap[right], indexMap[left], Number(value.toFixed(3)), pair, rule])
    })
  })

  return { items, data }
}

function renderChart() {
  if (!chartRef.value) return
  // ECharts 实例只初始化一次；之后只 clear + setOption 更新配置。
  if (!chart) chart = echarts.init(chartRef.value)

  const { items, data } = buildHeatmap()
  chart.clear()
  chart.setOption({
    backgroundColor: '#ffffff',
    tooltip: {
      position: 'top',
      formatter(params) {
        const pair = params.data[3]
        const rule = params.data[4]
        const left = items[params.data[1]]
        const right = items[params.data[0]]
        if (!pair || params.data[2] <= 0) {
          return `${left} -> ${right}<br/>置信度：0.000`
        }
        return [
          `${left} -> ${right}`,
          `支持度：${pair.support}`,
          `置信度：${params.data[2]}`,
          `提升度：${rule ? rule.lift : '-'}`,
          `规则状态：${rule ? '达到阈值' : '未达到阈值'}`,
        ].join('<br/>')
      },
    },
    grid: { left: 92, right: 32, top: 32, bottom: 92, containLabel: true },
    xAxis: {
      type: 'category',
      data: items,
      splitArea: { show: true },
      axisLabel: { rotate: 28, color: '#6b7280' },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'category',
      data: items,
      inverse: true,
      splitArea: { show: true },
      axisLabel: { color: '#6b7280' },
      axisTick: { show: false },
    },
    visualMap: {
      min: 0,
      max: 1,
      dimension: 2,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: 14,
      inRange: { color: ['#fee8e6', '#fcbba1', '#fb6a4a', '#cb181d'] },
    },
    series: [{
      name: '2 阶关联规则置信度热力图',
      type: 'heatmap',
      data,
      label: {
        show: true,
        color: '#374151',
        formatter(params) {
          return Number(params.data[2]).toFixed(3)
        },
      },
      itemStyle: {
        borderColor: '#ffffff',
        borderWidth: 2,
      },
      emphasis: {
        itemStyle: {
          borderColor: '#111827',
          borderWidth: 1,
        },
      },
    }],
  })
  chart.resize()
}

function clearAnalysis() {
  // 换数据后清空旧规则和旧图表，避免用户误读上一轮分析结果。
  rules.value = []
  frequentPairs.value = []
  if (chart) chart.clear()
}

async function loadData() {
  // 从后端读取默认事务数据。
  const response = await fetchTransactions()
  transactions.value = response.data.transactions
  clearAnalysis()
  ElMessage.success('默认数据已载入')
}

async function handleUpload(file) {
  // 交给自定义上传逻辑处理，并返回 false 阻止 Element Plus 自动上传。
  const response = await uploadTransactions(file)
  transactions.value = response.data.transactions
  clearAnalysis()
  ElMessage.success('CSV 已上传')
  return false
}

async function analyze() {
  if (!transactions.value.length) {
    ElMessage.warning('请先载入数据')
    return
  }
  // 提交阈值参数后，后端根据当前事务数据重新计算关联规则。
  const response = await runAssociation(minSupport.value, minConfidence.value)
  transactions.value = response.data.transactions
  rules.value = response.data.rules
  frequentPairs.value = response.data.frequent_pairs || []
  await nextTick()
  renderChart()
  ElMessage.success('关联规则分析完成')
}
</script>

<style scoped>
.page {
  display: grid;
  gap: 16px;
}

.association-chart {
  height: 420px;
}
</style>
