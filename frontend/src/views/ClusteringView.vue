<template>
  <section class="page">
    <div class="page-title">
      <strong>聚类分析</strong>
      <span>{{ currentMethodLabel }}</span>
    </div>

    <el-card shadow="never" class="toolbar-card">
      <el-form inline label-width="86px">
        <el-form-item label="数据操作">
          <el-button type="primary" :icon="Refresh" @click="loadData">载入数据</el-button>
          <el-upload :show-file-list="false" accept=".csv" :before-upload="handleUpload">
            <el-button>上传 CSV</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="聚类方法">
          <el-select v-model="method" class="method-select">
            <el-option
              v-for="item in methodOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="needsClusterCount" label="聚类个数">
          <el-input-number v-model="k" :min="1" :max="8" />
        </el-form-item>
        <el-form-item v-if="method === 'agglomerative'" label="连接方式">
          <el-select v-model="linkage" class="small-select">
            <el-option label="ward" value="ward" />
            <el-option label="complete" value="complete" />
            <el-option label="average" value="average" />
            <el-option label="single" value="single" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="method === 'birch'" label="阈值">
          <el-input-number v-model="threshold" :min="0.01" :max="2" :step="0.05" />
        </el-form-item>
        <el-form-item v-if="method === 'dbscan'" label="邻域半径">
          <el-input-number v-model="eps" :min="0.01" :max="3" :step="0.05" />
        </el-form-item>
        <el-form-item v-if="method === 'dbscan'" label="最小样本">
          <el-input-number v-model="minSamples" :min="1" :max="30" />
        </el-form-item>
        <el-form-item v-if="method === 'meanshift'" label="带宽">
          <el-input-number v-model="bandwidth" :min="0" :max="5" :step="0.1" />
        </el-form-item>
        <el-form-item>
          <el-button type="success" :loading="analyzing" :disabled="analyzing" @click="analyze">聚类分析</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="scatter-layout">
      <DataTable :rows="rows" show-cluster />
      <el-card shadow="never" class="work-card">
        <template #header>
          <div class="card-header">
            <strong>聚类散点图</strong>
            <div class="chart-tags">
              <el-tag class="method-tag">{{ resultMethodLabel }} · 标准化后 PCA1 / PCA2</el-tag>
              <el-tag class="count-tag" type="info">{{ resultClusterCount !== null ? `${resultClusterCount} 个簇` : '未生成簇' }}</el-tag>
              <el-tag class="noise-tag" :type="resultNoiseCount ? 'warning' : 'info'">{{ resultNoiseCount ? `${resultNoiseCount} 个噪声点` : '无噪声点' }}</el-tag>
            </div>
          </div>
        </template>
        <div ref="chartRef" class="chart scatter-chart"></div>
      </el-card>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { fetchIris, runClustering, uploadIris } from '../api/request'
import DataTable from './components/DataTable.vue'

const rows = ref([])
const centers = ref([])
const method = ref('kmeans')
const k = ref(3)
const linkage = ref('ward')
const threshold = ref(0.2)
const eps = ref(0.5)
const minSamples = ref(5)
const bandwidth = ref(1)
const resultMethodLabel = ref('未聚类')
const resultClusterCount = ref(null)
const resultNoiseCount = ref(0)
const analyzing = ref(false)
const chartRef = ref(null)
let chart = null

const methodOptions = [
  { label: 'K-Means', value: 'kmeans' },
  { label: 'AgglomerativeClustering', value: 'agglomerative' },
  { label: 'Birch', value: 'birch' },
  { label: 'DBSCAN', value: 'dbscan' },
  { label: 'MeanShift', value: 'meanshift' },
]

const currentMethodLabel = computed(() => {
  const selected = methodOptions.find((item) => item.value === method.value)
  return selected ? selected.label : 'K-Means'
})

const needsClusterCount = computed(() => ['kmeans', 'agglomerative', 'birch'].includes(method.value))

function resizeChart() {
  chart?.resize()
}

function renderEmptyChart() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  chart.clear()
  chart.setOption({
    backgroundColor: '#ffffff',
    title: {
      text: '点击载入数据后显示散点图',
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
  chart.resize()
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

function clusterName(cluster) {
  const label = Number(cluster)
  return label < 0 ? '噪声点' : `第 ${label + 1} 类`
}

function centerName(cluster) {
  const label = Number(cluster)
  return label < 0 ? '噪声' : `C${label + 1}`
}

function clusteringParams() {
  return {
    method: method.value,
    rows: rows.value,
    k: k.value,
    linkage: linkage.value,
    threshold: threshold.value,
    eps: eps.value,
    min_samples: minSamples.value,
    bandwidth: bandwidth.value,
  }
}

function hasPcaRows(nextRows) {
  return nextRows.every((item) => item.pca1 !== undefined && item.pca2 !== undefined)
}

async function ensurePcaRows(nextRows) {
  if (hasPcaRows(nextRows)) {
    return nextRows
  }
  const response = await runClustering({
    method: method.value,
    rows: nextRows,
    k: k.value,
    linkage: linkage.value,
    threshold: threshold.value,
    eps: eps.value,
    min_samples: minSamples.value,
    bandwidth: bandwidth.value,
  })
  return (response.data.rows || []).map(({ cluster, ...row }) => row)
}

function renderChart() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  chart.clear()
  if (rows.value.some((item) => item.pca1 === undefined || item.pca2 === undefined)) {
    renderEmptyChart()
    ElMessage.error('后端没有返回 PCA 坐标，无法绘制降维散点图')
    return
  }
  const hasClusterResult = rows.value.some((item) => item.cluster !== undefined)
  const groups = {}
  rows.value.forEach((item) => {
    const name = hasClusterResult ? clusterName(item.cluster) : '未聚类数据'
    groups[name] = groups[name] || []
    groups[name].push([item.pca1, item.pca2, item.species])
  })
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
        symbol: 'diamond',
        symbolSize: 18,
        itemStyle: {
          color: '#111827',
          opacity: 0.95,
        },
        label: {
          show: true,
          formatter(params) {
            return centerName(params.data[2])
          },
          position: 'right',
          color: '#111827',
          fontWeight: 600,
        },
        tooltip: {
          formatter(params) {
            return `聚类中心 ${centerName(params.data[2])}<br/>PCA1：${params.data[0]}<br/>PCA2：${params.data[1]}`
          },
        },
        data: centers.value.map((item) => [item.pca1, item.pca2, item.cluster]),
      },
    ],
  })
  chart.resize()
}

async function loadData() {
  const response = await fetchIris('clustering')
  rows.value = await ensurePcaRows(response.data.rows || [])
  centers.value = []
  resultMethodLabel.value = '未聚类'
  resultClusterCount.value = null
  resultNoiseCount.value = 0
  await nextTick()
  renderChart()
  ElMessage.success('默认数据已载入')
}

async function handleUpload(file) {
  const response = await uploadIris(file, 'clustering')
  rows.value = await ensurePcaRows(response.data.rows || [])
  centers.value = []
  resultMethodLabel.value = '未聚类'
  resultClusterCount.value = null
  resultNoiseCount.value = 0
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
  analyzing.value = true
  try {
    const response = await runClustering(clusteringParams())
    rows.value = response.data.rows
    centers.value = response.data.centers || []
    resultMethodLabel.value = response.data.methodName || currentMethodLabel.value
    resultClusterCount.value = response.data.clusterCount ?? null
    resultNoiseCount.value = response.data.noiseCount || 0
    await nextTick()
    renderChart()
    ElMessage.success(`${resultMethodLabel.value} 聚类分析完成`)
  } catch (error) {
    const message = error.response?.data?.message || error.response?.data?.error || error.message || '聚类分析失败'
    ElMessage.error(message)
  } finally {
    analyzing.value = false
  }
}

onMounted(() => {
  renderEmptyChart()
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  chart?.dispose()
  chart = null
})
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

.method-select {
  width: 230px;
}

.small-select {
  width: 130px;
}

.chart-tags {
  display: grid;
  grid-template-columns: 230px 86px 104px;
  gap: 8px;
  align-items: center;
}

.chart-tags .el-tag {
  justify-content: center;
}
</style>
