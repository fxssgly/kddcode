<template>
  <section class="page regression-page">
    <div class="page-title">
      <strong>回归分析</strong>
      <span>固定数据集</span>
    </div>

    <el-card shadow="never" class="toolbar-card">
      <el-form inline label-width="92px">
        <el-form-item label="数据集">
          <el-button type="primary" :icon="Refresh" :loading="loading" @click="loadData">载入数据</el-button>
          <el-button type="success" :loading="loading" @click="startRegression">开始回归</el-button>
        </el-form-item>
        <el-form-item label="样本规模">
          <el-tag>{{ points.length }} 个样本</el-tag>
          <el-tag type="warning">{{ outlierCount }} 个噪声点</el-tag>
        </el-form-item>
        <el-form-item label="训练 / 测试">
          <el-tag type="success">{{ trainSize }} / {{ testSize }}</el-tag>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="work-card">
      <template #header>
        <div class="card-header">
          <strong>实验数据表格</strong>
          <el-tag>regression_experiment.csv</el-tag>
        </div>
      </template>
      <el-table :data="points" border size="small" height="360">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="x" label="X" width="110" />
        <el-table-column prop="y" label="y" width="110" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.type === '噪声点' ? 'warning' : 'success'">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="split" label="划分" width="90">
          <template #default="{ row }">
            <el-tag :type="row.split === 'test' ? 'primary' : 'info'">
              {{ row.split === 'test' ? '测试' : '训练' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="linear_predicted" label="线性预测" width="120" />
        <el-table-column prop="polynomial_predicted" label="二次预测" width="120" />
        <el-table-column prop="ransac_predicted" label="RANSAC 预测" width="130" />
      </el-table>
    </el-card>

    <el-card shadow="never" class="work-card">
      <template #header>
        <div class="card-header">
          <strong>模型指标表格</strong>
          <el-tag type="info">测试集 R² / MSE</el-tag>
        </div>
      </template>
      <el-table :data="models" border size="small">
        <el-table-column prop="name" label="算法" min-width="160" />
        <el-table-column prop="formula" label="拟合方程" min-width="260" show-overflow-tooltip />
        <el-table-column prop="r2" label="R²" width="110" />
        <el-table-column prop="mse" label="MSE" width="120" />
      </el-table>
    </el-card>

    <div class="chart-grid">
      <el-card
        v-for="chart in chartConfigs"
        :key="chart.key"
        shadow="never"
        class="work-card"
      >
        <template #header>
          <div class="card-header">
            <strong>{{ chart.title }}</strong>
            <el-tag>{{ metricLabel(chart.key) }}</el-tag>
          </div>
        </template>
        <div :ref="chart.setRef" class="chart regression-chart"></div>
      </el-card>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { fetchRegressionRows, runRegression } from '../api/request'

const rawRows = ref([])
const points = ref([])
const models = ref([])
const trainSize = ref(0)
const testSize = ref(0)
const loading = ref(false)
const chartRefs = {
  linear: ref(null),
  polynomial: ref(null),
  ransac: ref(null),
}
const chartInstances = {}
const resizeFrames = {}

const chartConfigs = [
  {
    key: 'linear',
    title: '一元线性回归拟合图',
    predictionField: 'linear_predicted',
    color: '#2563eb',
    setRef: (el) => { chartRefs.linear.value = el },
  },
  {
    key: 'polynomial',
    title: '一元二次多项式回归拟合图',
    predictionField: 'polynomial_predicted',
    color: '#2563eb',
    smooth: true,
    setRef: (el) => { chartRefs.polynomial.value = el },
  },
  {
    key: 'ransac',
    title: 'RANSAC 稳健回归拟合图',
    predictionField: 'ransac_predicted',
    color: '#2563eb',
    setRef: (el) => { chartRefs.ransac.value = el },
  },
]

const outlierCount = computed(() => points.value.filter((item) => item.type === '噪声点').length)

function sortedPoints() {
  return [...points.value].sort((a, b) => a.x - b.x)
}

function scatterSize(count) {
  if (count > 300) return 5
  if (count > 120) return 6
  return 8
}

function valueAxis(name) {
  return {
    type: 'value',
    name,
    nameLocation: 'end',
    nameGap: 18,
    scale: true,
    axisLine: { show: true, lineStyle: { color: '#9aa4b2' } },
    axisTick: { show: true, lineStyle: { color: '#c6cdd8' } },
    axisLabel: { color: '#8b95a3' },
    splitLine: { show: true, lineStyle: { color: '#edf1f7' } },
  }
}

function metricLabel(key) {
  const model = models.value.find((item) => item.key === key)
  if (!model) return '等待分析'
  return `R²=${Number(model.r2).toFixed(4)}，MSE=${Number(model.mse).toFixed(2)}`
}

function renderChart(config) {
  const element = chartRefs[config.key].value
  if (!element) return
  if (!chartInstances[config.key]) chartInstances[config.key] = echarts.init(element)
  const sorted = sortedPoints()
  chartInstances[config.key].setOption({
    backgroundColor: '#ffffff',
    color: ['#facc15', '#22c55e', config.color],
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: '#dbe3ef',
      borderWidth: 1,
      textStyle: { color: '#374151' },
    },
    legend: { top: 4, textStyle: { color: '#6b7280' }, itemWidth: 10, itemHeight: 10 },
    grid: { left: 68, right: 42, top: 58, bottom: 58 },
    xAxis: valueAxis('X'),
    yAxis: valueAxis('y'),
    dataZoom: [
      { type: 'inside', xAxisIndex: 0, filterMode: 'none' },
      { type: 'inside', yAxisIndex: 0, filterMode: 'none' },
    ],
    series: [
      {
        name: '训练样本',
        type: 'scatter',
        symbolSize: scatterSize(sorted.length),
        data: sorted.filter((item) => item.split === 'train').map((item) => [item.x, item.y, item.type]),
        itemStyle: { opacity: 0.62 },
      },
      {
        name: '测试样本',
        type: 'scatter',
        symbolSize: scatterSize(sorted.length) + 2,
        data: sorted.filter((item) => item.split === 'test').map((item) => [item.x, item.y, item.type]),
        itemStyle: { opacity: 0.9, borderColor: '#ffffff', borderWidth: 1 },
      },
      {
        name: config.title.replace('拟合图', ''),
        type: 'line',
        smooth: !!config.smooth,
        showSymbol: false,
        lineStyle: { width: 3 },
        data: sorted.map((item) => [item.x, item[config.predictionField]]),
      },
    ],
  })
  scheduleChartResize(config.key)
}

function renderCharts() {
  chartConfigs.forEach(renderChart)
}

function scheduleChartResize(key) {
  if (resizeFrames[key]) cancelAnimationFrame(resizeFrames[key])
  resizeFrames[key] = requestAnimationFrame(() => {
    chartInstances[key]?.resize()
    resizeFrames[key] = null
  })
}

function clearCharts() {
  Object.entries(resizeFrames).forEach(([key, frame]) => {
    if (frame) cancelAnimationFrame(frame)
    resizeFrames[key] = null
  })
  Object.values(chartInstances).forEach((chart) => chart.clear())
}

async function applyResult(response, message) {
  points.value = response.data.points || []
  models.value = response.data.models || []
  trainSize.value = response.data.train_size || 0
  testSize.value = response.data.test_size || 0
  await nextTick()
  renderCharts()
  ElMessage.success(message)
}

async function applyLoadedRows(rows, message) {
  rawRows.value = rows
  points.value = rows
  models.value = []
  trainSize.value = 0
  testSize.value = 0
  clearCharts()
  ElMessage.success(message)
}

async function loadData() {
  loading.value = true
  try {
    const response = await fetchRegressionRows()
    await applyLoadedRows(response.data.rows || [], '数据已载入')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '数据载入失败，请检查后端服务')
  } finally {
    loading.value = false
  }
}

async function analyzeRows(rows, message) {
  loading.value = true
  try {
    const response = await runRegression('x', 'y', rows)
    await applyResult(response, message)
  } catch (error) {
    ElMessage.error(error.response?.data?.message || 'CSV 分析失败，请检查字段是否包含 x 和 y')
  } finally {
    loading.value = false
  }
}

async function startRegression() {
  const rows = rawRows.value.length ? rawRows.value : points.value
  if (!rows.length) {
    ElMessage.warning('请先载入数据')
    return
  }
  await analyzeRows(rows, '回归分析完成')
}
</script>

<style scoped>
.regression-page {
  display: grid;
  gap: 16px;
}

.toolbar-card .el-form-item {
  margin-bottom: 0;
}

.toolbar-card .el-tag + .el-tag {
  margin-left: 8px;
}

.toolbar-card .el-upload {
  margin-left: 8px;
}

.chart-grid {
  display: grid;
  gap: 16px;
}

.regression-chart {
  height: 620px;
}
</style>
