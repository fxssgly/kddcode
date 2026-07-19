// 文件作用：接口请求层，集中封装前端访问后端的 HTTP 请求。
// 关联文件：各 views 页面只调用这里的函数，不直接写 axios 地址；vue.config.js 在开发环境把 /api 代理到后端。
import axios from 'axios'

// request 是 axios 实例；统一设置 baseURL 和超时时间，后续接口函数复用它发送请求。
const request = axios.create({
  // baseURL 为空表示请求从当前前端站点发出；开发时 /api 会被 vue.config.js 代理到 http://127.0.0.1:5000。
  baseURL: '',
  // timeout = 15000 毫秒，超过 15 秒后 axios 会认为请求失败，页面可进入错误提示流程。
  timeout: 15000,
})

// 注册流程：LoginView.vue 点击注册 → registerUser → POST /api/auth/register → 后端返回 JSON。
export function registerUser(username, password) {
  return request.post('/api/auth/register', { username, password })
}

// 登录流程：LoginView.vue 点击登录 → loginUser → 后端校验账号密码 → 前端保存 user 到 localStorage。
export function loginUser(username, password) {
  return request.post('/api/auth/login', { username, password })
}

// 读取 Iris 数据；dataset 参数区分聚类或分类场景，后端据此返回对应数据。
export function fetchIris(dataset = 'clustering') {
  return request.get('/api/iris', { params: { dataset } })
}

// 上传 Iris CSV；FormData 用来按 multipart/form-data 格式传文件，这是浏览器上传文件的标准方式。
export function uploadIris(file, dataset = 'clustering') {
  const form = new FormData()
  form.append('file', file)
  return request.post('/api/iris/upload', form, { params: { dataset } })
}

// 读取关联规则实验的默认事务数据。
export function fetchTransactions() {
  return request.get('/api/transactions')
}

// 上传事务 CSV，页面上传后用后端解析出的 transactions 更新表格。
export function uploadTransactions(file) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/api/transactions/upload', form)
}

// 读取回归实验固定数据集。
export function fetchRegressionRows() {
  return request.get('/api/regression/data')
}

// 运行关联规则：页面传入支持度和置信度，后端返回规则列表和二项指标。
export function runAssociation(minSupport, minConfidence, transactions = null) {
  return request.post('/api/association', {
    min_support: minSupport,
    min_confidence: minConfidence,
    transactions,
  })
}

// 运行聚类：params 包含 method、rows、k、eps 等参数，后端返回聚类标签和中心点。
export function runClustering(params) {
  return request.post('/api/clustering', params)
}

// 运行分类：页面传树深和叶子样本数，后端返回预测结果、评估指标和树结构。
export function runClassification(maxDepth, minLeaf, rows = null) {
  return request.post('/api/classification', {
    max_depth: maxDepth,
    min_leaf: minLeaf,
    rows,
  })
}

// 运行回归：后端使用固定字段 x/y 做训练和预测，前端只负责展示 points 和 models。
export function runRegression() {
  return request.post('/api/regression', {
    x_field: 'x',
    y_field: 'y',
  })
}
