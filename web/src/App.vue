<template>
  <router-view v-if="route.path === '/login'" />

  <el-container v-else class="app-wrapper">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="app-aside">
      <div class="aside-logo" :class="{ 'collapsed': isCollapse }">
        <span class="logo-icon">🎓</span>
        <span v-show="!isCollapse" class="logo-text">智能考试系统</span>
      </div>

      <el-menu
          :default-active="route.path"
          router
          :collapse="isCollapse"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
          class="el-menu-vertical"
      >
        <el-menu-item v-if="can(['ADMIN','TEACHER'])" index="/">
          <el-icon><Odometer /></el-icon>
          <span>控制台</span>
        </el-menu-item>

        <el-menu-item v-if="can(['ADMIN','TEACHER'])" index="/banks">
          <el-icon><Files /></el-icon>
          <span>题库管理</span>
        </el-menu-item>

        <el-menu-item v-if="can(['ADMIN','TEACHER'])" index="/assemble">
          <el-icon><Cpu /></el-icon>
          <span>智能组卷</span>
        </el-menu-item>

        <el-menu-item index="/exam-center">
          <el-icon><Monitor /></el-icon>
          <span>考试中心</span>
        </el-menu-item>

        <el-menu-item v-if="can(['ADMIN','TEACHER'])" index="/exams">
          <el-icon><Timer /></el-icon>
          <span>考务管理</span>
        </el-menu-item>

        <el-menu-item v-if="can(['ADMIN','TEACHER'])" index="/grading">
          <el-icon><EditPen /></el-icon>
          <span>智能阅卷</span>
        </el-menu-item>

        <el-menu-item v-if="can(['ADMIN','TEACHER'])" index="/proctor">
          <el-icon><Warning /></el-icon>
          <span>违规监控</span>
        </el-menu-item>

        <el-menu-item index="/records">
          <el-icon><Trophy /></el-icon>
          <span>我的成绩</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="app-header">
        <div class="header-left">
          <el-icon class="hamburger" @click="toggleCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>系统</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentRouteName }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <div class="role-tag" v-if="currentRoles.length">
            <el-tag effect="plain" type="success" size="small" round>
              {{ roleNameDisplay }}
            </el-tag>
          </div>

          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="32" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
              <span class="username">{{ username }}</span>
              <el-icon><CaretBottom /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item divided command="logout" style="color: #f56c6c;">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="app-main-container">
        <router-view v-slot="{ Component, route: routeData }">
          <transition name="fade-transform" mode="out-in" appear>
            <component :is="Component" :key="routeData.path" v-if="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
// 引入图标
import {
  Odometer, Files, Cpu, Monitor, Timer, EditPen,
  Warning, Trophy, Fold, Expand, CaretBottom
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

// 状态
const isCollapse = ref(false)
const username = ref('User') // 实际应从 localStorage 或 Store 获取

// 切换侧边栏
const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

// 获取当前页面名称（用于面包屑）
const currentRouteName = computed(() => route.meta?.title || route.name || '当前页面')

// 角色逻辑优化
const currentRoles = computed(() => {
  try {
    const rolesStr = localStorage.getItem('roles')
    return rolesStr ? JSON.parse(rolesStr) : []
  } catch (e) {
    return []
  }
})

const roleNameDisplay = computed(() => {
  const map = { 'ADMIN': '管理员', 'TEACHER': '教师', 'STUDENT': '学生' }
  return currentRoles.value.map(r => map[r] || r).join(' / ')
})

const isAdmin = computed(() => currentRoles.value.includes('ADMIN'))

// 权限检查
const can = (allowedRoles) => {
  // 管理员拥有所有权限
  if (isAdmin.value) return true
  if (!allowedRoles || allowedRoles.length === 0) return true
  // 检查当前角色是否包含允许的角色
  const hasPermission = currentRoles.value.some(role => allowedRoles.includes(role))
  return hasPermission
}

// 下拉菜单处理
const handleCommand = (command) => {
  if (command === 'logout') {
    handleLogout()
  } else if (command === 'profile') {
    // router.push('/profile')
    ElMessage.info('开发中...')
  }
}

// 退出登录
const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    localStorage.clear() // 暴力清除所有，或者只清除特定的 key
    router.push('/login')
    ElMessage.success('已安全退出')
  }).catch(() => {})
}

onMounted(() => {
  const u = localStorage.getItem('username') // 假设存了用户名
  if (u) username.value = u

  // 简单的路由守卫检查
  if (route.path !== '/login' && !localStorage.getItem('token')) {
  router.push('/login')
}
  
  // 调试：打印当前角色信息
  console.log('[App] 当前角色:', currentRoles.value)
  console.log('[App] 是否管理员:', isAdmin.value)
  console.log('[App] 智能阅卷权限:', can(['ADMIN','TEACHER']))
})
</script>

<style scoped>
.app-wrapper {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

/* 侧边栏样式 */
.app-aside {
  background-color: #304156;
  transition: width 0.3s;
  overflow-x: hidden;
  display: flex;
  flex-direction: column;
}

.aside-logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #2b2f3a;
  color: #fff;
  transition: all 0.3s;
}
.aside-logo.collapsed .logo-text {
  display: none;
}
.logo-icon {
  font-size: 24px;
  margin-right: 8px;
}
.aside-logo.collapsed .logo-icon {
  margin-right: 0;
}
.logo-text {
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}

.el-menu-vertical {
  border-right: none;
  flex: 1;
}

/* Header 样式 */
.app-header {
  background-color: #fff;
  height: 60px;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.hamburger {
  font-size: 20px;
  cursor: pointer;
  color: #606266;
  transition: color 0.3s;
}
.hamburger:hover {
  color: #409EFF;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: #606266;
  padding: 4px 8px;
  border-radius: 4px;
}
.user-info:hover {
  background-color: #f5f7fa;
}
.username {
  margin: 0 6px 0 8px;
  font-size: 14px;
  font-weight: 500;
}

/* Main 内容区 */
.app-main-container {
  background-color: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
  position: relative;
}

/* 页面切换动画: Fade-Transform */
.fade-transform-leave-active,
.fade-transform-enter-active {
  transition: all 0.3s ease;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(10px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(-10px);
}

.fade-transform-enter-to,
.fade-transform-leave-from {
  opacity: 1;
  transform: translateX(0);
}
</style>