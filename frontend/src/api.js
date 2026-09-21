import axios from 'axios'

const http = axios.create({ baseURL: '/api', timeout: 20000 })

http.interceptors.response.use(
  (resp) => resp.data,
  (err) => {
    const msg = err?.response?.data?.message || err.message || '请求失败'
    return Promise.reject(new Error(msg))
  }
)

function unwrap(promise) {
  return promise.then((body) => {
    if (body && body.ok === false) {
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body?.data
  })
}

export const materialCategoryApi = {
  tree: () => unwrap(http.get('/material-categories/tree')),
  children: (parentId) => unwrap(http.get(`/material-categories/${parentId}/children`)),
  create: (payload) => unwrap(http.post('/material-categories', payload)),
  remove: (id) => unwrap(http.delete(`/material-categories/${id}`))
}

export const materialApi = {
  list: (params) => unwrap(http.get('/materials', { params })),
  create: (payload) => unwrap(http.post('/materials', payload)),
  update: (id, payload) => unwrap(http.put(`/materials/${id}`, payload)),
  consume: (id, amount) => unwrap(http.post(`/materials/${id}/consume`, null, { params: { amount } })),
  remove: (id) => unwrap(http.delete(`/materials/${id}`))
}

export const workAreaApi = {
  tree: () => unwrap(http.get('/work-areas/tree')),
  list: () => unwrap(http.get('/work-areas')),
  create: (payload) => unwrap(http.post('/work-areas', payload)),
  remove: (id) => unwrap(http.delete(`/work-areas/${id}`))
}

export const greenwareApi = {
  list: (params) => unwrap(http.get('/greenwares', { params })),
  create: (payload) => unwrap(http.post('/greenwares', payload)),
  update: (id, payload) => unwrap(http.put(`/greenwares/${id}`, payload)),
  stage: (id, stage) => unwrap(http.post(`/greenwares/${id}/stage`, null, { params: { stage } })),
  move: (id, areaId) => unwrap(http.post(`/greenwares/${id}/move`, null, { params: { areaId } })),
  remove: (id) => unwrap(http.delete(`/greenwares/${id}`))
}

export const kilnApi = {
  list: () => unwrap(http.get('/kilns')),
  create: (payload) => unwrap(http.post('/kilns', payload))
}

export const firingBatchApi = {
  list: (params) => unwrap(http.get('/firing-batches', { params })),
  greenwares: (id) => unwrap(http.get(`/firing-batches/${id}/greenwares`)),
  create: (payload) => unwrap(http.post('/firing-batches', payload)),
  stage: (id, stage, peakTemp) =>
    unwrap(http.post(`/firing-batches/${id}/stage`, null, { params: { stage, peakTemp } })),
  load: (id, greenwareId) =>
    unwrap(http.post(`/firing-batches/${id}/load`, null, { params: { greenwareId } })),
  remove: (id) => unwrap(http.delete(`/firing-batches/${id}`))
}

export const courseApi = {
  list: () => unwrap(http.get('/courses')),
  create: (payload) => unwrap(http.post('/courses', payload)),
  enroll: (id, count) => unwrap(http.post(`/courses/${id}/enroll`, null, { params: { count } })),
  remove: (id) => unwrap(http.delete(`/courses/${id}`))
}

export const artworkApi = {
  list: (params) => unwrap(http.get('/artworks', { params })),
  create: (payload) => unwrap(http.post('/artworks', payload)),
  owner: (id, ownerStatus, consignPrice) =>
    unwrap(http.post(`/artworks/${id}/owner`, null, { params: { ownerStatus, consignPrice } })),
  remove: (id) => unwrap(http.delete(`/artworks/${id}`))
}

export default http
