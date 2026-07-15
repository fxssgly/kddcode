<template>
  <section class="page">
    <div class="page-title">
      <strong>分类分析</strong>
      <span>CART 决策树分类</span>
    </div>

    <el-card shadow="never" class="toolbar-card">
      <el-form inline label-width="92px">
        <el-form-item label="数据操作">
          <el-button type="primary" @click="loadData">载入数据</el-button>
          <el-upload :show-file-list="false" accept=".csv" :before-upload="handleUpload">
            <el-button>上传 CSV</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="树高度">
          <el-input-number v-model="maxDepth" :min="1" :max="8" />
        </el-form-item>
        <el-form-item label="叶子样本数">
          <el-input-number v-model="minLeaf" :min="1" :max="10" />
        </el-form-item>
        <el-form-item>
          <el-button type="success" @click="analyze">CART 分类</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="classification-layout">
      <DataTable :rows="rows" show-predicted />
      <el-card shadow="never" class="work-card">
        <template #header>
          <div class="card-header">
            <strong>决策树图（CART）</strong>
            <el-tag type="success">准确率 {{ accuracyText }}</el-tag>
            <el-tag>Precision {{ precisionText }}</el-tag>
            <el-tag>Recall {{ recallText }}</el-tag>
            <el-tag>F1 {{ f1Text }}</el-tag>
          </div>
        </template>
        <div class="tree-scroll">
          <div ref="chartRef" class="chart tree-chart"></div>
        </div>
      </el-card>
    </div>
  </section>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { fetchIris, runClassification, uploadIris } from '../api/request'
import DataTable from './components/DataTable.vue'

const rows = ref([])
const maxDepth = ref(4)
const minLeaf = ref(2)
const accuracyText = ref('-')
const precisionText = ref('-')
const recallText = ref('-')
const f1Text = ref('-')
const chartRef = ref(null)
let chart = null

function formatNodeName(name) {
  return String(name || '').replace(' <= ', '\n<= ')
}

function nodeColor(name, isLeaf) {
  if (!isLeaf) return '#ffffff'
  if (name === 'setosa') return '#f59e42'
  if (name === 'versicolor') return '#38a9e6'
  if (name === 'virginica') return '#d946ef'
  return '#a7f3d0'
}

function decorateTree(node) {
  const children = (node.children || []).map(decorateTree)
  const isLeaf = children.length === 0
  return {
    ...node,
    name: formatNodeName(node.name),
    children,
    itemStyle: {
      color: nodeColor(node.name, isLeaf),
      borderColor: '#9ca3af',
      borderWidth: 1.4,
    },
    label: {
      color: isLeaf ? '#111827' : '#374151',
      fontSize: 12,
      fontWeight: isLeaf ? 600 : 500,
    },
  }
}

function renderTree(tree) {
  if (!chartRef.value || !tree) return
  if (!chart) chart = echarts.init(chartRef.value)
  chart.clear()
  chart.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'tree',
      orient: 'vertical',
      data: [decorateTree(tree)],
      top: '8%',
      left: '3%',
      bottom: '10%',
      right: '3%',
      symbol: 'rect',
      symbolSize: [136, 48],
      edgeShape: 'polyline',
      edgeForkPosition: '50%',
      initialTreeDepth: -1,
      roam: true,
      scaleLimit: {
        min: 0.7,
        max: 1.8,
      },
      label: {
        position: 'inside',
        verticalAlign: 'middle',
        align: 'center',
        lineHeight: 17,
      },
      leaves: {
        label: {
          position: 'inside',
          verticalAlign: 'middle',
          align: 'center',
          lineHeight: 17,
        },
      },
      lineStyle: {
        color: '#9ca3af',
        width: 1.2,
      },
      emphasis: {
        focus: 'descendant',
      },
      expandAndCollapse: true,
      animationDuration: 350,
    }],
  })
  chart.resize()
}

async function loadData() {
  const response = await fetchIris('classification')
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
  const response = await runClassification(maxDepth.value, minLeaf.value)
  rows.value = response.data.rows
  accuracyText.value = `${Math.round(response.data.accuracy * 100)}%`
  precisionText.value = Number(response.data.precision || 0).toFixed(3)
  recallText.value = Number(response.data.recall || 0).toFixed(3)
  f1Text.value = Number(response.data.f1 || 0).toFixed(3)
  await nextTick()
  renderTree(response.data.tree)
  ElMessage.success('分类分析完成')
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

.classification-layout {
  display: grid;
  gap: 16px;
}

.tree-scroll {
  width: 100%;
  overflow-x: auto;
  overflow-y: hidden;
  padding-bottom: 6px;
}

.tree-chart {
  width: 100%;
  min-width: 980px;
  height: 620px;
}
</style>
