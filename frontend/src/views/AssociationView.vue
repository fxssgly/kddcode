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
            <el-tag type="info">minSupport={{ minSupport }}，minConfidence={{ minConfidence }}</el-tag>
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
import { computed, nextTick, onMounted, ref } from 'vue'
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

const transactionRows = computed(() => transactions.value.map((items, index) => ({
  id: 101 + index,
  itemsText: items.join('，'),
})))

const twoItemRules = computed(() => rules.value.filter((rule) => rule.left.length === 1 && rule.right.length === 1))

const heatmapItems = computed(() => {
  const itemSet = new Set()
  frequentPairs.value.forEach((pair) => pair.items.forEach((item) => itemSet.add(item)))
  return Array.from(itemSet).sort((left, right) => left.localeCompare(right, 'zh-CN'))
})

function buildHeatmap() {
  const items = heatmapItems.value
  const indexMap = Object.fromEntries(items.map((item, index) => [item, index]))
  const itemCounts = new Map()
  transactions.value.forEach((items) => {
    Array.from(new Set(items)).forEach((item) => {
      itemCounts.set(item, (itemCounts.get(item) || 0) + 1)
    })
  })
  const pairMap = new Map()
  frequentPairs.value.forEach((pair) => {
    const [left, right] = pair.items
    pairMap.set(`${left}\u0000${right}`, pair)
    pairMap.set(`${right}\u0000${left}`, pair)
  })
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
          return `${left} → ${right}<br/>置信度：0.000`
        }
        return [
          `${left} → ${right}`,
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

function metricName() {
  return '支持度'
}

async function loadData() {
  const response = await fetchTransactions()
  transactions.value = response.data.transactions
  await analyze()
}

async function handleUpload(file) {
  const response = await uploadTransactions(file)
  transactions.value = response.data.transactions
  await analyze()
  ElMessage.success('CSV 已上传')
  return false
}

async function analyze() {
  const response = await runAssociation(minSupport.value, minConfidence.value)
  transactions.value = response.data.transactions
  rules.value = response.data.rules
  frequentPairs.value = response.data.frequent_pairs || []
  await nextTick()
  renderChart()
  ElMessage.success('关联规则分析完成')
}

onMounted(loadData)
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
