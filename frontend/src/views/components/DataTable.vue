<template>
  <el-card shadow="never" class="work-card">
    <template #header>
      <div class="card-header">
        <strong>数据表格</strong>
        <el-tag type="info">{{ rows.length }} 条</el-tag>
      </div>
    </template>

    <el-table :data="rows" height="360" border size="small">
      <el-table-column
        v-for="column in columns"
        :key="column.prop"
        :prop="column.prop"
        :label="column.label"
        :width="column.width"
        :min-width="column.minWidth"
        show-overflow-tooltip
      />
      <el-table-column v-if="showCluster" label="聚类" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.cluster !== undefined" type="success">第 {{ row.cluster + 1 }} 类</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column v-if="showPredicted" prop="predicted" label="预测类别" min-width="120" show-overflow-tooltip />
    </el-table>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  rows: { type: Array, default: () => [] },
  showCluster: { type: Boolean, default: false },
  showPredicted: { type: Boolean, default: false },
})

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

const preferredOrder = [
  'id',
  'sepal_length',
  'sepal_width',
  'petal_length',
  'petal_width',
  'species',
]

const hiddenResultFields = computed(() => {
  const fields = new Set()
  if (props.showCluster) fields.add('cluster')
  if (props.showPredicted) fields.add('predicted')
  return fields
})

const columns = computed(() => {
  const keys = []
  props.rows.forEach((row) => {
    Object.keys(row || {}).forEach((key) => {
      if (!hiddenResultFields.value.has(key) && !keys.includes(key)) {
        keys.push(key)
      }
    })
  })

  keys.sort((left, right) => {
    const leftIndex = preferredOrder.indexOf(left)
    const rightIndex = preferredOrder.indexOf(right)
    if (leftIndex !== -1 || rightIndex !== -1) {
      return (leftIndex === -1 ? Number.MAX_SAFE_INTEGER : leftIndex)
        - (rightIndex === -1 ? Number.MAX_SAFE_INTEGER : rightIndex)
    }
    return 0
  })

  return keys.map((key) => ({
    prop: key,
    label: labelMap[key] || key,
    width: key === 'id' ? 70 : undefined,
    minWidth: key === 'id' ? undefined : 110,
  }))
})
</script>
