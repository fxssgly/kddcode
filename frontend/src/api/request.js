// 文件作用：集中封装前端访问后端的所有 HTTP 请求。
// 项目位置：前端 API 层，页面组件不直接写 URL 细节，而是调用这里的函数。
// 交互关系：Axios 把请求发给 Spring Boot；返回的数据再交给各个 Vue 页面渲染表格和图表。

import axios from 'axios'

// 统一的 Axios 实例。baseURL 为空时，请求会走当前前端开发服务器；
// vue.config.js 会把 /api 代理到 Spring Boot 后端。
const request = axios.create({
  baseURL: '',
  timeout: 15000,
})

// 注册接口：后端负责用户名去重和密码哈希。
export function registerUser(username, password) {
  return request.post('/api/auth/register', { username, password })
}

// 登录接口：成功后返回前端需要保存的用户基础信息。
export function loginUser(username, password) {
  return request.post('/api/auth/login', { username, password })
}

// 健康检查接口，用来确认后端服务是否可访问。
export function fetchHealth() {
  return request.get('/api/health')
}

// 获取 Iris 数据；dataset 参数区分聚类数据和分类数据。
export function fetchIris(dataset = 'clustering') {
  return request.get('/api/iris', { params: { dataset } })
}

// 上传 Iris CSV。FormData 与后端 MultipartFile 参数保持一致。
export function uploadIris(file) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/api/iris/upload', form)
}

// 获取默认事务篮子数据，用于关联规则分析。
export function fetchTransactions() {
  return request.get('/api/transactions')
}

// 上传事务 CSV，后端会解析成 List<List<String>> 结构。
export function uploadTransactions(file) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/api/transactions/upload', form)
}

// 上传回归 CSV 后由后端解析，但当前回归页主要使用固定数据集。
export function uploadRegression(file) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/api/regression/upload', form)
}

/**
 * 读取前端静态目录中的回归 CSV。
 *
 * 这里指定 responseType 和 transformResponse，是为了拿到原始文本，
 * 再由前端 parseRegressionCsv 转成表格和后端算法需要的 rows。
 */
export function fetchRegressionRows() {
  return request.get('/data/regression_experiment.csv', {
    responseType: 'text',
    transformResponse: [(data) => data],
  }).then((response) => {
    const rows = parseRegressionCsv(response.data)
    return {
      data: {
        total: rows.length,
        rows,
      },
    }
  })
}

/**
 * 解析简单 CSV：第一行作为表头，其余行转换为 {id, x, y, type}。
 * 这个解析器只面向当前实验数据集，不处理带引号和逗号转义的复杂 CSV。
 */
function parseRegressionCsv(text) {
  const lines = String(text || '').replace(/^\uFEFF/, '').split(/\r?\n/).filter((line) => line.trim())
  const headers = lines.shift()?.split(',').map((header) => header.trim()) || []
  return lines.map((line, index) => {
    const values = line.split(',').map((value) => value.trim())
    const row = Object.fromEntries(headers.map((header, headerIndex) => [header, values[headerIndex]]))
    return {
      id: Number(row.id || row.ID || index + 1),
      x: Number(row.x || row.X || 0),
      y: Number(row.y || row.Y || 0),
      type: row.type || row.Type || '正常点',
    }
  })
}

// 提交关联规则参数，后端会返回频繁项集、二项指标和规则列表。
export function runAssociation(minSupport, minConfidence) {
  return request.post('/api/association', {
    min_support: minSupport,
    min_confidence: minConfidence,
  })
}

// 提交聚类个数 k，后端返回带 cluster 标签的数据行和聚类中心。
export function runClustering(k) {
  return request.post('/api/clustering', { k })
}

// 提交 CART 决策树参数，后端返回预测结果、树结构和评估指标。
export function runClassification(maxDepth, minLeaf) {
  return request.post('/api/classification', {
    max_depth: maxDepth,
    min_leaf: minLeaf,
  })
}

// 提交回归字段映射和可选 rows，后端返回三种模型的预测值和指标。
export function runRegression(xField = 'x', yField = 'y', rows = null) {
  const body = {
    x_field: xField,
    y_field: yField,
  }
  if (Array.isArray(rows)) body.rows = rows
  return request.post('/api/regression', body)
}
