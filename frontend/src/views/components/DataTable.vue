<template>
  <el-card shadow="never" class="work-card">
    <template #header>
      <div class="card-header">
        <strong>数据表格</strong>
        <el-tag type="info">{{ rows.length }} 条</el-tag>
      </div>
    </template>

    <el-table :data="rows" height="360" border size="small">
      <!-- 普通数据列由 columns 动态生成，这样上传 CSV 字段变化时表格也能适配。 -->
      <el-table-column
        v-for="column in columns"
        :key="column.prop"
        :prop="column.prop"
        :label="column.label"
        :width="column.width"
        :min-width="column.minWidth"
        show-overflow-tooltip
      />
      <!-- 聚类结果单独展示为标签，避免和普通字段重复出现。 -->
      <el-table-column v-if="showCluster" label="聚类" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.cluster !== undefined" type="success">第 {{ row.cluster + 1 }} 类</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <!-- 分类预测结果单独作为最后一列展示。 -->
      <el-table-column v-if="showPredicted" prop="predicted" label="预测类别" min-width="120" show-overflow-tooltip />
    </el-table>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'

// rows 是要展示的数据；showCluster/showPredicted 决定是否附加算法结果列。
const props = defineProps({
  rows: { type: Array, default: () => [] },
  showCluster: { type: Boolean, default: false },
  showPredicted: { type: Boolean, default: false },
})

// 后端字段名到中文表头的映射，未匹配到的字段会直接使用原字段名。
const labelMap = {
  id: 'ID',
  sepal_length: '萼片长度',
  sepal_width: '萼片宽度',
  petal_length: '花瓣长度',
  petal_width: '花瓣宽度',
  species: '实际类别',
  predicted: '预测类别',
  cluster: '聚类',
}

// 常见 Iris 字段希望按实验报告习惯排序，其它字段保持首次出现顺序。
const preferredOrder = [
  'id',
  'sepal_length',
  'sepal_width',
  'petal_length',
  'petal_width',
  'species',
]

const hiddenResultFields = computed(() => {
  // 聚类/预测字段已经有专门的结果列，动态普通列中需要隐藏它们，防止重复展示。
  const fields = new Set()
  if (props.showCluster) fields.add('cluster')
  if (props.showPredicted) fields.add('predicted')
  return fields
})

const columns = computed(() => {
  // 收集 rows 中实际出现过的字段；上传文件字段不固定，所以不能写死列。
  const keys = []
  props.rows.forEach((row) => {
    Object.keys(row || {}).forEach((key) => {
      if (!hiddenResultFields.value.has(key) && !keys.includes(key)) {
        keys.push(key)
      }
    })
  })

  // 已知字段按 preferredOrder 排在前面，其它字段跟在后面。
  keys.sort((left, right) => {
    const leftIndex = preferredOrder.indexOf(left)
    const rightIndex = preferredOrder.indexOf(right)
    if (leftIndex !== -1 || rightIndex !== -1) {
      return (leftIndex === -1 ? Number.MAX_SAFE_INTEGER : leftIndex)
        - (rightIndex === -1 ? Number.MAX_SAFE_INTEGER : rightIndex)
    }
    return 0
  })

  // 转成 Element Plus 表格列需要的配置。
  return keys.map((key) => ({
    prop: key,
    label: labelMap[key] || key,
    width: key === 'id' ? 70 : undefined,
    minWidth: key === 'id' ? undefined : 110,
  }))
})
</script>
