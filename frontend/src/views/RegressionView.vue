<template>
  <section class="page">
    <div class="page-title">
      <strong>回归分析</strong>
      <span>线性回归散点图</span>
    </div>

    <el-card shadow="never" class="toolbar-card">
      <el-form inline label-width="82px">
        <el-form-item label="数据操作">
          <el-button type="primary" @click="loadData">载入数据</el-button>
          <el-upload :show-file-list="false" accept=".csv" :before-upload="handleUpload">
            <el-button>上传 CSV</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="X 字段">
          <el-select v-model="xField" style="width: 150px">
            <el-option v-for="field in fields" :key="field" :label="field" :value="field" />
          </el-select>
        </el-form-item>
        <el-form-item label="Y 字段">
          <el-select v-model="yField" style="width: 150px">
            <el-option v-for="field in fields" :key="field" :label="field" :value="field" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="success" @click="analyze">回归分析</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="two-column">
      <DataTable :rows="rows" />
      <el-card shadow="never" class="work-card">
        <template #header>
          <div class="card-header">
            <strong>回归散点图</strong>
            <el-tag>y = {{ slope }}x + {{ intercept }}，R²={{ r2 }}</el-tag>
          </div>
        </template>
        <div ref="chartRef" class="chart"></div>
      </el-card>
    </div>
  </section>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { fetchIris, runRegression, uploadIris } from '../api/request'
import DataTable from './components/DataTable.vue'

const fields = ['sepal_length', 'sepal_width', 'petal_length', 'petal_width']
const rows = ref([])
const points = ref([])
const xField = ref('petal_length')
const yField = ref('petal_width')
const slope = ref('-')
const intercept = ref('-')
const r2 = ref('-')
const chartRef = ref(null)
let chart = null

function scatterSize(count) {
  if (count > 300) return 5
  if (count > 120) return 6
  return 7
}

function renderChart() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  const sorted = [...points.value].sort((a, b) => a.x - b.x)
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { top: 4 },
    grid: { left: 48, right: 24, top: 48, bottom: 42 },
    xAxis: { type: 'value', name: xField.value },
    yAxis: { type: 'value', name: yField.value },
    series: [
      {
        name: '原始数据',
        type: 'scatter',
        symbolSize: scatterSize(sorted.length),
        itemStyle: {
          opacity: 0.78,
          borderColor: '#ffffff',
          borderWidth: 1,
        },
        data: sorted.map((item) => [item.x, item.y, item.species]),
      },
      { name: '回归线', type: 'line', smooth: true, showSymbol: false, data: sorted.map((item) => [item.x, item.predicted]) },
    ],
  })
}

async function loadData() {
  const response = await fetchIris()
  rows.value = response.data.rows
  ElMessage.success('默认数据已载入')
}

async function handleUpload(file) {
  const response = await uploadIris(file)
  rows.value = response.data.rows
  ElMessage.success('CSV 已上传')
  return false
}

async function analyze() {
  const response = await runRegression(xField.value, yField.value)
  points.value = response.data.points
  slope.value = response.data.slope
  intercept.value = response.data.intercept
  r2.value = response.data.r2
  await nextTick()
  renderChart()
  ElMessage.success('回归分析完成')
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
