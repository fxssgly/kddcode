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
          <el-table-column prop="id" label="TID" width="70" />
          <el-table-column prop="itemsText" label="商品项" min-width="240" />
        </el-table>
      </el-card>

      <el-card shadow="never" class="work-card">
        <template #header>
          <div class="card-header">
            <strong>热力图展示</strong>
            <el-segmented v-model="heatmapMode" :options="heatmapOptions" @change="renderChart" />
          </div>
        </template>
        <div ref="chartRef" class="chart"></div>
      </el-card>
    </div>

    <el-card shadow="never" class="work-card">
      <template #header>
        <div class="card-header">
          <strong>关联规则结果</strong>
          <el-tag type="success">{{ rules.length }} 条规则</el-tag>
        </div>
      </template>
      <el-table :data="rules" border size="small">
        <el-table-column label="前项" min-width="180">
          <template #default="{ row }">{{ row.left.join(', ') }}</template>
        </el-table-column>
        <el-table-column label="后项" min-width="180">
          <template #default="{ row }">{{ row.right.join(', ') }}</template>
        </el-table-column>
        <el-table-column prop="support" label="支持度" width="120" />
        <el-table-column prop="confidence" label="置信度" width="120" />
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
const minSupport = ref(0.3)
const minConfidence = ref(0.6)
const heatmapMode = ref('relation')
const chartRef = ref(null)
const heatmapOptions = [
  { label: '关联强度', value: 'relation' },
  { label: '支持度', value: 'support' },
  { label: '置信度', value: 'confidence' },
]
let chart = null

const transactionRows = computed(() => transactions.value.map((items, index) => ({
  id: index + 1,
  itemsText: items.join(', '),
})))

function collectItems() {
  const itemSet = new Set()
  transactions.value.forEach((items) => items.forEach((item) => itemSet.add(item)))
  return Array.from(itemSet).slice(0, 10)
}

function buildHeatmap(metric) {
  const items = collectItems()
  const indexMap = Object.fromEntries(items.map((item, index) => [item, index]))
  const matrix = items.map(() => items.map(() => 0))
  const itemCount = items.map(() => 0)
  const total = Math.max(1, transactions.value.length)

  transactions.value.forEach((record) => {
    const values = Array.from(new Set(record)).filter((item) => item in indexMap)
    values.forEach((left) => {
      itemCount[indexMap[left]] += 1
      values.forEach((right) => {
        matrix[indexMap[left]][indexMap[right]] += 1
      })
    })
  })

  const maxValue = Math.max(1, ...matrix.flat())
  const data = []
  matrix.forEach((row, yIndex) => {
    row.forEach((count, xIndex) => {
      let value = count / maxValue
      if (metric === 'support') value = count / total
      if (metric === 'confidence') value = count / Math.max(1, itemCount[yIndex])
      data.push([xIndex, yIndex, Number(value.toFixed(2))])
    })
  })
  return { items, data }
}

function renderChart() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  const { items, data } = buildHeatmap(heatmapMode.value)
  chart.setOption({
    tooltip: {
      position: 'top',
      formatter(params) {
        return `${items[params.data[1]]} -> ${items[params.data[0]]}<br/>指标值：${params.data[2]}`
      },
    },
    grid: { left: 110, right: 24, top: 28, bottom: 92 },
    xAxis: { type: 'category', data: items, splitArea: { show: true } },
    yAxis: { type: 'category', data: items, splitArea: { show: true } },
    visualMap: { min: 0, max: 1, calculable: true, orient: 'horizontal', left: 'center', bottom: 18 },
    series: [{
      name: '关联规则热力图',
      type: 'heatmap',
      data,
      label: { show: true },
    }],
  })
}

async function loadData() {
  const response = await fetchTransactions()
  transactions.value = response.data.transactions
  await nextTick()
  renderChart()
  ElMessage.success('默认交易数据已载入')
}

async function handleUpload(file) {
  const response = await uploadTransactions(file)
  transactions.value = response.data.transactions
  await nextTick()
  renderChart()
  ElMessage.success('CSV 已上传')
  return false
}

async function analyze() {
  const response = await runAssociation(minSupport.value, minConfidence.value)
  transactions.value = response.data.transactions
  rules.value = response.data.rules
  await nextTick()
  renderChart()
  ElMessage.success('关联规则分析完成')
}

onMounted(async () => {
  await loadData()
  await analyze()
})
</script>

<style scoped>
.page {
  display: grid;
  gap: 16px;
}
</style>
