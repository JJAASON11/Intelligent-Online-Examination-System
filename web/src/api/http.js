import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// 使用环境变量配置 baseURL，避免硬编码
const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 10000 // 增加超时设置
})

http.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
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
      const msg = error.response?.data?.message || error.message || '网络连接失败'
      ElMessage.error(msg)
      if (error.response?.status === 401) {
        router.push('/login')
      }
      return Promise.reject(error)
    }
)

export default http