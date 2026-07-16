<template>
  <section class="page">
    <div class="page-title">
      <strong>分类分析</strong>
      <span>CART 决策树分类</span>
    </div>

    <el-card shadow="never" class="toolbar-card">
      <el-form inline label-width="92px">
        <!-- 分类实验的主要参数：树高度和叶子节点最小样本数。 -->
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
import { nextTick, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { fetchIris, runClassification, uploadIris } from '../api/request'
import DataTable from './components/DataTable.vue'

// rows 保存表格数据，分类完成后每行会带 predicted 字段。
const rows = ref([])
const maxDepth = ref(4)
const minLeaf = ref(2)

// 这几个指标以文本形式保存，便于直接显示在卡片头部。
const accuracyText = ref('-')
const precisionText = ref('-')
const recallText = ref('-')
const f1Text = ref('-')
const chartRef = ref(null)
let chart = null

// 不同 Iris 类别对应的基础颜色，树节点会根据类别纯度做浅色混合。
const classPalette = {
  setosa: [252, 186, 122],
  versicolor: [119, 191, 229],
  virginica: [225, 146, 231],
  unknown: [190, 210, 205],
}

function classColor(className) {
  // 未知类别使用中性色，防止后端返回新类别时图表报错。
  return classPalette[className] || classPalette.unknown
}

function blendedNodeColor(node) {
  // 根据节点中占比最高的类别计算纯度，纯度越高颜色越接近类别基础色。
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
  // 规则文本预留格式化入口，后续如果需要替换字段名可在这里统一处理。
  return String(name || '').replace(' <= ', ' <= ')
}

function buildNodeLabel(node) {
  // ECharts tree 节点只显示 name，所以把 CART 节点信息拼成多行标签。
  const rule = node.children && node.children.length ? formatRule(node.name) : `class = ${node.className || node.name}`
  const criterion = node.criterion || 'gini'
  const impurity = Number(node.impurity || 0).toFixed(3)
  const samples = Number(node.samples || 0)
  const value = Array.isArray(node.value) ? `[${node.value.join(', ')}]` : `[${node.value || 0}]`
  const className = node.className || node.name || 'unknown'
  return `${rule}\n${criterion}=${impurity}\nsamples=${samples}\nvalue=${value}\nclass=${className}`
}

function decorateTree(node) {
  // 递归给每个节点补充 ECharts 样式和展示标签，保留原始 children 结构。
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
  // 统计树深度和叶子数，用来动态设置画布大小，避免节点挤在一起。
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
  // 树越宽或越深，图表容器越大；外层 tree-scroll 负责滚动查看。
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
      // tree 系列负责绘制决策树，roam=true 允许拖拽和平移缩放。
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

function clearTree() {
  // 重新载入数据后，旧树和旧指标都需要清空。
  if (chart) chart.clear()
  accuracyText.value = '-'
  precisionText.value = '-'
  recallText.value = '-'
  f1Text.value = '-'
}

async function loadData() {
  // 载入默认 Iris 分类数据。
  const response = await fetchIris('classification')
  rows.value = response.data.rows
  clearTree()
  ElMessage.success('默认数据已载入')
}

async function handleUpload(file) {
  // 上传用户提供的 Iris CSV，并阻止 Element Plus 自动上传。
  const response = await uploadIris(file)
  rows.value = response.data.rows
  clearTree()
  ElMessage.success('CSV 已上传')
  return false
}

async function analyze() {
  if (!rows.value.length) {
    ElMessage.warning('请先载入数据')
    return
  }
  // 后端返回预测结果、指标和树结构；前端负责展示和绘制。
  const response = await runClassification(maxDepth.value, minLeaf.value)
  rows.value = response.data.rows
  accuracyText.value = `${Math.round(response.data.accuracy * 100)}%`
  precisionText.value = Number(response.data.precision || 0).toFixed(3)
  recallText.value = Number(response.data.recall || 0).toFixed(3)
  f1Text.value = Number(response.data.f1 || 0).toFixed(3)
  // 等表格和图表容器完成更新后再渲染决策树。
  await nextTick()
  renderTree(response.data.tree)
  ElMessage.success('分类分析完成')
}
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
