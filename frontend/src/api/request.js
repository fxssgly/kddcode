import axios from 'axios'

const request = axios.create({
  baseURL: '',
  timeout: 15000,
})

export function fetchHealth() {
  return request.get('/api/health')
}

export function fetchIris() {
  return request.get('/api/iris')
}

export function uploadIris(file) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/api/iris/upload', form)
}

export function fetchTransactions() {
  return request.get('/api/transactions')
}

export function uploadTransactions(file) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/api/transactions/upload', form)
}

export function runAssociation(minSupport, minConfidence) {
  return request.post('/api/association', {
    min_support: minSupport,
    min_confidence: minConfidence,
  })
}

export function runClustering(k) {
  return request.post('/api/clustering', { k })
}

export function runClassification(maxDepth, minLeaf) {
  return request.post('/api/classification', {
    max_depth: maxDepth,
    min_leaf: minLeaf,
  })
}

export function runRegression(xField, yField) {
  return request.post('/api/regression', {
    x_field: xField,
    y_field: yField,
  })
}
