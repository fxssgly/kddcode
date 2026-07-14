<template>
  <section class="page">
    <div class="page-title">
      <strong>聚类分析</strong>
      <span>K-Means 散点图</span>
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

    <div class="two-column">
      <DataTable :rows="rows" show-cluster />
      <el-card shadow="never" class="work-card">
        <template #header>
          <div class="card-header">
            <strong>聚类散点图</strong>
            <el-tag>花瓣长度 / 花瓣宽度</el-tag>
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
import { fetchIris, runClustering, uploadIris } from '../api/request'
import DataTable from './components/DataTable.vue'

const rows = ref([])
const k = ref(3)
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
  const groups = {}
  rows.value.forEach((item) => {
    const name = item.cluster === undefined ? item.species : `第 ${item.cluster + 1} 类`
    groups[name] = groups[name] || []
    groups[name].push([item.petal_length, item.petal_width, item.species])
  })
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { top: 4 },
    grid: { left: 48, right: 24, top: 48, bottom: 42 },
    xAxis: { type: 'value', name: 'petal_length' },
    yAxis: { type: 'value', name: 'petal_width' },
    series: Object.keys(groups).map((name) => ({
      name,
      type: 'scatter',
      symbolSize: scatterSize(rows.value.length),
      itemStyle: {
        opacity: 0.78,
        borderColor: '#ffffff',
        borderWidth: 1,
      },
      data: groups[name],
    })),
  })
}

async function loadData() {
  const response = await fetchIris()
  rows.value = response.data.rows
  await nextTick()
  renderChart()
  ElMessage.success('默认数据已载入')
}

async function handleUpload(file) {
  const response = await uploadIris(file)
  rows.value = response.data.rows
  await nextTick()
  renderChart()
  ElMessage.success('CSV 已上传')
  return false
}

async function analyze() {
  const response = await runClustering(k.value)
  rows.value = response.data.rows
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
</style>
