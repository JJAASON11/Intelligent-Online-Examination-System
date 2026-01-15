import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// 使用环境变量配置 baseURL，避免硬编码
const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 10000 // 增加超时设置
})

// 调试：打印请求
let printedBase = false
http.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  config.headers['Accept'] = 'application/json'
  config.headers['X-Requested-With'] = 'XMLHttpRequest'
  if (!printedBase) { console.info('[HTTP] baseURL =', http.defaults.baseURL); printedBase = true }
  console.debug('[HTTP] request', { method: config.method, url: (config.baseURL||'') + (config.url||''), headers: config.headers, params: config.params, data: config.data })
  return config
}, error => Promise.reject(error))

http.interceptors.response.use(
    response => {
      // 假设后端统一返回 { code: 0, data: ..., message: ... }
      const res = response.data
      // 如果 code 不为 0，视为业务错误（根据你的后端 ApiResponse 调整）
      if (res.code !== undefined && res.code !== 0) {
        ElMessage.error(res.message || '系统错误')
        // 40100 是你后端定义的未认证状态码
        if (res.code === 40100) {
          localStorage.removeItem('token')
          localStorage.removeItem('roles')
          router.push('/login')
        }
        return Promise.reject(new Error(res.message || 'Error'))
      }
      return response
    },
    error => {
      // 处理 HTTP 状态码错误
      let msg = '网络连接失败'
      
      if (error.response) {
        // 服务器返回了响应，但状态码不在 2xx 范围内
        msg = error.response.data?.message || `服务器错误: ${error.response.status}`
      } else if (error.request) {
        // 请求已发出，但没有收到响应
        msg = '无法连接到服务器，请检查后端服务是否启动（端口8080）'
        console.error('请求失败，未收到响应:', error.request)
      } else {
        // 请求配置出错
        msg = error.message || '请求配置错误'
      }
      
      ElMessage.error(msg)
      
      if (error.response?.status === 401 || error.response?.status === 403) {
        localStorage.removeItem('token')
        localStorage.removeItem('roles')
        localStorage.removeItem('userId')
        router.push('/login')
      }
      return Promise.reject(error)
    }
)

export default http
