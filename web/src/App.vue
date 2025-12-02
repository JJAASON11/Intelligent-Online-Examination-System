<template>
  <router-view v-if="route.path === '/login'" />

  <el-container v-else style="height: 100vh;">
    <el-aside width="220px" class="app-aside">
      <div class="aside-logo">🎓 智能考试</div>
      <el-menu :default-active="active" router>
        <el-menu-item index="/">首页</el-menu-item>
        <el-menu-item v-if="can(['ADMIN','TEACHER'])" index="/banks">题库管理</el-menu-item>
        <el-menu-item v-if="can(['ADMIN','TEACHER'])" index="/assemble">自动组卷</el-menu-item>
        <el-menu-item v-if="can(['ADMIN','TEACHER','STUDENT'])" index="/exams">考试与监考</el-menu-item>
        <el-menu-item v-if="can(['ADMIN','TEACHER'])" index="/grading">智能阅卷</el-menu-item>
        <el-menu-item v-if="can(['ADMIN','PROCTOR'])" index="/proctor">在线监考WS</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="app-header">
        <div class="header-inner">
          <span class="logo-text">智能在线考试系统</span>
          <el-button type="danger" link @click="logout" size="small">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { computed } from 'vue'
const route = useRoute()
const router = useRouter()
const active = computed(()=> route.path)
const roles = computed(()=> JSON.parse(localStorage.getItem('roles')||'[]'))
const can = (rs)=> roles.value.some(r=>rs.includes(r))

// 简单的退出功能
const logout = () => {
  localStorage.removeItem('token')
  router.push('/login')
}
</script>

<style>
/* 全局重置 */
body { margin: 0; padding: 0; background-color: #f5f7fa; font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', '微软雅黑', Arial, sans-serif; }

.app-header {
  background-color: #fff;
  border-bottom: 1px solid #dcdfe6;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  line-height: 60px;
}
.app-aside{ background:#fff; border-right:1px solid #dcdfe6; }
.aside-logo{ padding:16px; font-weight:bold; color:#409EFF; }
.header-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 1200px;
  margin: 0 auto;
  height: 100%;
}
.logo-text {
  font-size: 20px;
  font-weight: bold;
  color: #409EFF;
}
.app-main {
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
  padding-top: 20px;
}
</style>
