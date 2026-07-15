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

const classPalette = {
  setosa: [252, 186, 122],
  versicolor: [119, 191, 229],
  virginica: [225, 146, 231],
  unknown: [190, 210, 205],
}

function classColor(className) {
  return classPalette[className] || classPalette.unknown
}

function blendedNodeColor(node) {
  const base = classColor(node.className || node.name)
  const values = Array.isArray(node.value) ? node.value : []
  const samples = Number(node.samples || values.reduce((sum, item) => sum + Number(item || 0), 0) || 1)
  const maxCount = values.length ? Math.max(...values.map((item) => Number(item || 0))) : samples
  const purity = samples ? maxCount / samples : 0
  const mix = 0.18 + purity * 0.5
  const rgb = base.map((channel) => Math.round(255 - (255 - channel) * mix))
  return `rgb(${rgb[0]}, ${rgb[1]}, ${rgb[2]})`
}

function formatRule(name) {
  return String(name || '').replace(' <= ', ' <= ')
}

function buildNodeLabel(node) {
  const rule = node.children && node.children.length ? formatRule(node.name) : `class = ${node.className || node.name}`
  const criterion = node.criterion || 'gini'
  const impurity = Number(node.impurity || 0).toFixed(3)
  const samples = Number(node.samples || 0)
  const value = Array.isArray(node.value) ? `[${node.value.join(', ')}]` : `[${node.value || 0}]`
  const className = node.className || node.name || 'unknown'
  return `${rule}\n${criterion}=${impurity}\nsamples=${samples}\nvalue=${value}\nclass=${className}`
}

function decorateTree(node) {
  const children = (node.children || []).map(decorateTree)
  const isLeaf = children.length === 0
  return {
    ...node,
    name: buildNodeLabel(node),
    children,
    itemStyle: {
      color: blendedNodeColor(node),
      borderColor: isLeaf ? '#8b8f98' : '#7f8794',
      borderWidth: 1.2,
    },
    label: {
      color: '#111827',
      fontSize: 9,
      fontWeight: isLeaf ? 600 : 500,
    },
  }
}

function treeStats(node, depth = 0) {
  const children = node.children || []
  if (!children.length) {
    return { depth, leaves: 1 }
  }
  return children.reduce((stats, child) => {
    const childStats = treeStats(child, depth + 1)
    return {
      depth: Math.max(stats.depth, childStats.depth),
      leaves: stats.leaves + childStats.leaves,
    }
  }, { depth, leaves: 0 })
}

function renderTree(tree) {
  if (!chartRef.value || !tree) return
  const stats = treeStats(tree)
  const chartWidth = Math.max(1180, stats.leaves * 240)
  const chartHeight = Math.max(780, (stats.depth + 1) * 180)
  chartRef.value.style.width = `${chartWidth}px`
  chartRef.value.style.height = `${chartHeight}px`

  if (!chart) chart = echarts.init(chartRef.value)
  chart.clear()
  chart.resize()
  chart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (params) => String(params.name || '').replace(/\n/g, '<br/>'),
    },
    series: [{
      type: 'tree',
      orient: 'vertical',
      data: [decorateTree(tree)],
      top: '8%',
      left: '6%',
      bottom: '10%',
      right: '6%',
      symbol: 'rect',
      symbolSize: [138, 78],
      edgeShape: 'polyline',
      edgeForkPosition: '50%',
      initialTreeDepth: -1,
      roam: true,
      scaleLimit: {
        min: 0.45,
        max: 2.2,
      },
      label: {
        position: 'inside',
        verticalAlign: 'middle',
        align: 'center',
        lineHeight: 12,
      },
      leaves: {
        label: {
          position: 'inside',
          verticalAlign: 'middle',
          align: 'center',
          lineHeight: 12,
        },
      },
      lineStyle: {
        color: '#8f96a3',
        width: 1,
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
  display: flex;
  justify-content: center;
  overflow-x: auto;
  overflow-y: auto;
  max-height: 78vh;
  padding: 0 18px 10px;
}

.tree-chart {
  flex: 0 0 auto;
  width: 1180px;
  min-width: 1180px;
  height: 780px;
}
</style>
