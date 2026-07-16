<template>
  <section class="page">
    <div class="page-title">
      <strong>聚类分析</strong>
      <span>K-Means</span>
    </div>

    <el-card shadow="never" class="toolbar-card">
      <el-form inline label-width="86px">
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
import { nextTick, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { fetchIris, runClustering, uploadIris } from '../api/request'
import DataTable from './components/DataTable.vue'

const rows = ref([])
const centers = ref([])
const k = ref(3)
const chartRef = ref(null)
let chart = null

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

function renderChart() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  chart.clear()
  const groups = {}
  rows.value.forEach((item) => {
    const name = item.cluster === undefined ? item.species : `第 ${item.cluster + 1} 类`
    groups[name] = groups[name] || []
    const x = item.pca1 ?? item.petal_length
    const y = item.pca2 ?? item.petal_width
    groups[name].push([x, y, item.species])
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
    color: ['#2563eb', '#f97316', '#16a34a', '#9333ea', '#dc2626', '#0891b2', '#ca8a04', '#db2777'],
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
  const response = await fetchIris('clustering')
  rows.value = response.data.rows
  centers.value = []
  await nextTick()
  renderChart()
  ElMessage.success('默认数据已载入')
}

async function handleUpload(file) {
  const response = await uploadIris(file)
  rows.value = response.data.rows
  centers.value = []
  await nextTick()
  renderChart()
  ElMessage.success('CSV 已上传')
  return false
}

async function analyze() {
  const response = await runClustering(k.value)
  rows.value = response.data.rows
  centers.value = response.data.centers || []
  await nextTick()
  renderChart()
  ElMessage.success('聚类分析完成')
}

onMounted(loadData)
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
