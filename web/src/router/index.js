import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'home', component: () => import('../views/Home.vue'), meta: { requiresAuth: true, roles:['ADMIN','TEACHER'], title: '控制台' } },
  { path: '/login', name: 'login', component: () => import('../views/Login.vue'), meta: { title: '登录' } }
  ,{ path: '/banks', name: 'banks', component: () => import('../views/Banks.vue'), meta: { requiresAuth: true, roles:['ADMIN','TEACHER'], title: '题库管理' } }
  ,{ path: '/assemble', name: 'assemble', component: () => import('../views/Assemble.vue'), meta: { requiresAuth: true, roles:['ADMIN','TEACHER'], title: '智能组卷' } }
  ,{ path: '/exams', name: 'exams', component: () => import('../views/Exams.vue'), meta: { requiresAuth: true, roles:['ADMIN','TEACHER'], title: '考务管理' } }
  ,{ path: '/exam/take', name: 'examTake', component: () => import('../views/ExamTake.vue'), meta: { requiresAuth: true, title: '参加考试' } }
  ,{ path: '/exam/success', name: 'examSuccess', component: () => import('../views/ExamSuccess.vue'), meta: { requiresAuth: true, title: '提交成功' } }
  ,{ path: '/records', name: 'records', component: () => import('../views/StudentRecords.vue'), meta: { requiresAuth: true, title: '我的成绩' } }
  ,{ path: '/exam-center', name: 'examCenter', component: () => import('../views/ExamCenter.vue'), meta: { requiresAuth: true, title: '考试中心' } }
  ,{ path: '/proctor', name: 'proctor', component: () => import('../views/Proctor.vue'), meta: { requiresAuth: true, roles:['ADMIN','TEACHER'], title: '违规监控' } }
  ,{ path: '/grading', name: 'grading', component: () => import('../views/Grading.vue'), meta: { requiresAuth: true, roles:['ADMIN','TEACHER'], title: '智能阅卷' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 缓存验证结果，避免频繁验证
let lastVerifyTime = 0
let verifyCache = null
const VERIFY_CACHE_DURATION = 5 * 60 * 1000 // 5分钟缓存

async function verifyToken() {
  const now = Date.now()
  // 如果缓存有效，直接返回
  if (verifyCache && (now - lastVerifyTime) < VERIFY_CACHE_DURATION) {
    return verifyCache
  }
  
  try {
    const httpModule = await import('../api/http')
    const { data } = await httpModule.default.get('/auth/me')
    const isValid = data && data.code === 0 && data.data && data.data.ok
    verifyCache = {
      valid: isValid,
      roles: isValid && data.data.roles ? data.data.roles : []
    }
    lastVerifyTime = now
    return verifyCache
  } catch (error) {
    verifyCache = { valid: false, roles: [] }
    lastVerifyTime = now
    return verifyCache
  }
}

router.beforeEach(async (to, from, next) => {
  const token = localStorage.getItem('token')
  const roles = JSON.parse(localStorage.getItem('roles')||'[]')
  
  // 如果已登录，访问登录页则跳转到首页
  if (to.path === '/login' && token) {
    const result = await verifyToken()
    if (result.valid) {
      // 更新角色信息
      if (result.roles && result.roles.length > 0) {
        localStorage.setItem('roles', JSON.stringify(result.roles))
      }
      return next('/')
    } else {
      // token无效，清除并允许访问登录页
      localStorage.removeItem('token')
      localStorage.removeItem('roles')
      localStorage.removeItem('userId')
      verifyCache = null
    }
  }
  
  // 需要认证的页面
  if (to.meta?.requiresAuth) {
    if (!token) {
      verifyCache = null
      return next('/login')
    }
    
    // 验证token有效性（带缓存）
    const result = await verifyToken()
    if (!result.valid) {
      // token无效，清除并跳转登录
      localStorage.removeItem('token')
      localStorage.removeItem('roles')
      localStorage.removeItem('userId')
      verifyCache = null
      return next('/login')
    }
    
    // 更新角色信息（可能已变更）
    if (result.roles && result.roles.length > 0) {
      localStorage.setItem('roles', JSON.stringify(result.roles))
    }
    
    // 检查角色权限
    if (to.meta?.roles && to.meta.roles.length > 0) {
      const currentRoles = result.roles.length > 0 ? result.roles : JSON.parse(localStorage.getItem('roles')||'[]')
      const ok = currentRoles.some(r => to.meta.roles.includes(r))
      if (!ok) {
        // 学生角色访问受限页面时，跳转到考试中心
        const isStudent = currentRoles.includes('STUDENT') && !currentRoles.some(r => ['ADMIN','TEACHER'].includes(r))
        return next(isStudent ? '/exam-center' : '/')
  }
    }
  }
  
  next()
})

export default router
