import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const http = axios.create({ baseURL: '/api', timeout: 20000 })

http.interceptors.request.use((cfg) => {
  const token = localStorage.getItem('vn_token')
  if (token) cfg.headers.Authorization = `Bearer ${token}`
  return cfg
})

// 统一业务码处理：401 回登录页，其余弹出后端提示
http.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && typeof body.code !== 'undefined' && body.code !== 0) {
      if (body.code === 401) {
        localStorage.removeItem('vn_token')
        router.push('/login')
      }
      ElMessage.error(body.msg || '请求失败')
      return Promise.reject(new Error(body.msg))
    }
    return body.data
  },
  (err) => {
    ElMessage.error(err.message || '网络异常')
    return Promise.reject(err)
  }
)

export default http

export const authApi = {
  login: (data) => http.post('/auth/login', data),
  me: () => http.get('/auth/me'),
  menus: () => http.get('/auth/menus'),
  updateProfile: (data) => http.put('/auth/profile', data),
  changePassword: (data) => http.put('/auth/password', data)
}

/** 场地档案 */
export const venueApi = {
  page: (params) => http.get('/campus-venue/page', { params }),
  options: () => http.get('/campus-venue/options'),
  detail: (id) => http.get(`/campus-venue/${id}`),
  items: (id) => http.get(`/campus-venue/${id}/items`),
  save: (data) => (data.id ? http.put(`/campus-venue/${data.id}`, data) : http.post('/campus-venue', data)),
  remove: (id) => http.delete(`/campus-venue/${id}`),
  saveItem: (data) => http.post('/campus-venue/item', data),
  dashboard: () => http.get('/campus-venue/dashboard')
}

/** 借用申请与审批 */
export const applyApi = {
  page: (params) => http.get('/venue-apply/page', { params }),
  detail: (id) => http.get(`/venue-apply/${id}`),
  save: (data) => (data.id ? http.put(`/venue-apply/${data.id}`, data) : http.post('/venue-apply', data)),
  submit: (id) => http.post(`/venue-apply/${id}/submit`),
  approve: (id, data) => http.post(`/venue-apply/${id}/approve`, data),
  transition: (id, data) => http.post(`/venue-apply/${id}/transition`, data),
  reserve: (id, data) => http.post(`/venue-apply/${id}/reserve`, data),
  conflicts: (id) => http.get(`/venue-apply/conflicts/${id}`),
  calendar: (params) => http.get('/venue-apply/calendar', { params })
}

/** 现场作业 */
export const opsApi = {
  handovers: (params) => http.get('/venue-ops/handover/page', { params }),
  handover: (id, data) => http.post(`/venue-ops/${id}/handover`, data),
  patrols: (params) => http.get('/venue-ops/patrol/page', { params }),
  patrol: (id, data) => http.post(`/venue-ops/${id}/patrol`, data),
  accepts: (params) => http.get('/venue-ops/accept/page', { params }),
  accept: (id, data) => http.post(`/venue-ops/${id}/accept`, data),
  reviewAccept: (id, data) => http.post(`/venue-ops/accept/${id}/review`, data),
  fees: (params) => http.get('/venue-ops/fee/page', { params }),
  saveFee: (data) => http.post('/venue-ops/fee', data)
}

/** 创新点：准备清单 */
export const prepApi = {
  rules: () => http.get('/venue-prep/rule'),
  saveRules: (data) => http.put('/venue-prep/rule', data),
  evaluate: (id) => http.post(`/venue-prep/${id}/evaluate`),
  latest: (id) => http.get(`/venue-prep/latest/${id}`),
  adjust: (id, data) => http.post(`/venue-prep/score/${id}/adjust`, data),
  board: () => http.get('/venue-prep/board')
}

export const systemApi = {
  users: (params) => http.get('/system/user/page', { params }),
  saveUser: (data) => http.post('/system/user', data),
  resetPwd: (id) => http.post(`/system/user/${id}/reset`),
  roles: () => http.get('/system/role'),
  saveRole: (data) => http.post('/system/role', data),
  menus: () => http.get('/system/menu'),
  assign: (id, menuIds) => http.post(`/system/role/${id}/menus`, { menuIds }),
  dicts: (type) => http.get('/system/dict', { params: { type } }),
  messages: (params) => http.get('/system/message/page', { params }),
  readMsg: (id) => http.post(`/system/message/${id}/read`),
  logs: (params) => http.get('/system/log/page', { params }),
  upload: (form) => http.post('/system/upload', form)
}

export const dashboardApi = { index: () => http.get('/dashboard') }
