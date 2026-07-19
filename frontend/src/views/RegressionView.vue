<!--
  文件作用：回归分析实验页面，展示固定数据集、模型指标和三种回归曲线。
  项目位置：前端 views 层，对应后端 RegressionController 和静态 CSV 数据。

  逐词注释：
  rawRows 是 CSV 原始行；points 是带预测值的展示行；models 是模型指标。
  linear/polynomial/ransac 分别表示线性、多项式和稳健回归；predictionField 表示预测值字段名。
  scatter 是散点；line 是拟合线；requestAnimationFrame 表示下一帧再执行 resize。
-->
<template>
  <section class="page regression-page">
    <div class="page-title">
      <strong>回归分析</strong>
      <span>固定数据集</span>
    </div>

    <el-card shadow="never" class="toolbar-card">
      <el-form inline label-width="92px">
        <!-- 回归页使用固定 CSV 数据集，因此工具栏只提供载入和开始回归。 -->
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
          <el-tag>regression_data</el-tag>
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
        <!-- 每种模型各占一个 ECharts 容器，ref 由 chartConfigs 中的 setRef 收集。 -->
        <div :ref="chart.setRef" class="chart regression-chart"></div>
      </el-card>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { fetchRegressionRows, runRegression } from '../api/request'

// rawRows 保存刚从 CSV 载入的原始数据，用于后续提交给后端训练。
const rawRows = ref([]) // CSV 载入后的原始样本，不含模型预测结果。
// points 保存当前表格和图表展示的数据；回归后会包含预测值和训练/测试划分。
const points = ref([]) // 页面表格和图表实际展示的点，回归后会带 predicted 字段。
// models 保存各回归模型的公式和指标。
const models = ref([]) // 模型指标列表，包括公式、R2 和 MSE。
const trainSize = ref(0) // 训练集样本数。
const testSize = ref(0) // 测试集样本数。
const loading = ref(false) // 载入或分析中的统一加载状态。

// 每张图单独持有 DOM 引用，便于独立初始化和 resize。
const chartRefs = {
  linear: ref(null), // 线性回归图表 DOM。
  polynomial: ref(null), // 二次多项式回归图表 DOM。
  ransac: ref(null), // RANSAC 回归图表 DOM。
}

// ECharts 实例缓存，避免重复创建实例。
const chartInstances = {}
// resizeFrames 用来合并同一帧内的 resize 请求，减少图表抖动。
const resizeFrames = {}

function resizeCharts() {
  Object.values(chartInstances).forEach((chart) => chart.resize())
}

// 三张图的差异集中放在配置里，渲染函数可以复用。
const chartConfigs = [
  {
    key: 'linear', // key 对应 chartRefs、chartInstances 和模型指标里的模型标识。
    title: '一元线性回归拟合图', // 卡片标题和图例名称来源。
    predictionField: 'linear_predicted', // 每个点上的线性预测字段。
    color: '#2563eb', // 拟合线颜色。
    setRef: (el) => { chartRefs.linear.value = el }, // Vue 会把 DOM 节点传进 el。
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

// 噪声点数量由当前 points 实时计算，数据变更后页面自动刷新。
const outlierCount = computed(() => points.value.filter((item) => item.type === '噪声点').length)

function renderEmptyChart(config) {
  const element = chartRefs[config.key].value
  if (!element) return
  if (!chartInstances[config.key]) chartInstances[config.key] = echarts.init(element)
  chartInstances[config.key].clear()
  chartInstances[config.key].setOption({
    backgroundColor: '#ffffff',
    title: {
      text: '点击开始回归后显示拟合图',
      left: 'center',
      top: 'middle',
      textStyle: {
        color: '#94a3b8',
        fontSize: 14,
        fontWeight: 400,
      },
    },
    xAxis: { show: false },
    yAxis: { show: false },
    series: [],
  })
  scheduleChartResize(config.key)
}

function renderEmptyCharts() {
  chartConfigs.forEach(renderEmptyChart)
}

onMounted(async () => {
  await nextTick()
  renderEmptyCharts()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  Object.entries(resizeFrames).forEach(([key, frame]) => {
    if (frame) cancelAnimationFrame(frame)
    resizeFrames[key] = null
  })
  Object.entries(chartInstances).forEach(([key, chart]) => {
    chart.dispose()
    delete chartInstances[key]
  })
})

function sortedPoints() {
  // 拟合线需要按 x 从小到大连接，否则折线会来回跳。
  return [...points.value].sort((a, b) => a.x - b.x)
}

function modelByKey(key) {
  return models.value.find((item) => item.key === key)
}

function predictionAt(config, xValue) {
  const model = modelByKey(config.key)
  if (!model) return null
  if (config.key === 'polynomial') {
    return model.a * xValue * xValue + model.b * xValue + model.c
  }
  return model.slope * xValue + model.intercept
}

function regressionLineData(config, sorted) {
  const fallback = sorted
    .map((item) => [item.x, item[config.predictionField]])
    .filter((item) => Number.isFinite(item[0]) && Number.isFinite(item[1]))
  if (!fallback.length) return []

  const minX = fallback[0][0]
  const maxX = fallback[fallback.length - 1][0]
  const steps = config.key === 'polynomial' ? 120 : 2
  const line = []
  for (let index = 0; index < steps; index++) {
    const ratio = steps === 1 ? 0 : index / (steps - 1)
    const xValue = minX + (maxX - minX) * ratio
    const yValue = predictionAt(config, xValue)
    if (!Number.isFinite(yValue)) return fallback
    line.push([Number(xValue.toFixed(4)), Number(yValue.toFixed(4))])
  }
  return line
}

function scatterSize(count) {
  // 根据样本量调整散点大小。
  if (count > 300) return 5
  if (count > 120) return 6
  return 8
}

function valueAxis(name) {
  // 统一回归图的坐标轴视觉样式。
  return {
    show: true,
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
  // 卡片标题右侧显示当前模型指标；未分析时显示等待状态。
  const model = modelByKey(key)
  if (!model) return '等待分析'
  return `R²=${Number(model.r2).toFixed(4)}，MSE=${Number(model.mse).toFixed(2)}`
}

function renderChart(config) {
  const element = chartRefs[config.key].value
  if (!element) return
  // 第一次渲染时初始化图表，后续只更新配置。
  if (!chartInstances[config.key]) chartInstances[config.key] = echarts.init(element)
  const sorted = sortedPoints()
  const lineData = regressionLineData(config, sorted)
  chartInstances[config.key].clear()
  chartInstances[config.key].setOption({
    // 三个 series 分别画训练点、测试点和模型拟合线。
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
        // 训练集和测试集分开画，便于观察模型泛化效果。
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
        // 多项式回归使用 smooth 曲线，其它模型使用普通折线。
        smooth: !!config.smooth,
        showSymbol: false,
        lineStyle: { width: 3 },
        data: lineData,
      },
    ],
  })
  scheduleChartResize(config.key)
}

function renderCharts() {
  // 一次性刷新所有模型图表。
  chartConfigs.forEach(renderChart)
}

function scheduleChartResize(key) {
  // setOption 后延后一帧 resize，让浏览器先完成布局计算。
  if (resizeFrames[key]) cancelAnimationFrame(resizeFrames[key])
  resizeFrames[key] = requestAnimationFrame(() => {
    // resize 让 ECharts 按最新容器尺寸重新计算画布。
    chartInstances[key]?.resize()
    resizeFrames[key] = null
  })
}

function clearCharts() {
  // 清理未执行的 resize，并清空已有图表内容。
  Object.entries(resizeFrames).forEach(([key, frame]) => {
    if (frame) cancelAnimationFrame(frame)
    resizeFrames[key] = null
  })
  renderEmptyCharts()
}

async function applyResult(response, message) {
  // 后端分析结果统一在这里落到页面状态，避免三个模型分别处理。
  points.value = response.data.points || []
  // models 与 points 同时更新，保证指标表和三张图来自同一次后端结果。
  models.value = response.data.models || []
  trainSize.value = response.data.train_size || 0
  testSize.value = response.data.test_size || 0
  await nextTick()
  renderCharts()
  ElMessage.success(message)
}

async function applyLoadedRows(rows, message) {
  // 只载入数据时不显示模型指标，也不保留旧图表。
  rawRows.value = rows
  points.value = rows
  models.value = []
  trainSize.value = 0
  testSize.value = 0
  clearCharts()
  ElMessage.success(message)
}

async function loadData() {
  // 加载状态同时绑定两个按钮，防止用户连续点击导致请求重叠。
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

async function analyzeRows(message) {
  // 回归实验使用后端固定数据集，前端只负责触发分析并展示结果。
  loading.value = true
  try {
    const response = await runRegression()
    await applyResult(response, message)
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '回归分析失败，请检查后端服务')
  } finally {
    loading.value = false
  }
}

async function startRegression() {
  if (!rawRows.value.length && !points.value.length) {
    ElMessage.warning('请先载入数据')
    return
  }
  await analyzeRows('回归分析完成')
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
