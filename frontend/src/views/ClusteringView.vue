<template>
  <section class="page">
    <div class="page-title">
      <strong>聚类分析</strong>
      <span>K-Means</span>
    </div>

    <el-card shadow="never" class="toolbar-card">
      <el-form inline label-width="86px">
        <!-- 数据操作和算法参数放在同一行，方便实验时反复调整 k 值。 -->
        <el-form-item label="数据操作">
          <el-button type="primary" @click="loadData">载入数据</el-button>
          <el-upload :show-file-list="false" accept=".csv" :before-upload="handleUpload">
            <el-button>上传 CSV</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="聚类个数">
          <el-input-number v-model="k" :min="1" :max="8" />
        </el-form-item>
        <el-form-item>
          <el-button type="success" @click="analyze">聚类分析</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="scatter-layout">
      <DataTable :rows="rows" show-cluster />
      <el-card shadow="never" class="work-card">
        <template #header>
          <div class="card-header">
            <strong>聚类散点图</strong>
            <el-tag>标准化后 PCA1 / PCA2</el-tag>
          </div>
        </template>
        <div ref="chartRef" class="chart scatter-chart"></div>
      </el-card>
    </div>
  </section>
</template>

<script setup>
import { nextTick, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { fetchIris, runClustering, uploadIris } from '../api/request'
import DataTable from './components/DataTable.vue'

// rows 保存表格和散点图共同使用的数据。
const rows = ref([])
// centers 保存后端返回的聚类中心，用菱形标记叠加到散点图上。
const centers = ref([])
const k = ref(3)
const chartRef = ref(null)
let chart = null

// Iris 的 4 个数值特征，用于前端 PCA 降维展示。
const featureNames = ['sepal_length', 'sepal_width', 'petal_length', 'petal_width']

function scatterSize(count) {
  // 数据越多点越小，减少图表重叠。
  if (count > 300) return 5
  if (count > 120) return 6
  return 8
}

function valueAxis(name) {
  // 聚类图和回归图都使用类似的数值坐标轴风格。
  return {
    type: 'value',
    name,
    nameLocation: 'end',
    nameGap: 18,
    scale: true,
    axisLine: {
      show: true,
      onZero: true,
      lineStyle: { color: '#9aa4b2', width: 1.4 },
    },
    axisTick: { show: true, lineStyle: { color: '#c6cdd8' } },
    axisLabel: { color: '#8b95a3' },
    splitLine: { show: true, lineStyle: { color: '#edf1f7', width: 1 } },
  }
}

function toNumber(value) {
  // 上传 CSV 时数值可能是字符串，这里统一转成可计算数字。
  const number = Number(value)
  return Number.isFinite(number) ? number : 0
}

function standardizeMatrix(matrix) {
  if (!matrix.length) return []
  const count = matrix.length
  const columns = matrix[0].length
  // PCA 前先标准化，避免不同量纲的特征影响主成分方向。
  const means = Array.from({ length: columns }, (_, columnIndex) => (
    matrix.reduce((total, row) => total + row[columnIndex], 0) / count
  ))
  const stds = Array.from({ length: columns }, (_, columnIndex) => {
    const variance = matrix.reduce((total, row) => total + (row[columnIndex] - means[columnIndex]) ** 2, 0) / count
    return Math.sqrt(variance) || 1
  })
  return matrix.map((row) => row.map((value, columnIndex) => (value - means[columnIndex]) / stds[columnIndex]))
}

function matVec(matrix, vector) {
  // 矩阵乘向量，是幂迭代求特征向量的基础操作。
  return matrix.map((row) => row.reduce((total, value, index) => total + value * vector[index], 0))
}

function vectorNorm(vector) {
  // 向量长度为 0 时返回 1，避免后续除以 0。
  return Math.sqrt(vector.reduce((total, value) => total + value * value, 0)) || 1
}

function powerIteration(matrix, initial = null) {
  // 幂迭代用于估计协方差矩阵最大的特征值和特征向量。
  let vector = initial ? [...initial] : Array.from({ length: matrix.length }, () => 1)
  vector = vector.map((value) => value / vectorNorm(vector))
  for (let index = 0; index < 80; index++) {
    const nextVector = matVec(matrix, vector)
    const length = vectorNorm(nextVector)
    if (length < 1e-12) break
    vector = nextVector.map((value) => value / length)
  }
  const transformed = matVec(matrix, vector)
  const eigenvalue = vector.reduce((total, value, index) => total + value * transformed[index], 0)
  return { eigenvalue, vector }
}

function covarianceModel(matrix) {
  if (!matrix.length) return null
  const count = matrix.length
  const columns = matrix[0].length
  // 中心化后的矩阵用于计算协方差矩阵。
  const means = Array.from({ length: columns }, (_, columnIndex) => (
    matrix.reduce((total, row) => total + row[columnIndex], 0) / count
  ))
  const centered = matrix.map((row) => row.map((value, columnIndex) => value - means[columnIndex]))
  const denominator = Math.max(count - 1, 1)
  const covariance = Array.from({ length: columns }, (_, rowIndex) => (
    Array.from({ length: columns }, (_, columnIndex) => (
      centered.reduce((total, row) => total + row[rowIndex] * row[columnIndex], 0) / denominator
    ))
  ))
  const first = powerIteration(covariance)
  // 从协方差矩阵中减去第一主成分的贡献，再求第二主成分。
  const deflated = covariance.map((row, rowIndex) => (
    row.map((value, columnIndex) => value - first.eigenvalue * first.vector[rowIndex] * first.vector[columnIndex])
  ))
  const second = powerIteration(deflated, [0.5, -1, 0.75, -0.25])
  return { centered, vectors: [first.vector, second.vector] }
}

function rowsWithPca(sourceRows) {
  // 在不修改原始行对象的前提下，为每行附加 pca1/pca2，供二维散点图展示。
  const nextRows = sourceRows.map((row) => ({ ...row }))
  const matrix = standardizeMatrix(nextRows.map((row) => featureNames.map((name) => toNumber(row[name]))))
  const model = covarianceModel(matrix)
  if (!model) return nextRows
  return nextRows.map((row, rowIndex) => {
    const centeredValues = model.centered[rowIndex]
    return {
      ...row,
      pca1: Number(model.vectors[0].reduce((total, value, index) => total + centeredValues[index] * value, 0).toFixed(4)),
      pca2: Number(model.vectors[1].reduce((total, value, index) => total + centeredValues[index] * value, 0).toFixed(4)),
    }
  })
}

function renderChart() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  chart.clear()
  // 如果已经有 cluster 字段，则按聚类结果分组；否则展示为未聚类数据。
  const hasClusterResult = rows.value.some((item) => item.cluster !== undefined)
  const groups = {}
  rows.value.forEach((item) => {
    const name = hasClusterResult ? `第 ${item.cluster + 1} 类` : '未聚类数据'
    groups[name] = groups[name] || []
    const x = item.pca1 ?? item.petal_length
    const y = item.pca2 ?? item.petal_width
    groups[name].push([x, y, item.species])
  })
  // 每个聚类类别生成一个 scatter series，图例会自动展示类别名称。
  const clusterSeries = Object.keys(groups).map((name) => ({
    name,
    type: 'scatter',
    symbolSize: scatterSize(rows.value.length),
    itemStyle: {
      opacity: 0.9,
    },
    emphasis: {
      scale: 1.35,
      itemStyle: {
        opacity: 1,
      },
    },
    data: groups[name],
  }))
  chart.setOption({
    backgroundColor: '#ffffff',
    color: hasClusterResult
      ? ['#2563eb', '#f97316', '#16a34a', '#9333ea', '#dc2626', '#0891b2', '#ca8a04', '#db2777']
      : ['#2563eb'],
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: '#dbe3ef',
      borderWidth: 1,
      textStyle: { color: '#374151' },
    },
    legend: { top: 4, textStyle: { color: '#6b7280' }, itemWidth: 10, itemHeight: 10 },
    grid: { left: 72, right: 56, top: 62, bottom: 60 },
    xAxis: valueAxis('PCA1'),
    yAxis: valueAxis('PCA2'),
    dataZoom: [
      { type: 'inside', xAxisIndex: 0, filterMode: 'none' },
      { type: 'inside', yAxisIndex: 0, filterMode: 'none' },
    ],
    series: [
      ...clusterSeries,
      {
        name: '聚类中心',
        type: 'scatter',
        // 聚类中心使用菱形，和普通样本点区分。
        symbol: 'diamond',
        symbolSize: 18,
        itemStyle: {
          color: '#111827',
          opacity: 0.95,
        },
        label: {
          show: true,
          formatter(params) {
            return `C${params.data[2] + 1}`
          },
          position: 'right',
          color: '#111827',
          fontWeight: 600,
        },
        tooltip: {
          formatter(params) {
            return `聚类中心 C${params.data[2] + 1}<br/>PCA1：${params.data[0]}<br/>PCA2：${params.data[1]}`
          },
        },
        data: centers.value.map((item) => [item.pca1, item.pca2, item.cluster]),
      },
    ],
  })
  chart.resize()
}

async function loadData() {
  // 载入默认 Iris 数据后，先在前端计算 PCA 并立即绘制未聚类散点图。
  const response = await fetchIris('clustering')
  rows.value = rowsWithPca(response.data.rows)
  centers.value = []
  await nextTick()
  renderChart()
  ElMessage.success('默认数据已载入')
}

async function handleUpload(file) {
  // 上传 CSV 后同样计算 PCA；返回 false 由我们接管上传流程。
  const response = await uploadIris(file)
  rows.value = rowsWithPca(response.data.rows)
  centers.value = []
  await nextTick()
  renderChart()
  ElMessage.success('CSV 已上传')
  return false
}

async function analyze() {
  if (!rows.value.length) {
    ElMessage.warning('请先载入数据')
    return
  }
  // 聚类本身由后端完成；前端负责接收聚类标签和中心点并重绘图表。
  const response = await runClustering(k.value)
  rows.value = response.data.rows
  centers.value = response.data.centers || []
  await nextTick()
  renderChart()
  ElMessage.success('聚类分析完成')
}
</script>

<style scoped>
.page {
  display: grid;
  gap: 16px;
}

.scatter-layout {
  display: grid;
  gap: 16px;
}

.scatter-chart {
  width: min(880px, 100%);
  height: 520px;
  margin: 0 auto;
}
</style>
