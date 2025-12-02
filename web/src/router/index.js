import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'home', component: () => import('../views/Home.vue'), meta: { requiresAuth: true } },
  { path: '/login', name: 'login', component: () => import('../views/Login.vue') }
  ,{ path: '/banks', name: 'banks', component: () => import('../views/Banks.vue'), meta: { requiresAuth: true, roles:['ADMIN','TEACHER'] } }
  ,{ path: '/assemble', name: 'assemble', component: () => import('../views/Assemble.vue'), meta: { requiresAuth: true, roles:['ADMIN','TEACHER'] } }
  ,{ path: '/exams', name: 'exams', component: () => import('../views/Exams.vue'), meta: { requiresAuth: true } }
  ,{ path: '/exam/take', name: 'examTake', component: () => import('../views/ExamTake.vue'), meta: { requiresAuth: true } }
  ,{ path: '/exam/success', name: 'examSuccess', component: () => import('../views/ExamSuccess.vue'), meta: { requiresAuth: true } }
  ,{ path: '/records', name: 'records', component: () => import('../views/StudentRecords.vue'), meta: { requiresAuth: true } }
  ,{ path: '/proctor', name: 'proctor', component: () => import('../views/Proctor.vue'), meta: { requiresAuth: true, roles:['ADMIN','PROCTOR'] } }
  ,{ path: '/grading', name: 'grading', component: () => import('../views/Grading.vue'), meta: { requiresAuth: true, roles:['ADMIN','TEACHER'] } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const roles = JSON.parse(localStorage.getItem('roles')||'[]')
  if (to.path === '/login' && token) return next('/')
  if (to.meta?.requiresAuth && !token) return next('/login')
  if (to.meta?.roles && to.meta.roles.length>0) {
    const ok = roles.some(r => to.meta.roles.includes(r))
    if (!ok) return next('/')
  }
  next()
})

export default router
