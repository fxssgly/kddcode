import axios from 'axios'

const request = axios.create({
  baseURL: '',
  timeout: 15000,
})

export function fetchHealth() {
  return request.get('/api/health')
}

export function fetchIris(dataset = 'clustering') {
  return request.get('/api/iris', { params: { dataset } })
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

export function uploadRegression(file) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/api/regression/upload', form)
}

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

export function runRegression(xField = 'x', yField = 'y', rows = null) {
  const body = {
    x_field: xField,
    y_field: yField,
  }
  if (Array.isArray(rows)) body.rows = rows
  return request.post('/api/regression', body)
}
