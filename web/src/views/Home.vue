<template>
  <div class="dashboard-container">
    <div class="welcome-header">
      <div class="header-content">
        <h2>👋 欢迎使用智能在线考试系统</h2>
        <p class="subtitle">高效、智能、便捷的教考一体化解决方案</p>
      </div>
      <div class="system-status">
        <el-tag :type="pingOk ? 'success' : 'danger'" effect="dark" round class="status-tag">
          <el-icon class="is-loading" v-if="loading"><Loading /></el-icon>
          <el-icon v-else><Connection /></el-icon>
          {{ pingOk ? '系统服务正常' : '服务断开 / 未连接' }}
        </el-tag>
        <el-button circle size="small" :icon="Refresh" @click="ping" title="重新检测连接" :loading="loading" />
      </div>
    </div>

    <el-row :gutter="20" class="nav-grid">
      <template v-for="item in menuItems" :key="item.path">
        <el-col :xs="24" :sm="12" :md="6" :lg="6" v-if="can(item.roles)">
          <el-card class="nav-card" shadow="hover" @click="$router.push(item.path)">
            <div class="card-content">
              <div class="icon-wrapper" :style="{ background: item.bgColor, color: item.color }">
                <el-icon><component :is="item.icon" /></el-icon>
              </div>
              <div class="text-wrapper">
                <h3>{{ item.title }}</h3>
                <p>{{ item.desc }}</p>
              </div>
            </div>
            <div class="card-arrow">
              <el-icon><Right /></el-icon>
            </div>
          </el-card>
        </el-col>
      </template>
    </el-row>

    <el-empty v-if="!hasAnyPermission" description="暂无可用功能，请联系管理员分配权限" />

    <!-- 豆包聊天框：控制台固定入口 -->
    <div class="ai-chat-entry">
      <el-card class="ai-card" shadow="hover">
        <div class="ai-header">
          <div class="ai-title">
            <span class="dot"></span>
            <span>豆包 AI 聊天助手</span>
          </div>
          <el-button size="small" type="primary" @click="chatOpen = true">
            打开聊天框
          </el-button>
        </div>
        <p class="ai-desc">可以先发一句“你好”测试豆包是否可用，也可以向它咨询考试系统相关问题。</p>
      </el-card>
    </div>

    <el-dialog
      v-model="chatOpen"
      title="豆包聊天测试"
      width="720px"
    >
      <div class="chat-wrap">
        <div class="chat-history">
          <div v-if="chatMessages.length === 0" class="chat-empty">
            <el-empty description="先发一句话测试豆包是否可用（例如：你好）" />
          </div>
          <div v-else>
            <div
              v-for="(m, i) in chatMessages"
              :key="i"
              class="chat-msg"
              :class="m.role"
            >
              <div class="chat-role">{{ m.role === 'user' ? '我' : '豆包' }}</div>
              <div class="chat-text" v-html="formatText(m.text)"></div>
            </div>
          </div>
        </div>

        <div class="chat-input">
          <el-input
            v-model="chatInput"
            type="textarea"
            :rows="3"
            placeholder="输入一句话，发送测试（建议先发：你好）"
            :disabled="chatLoading"
          />
          <div class="chat-actions">
            <el-button @click="chatOpen = false" :disabled="chatLoading">关闭</el-button>
            <el-button type="primary" :loading="chatLoading" @click="sendChat">
              发送
            </el-button>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import http from '../api/http'
import {
  Connection, Refresh, Loading, Right,
  Files, MagicStick, Monitor, Medal
} from '@element-plus/icons-vue'

// --- 状态管理 ---
const pingOk = ref(false)
const loading = ref(false)
const roles = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('roles') || '[]')
  } catch (e) {
    return []
  }
})

// --- 权限辅助 ---
const can = (rs) => roles.value.some(r => rs.includes(r))

// --- 菜单配置 (将数据与视图分离，易于维护) ---
const menuItems = [
  {
    title: '题库管理',
    desc: '试题录入、编辑与分类管理',
    path: '/banks',
    roles: ['ADMIN', 'TEACHER'],
    icon: 'Files',
    color: '#409eff',
    bgColor: '#ecf5ff'
  },
  {
    title: '自动组卷',
    desc: '配置策略，一键生成试卷',
    path: '/assemble',
    roles: ['ADMIN', 'TEACHER'],
    icon: 'MagicStick',
    color: '#b37feb',
    bgColor: '#f9f0ff'
  },
  {
    title: '考试与监考',
    desc: '在线考试、实时监考与防作弊',
    path: '/exams',
    roles: ['ADMIN', 'TEACHER', 'STUDENT'],
    icon: 'Monitor',
    color: '#67c23a',
    bgColor: '#f0f9eb'
  },
  {
    title: '智能阅卷',
    desc: '自动批改客观题，辅助批改主观题',
    path: '/grading',
    roles: ['ADMIN', 'TEACHER'],
    icon: 'Medal',
    color: '#e6a23c',
    bgColor: '#fdf6ec'
  }
]

// 计算是否有任意权限显示（用于显示空状态）
const hasAnyPermission = computed(() => menuItems.some(item => can(item.roles)))

// --- 豆包聊天 ---
const chatOpen = ref(false)
const chatInput = ref('')
const chatLoading = ref(false)
const chatMessages = ref([]) // { role: 'user'|'assistant', text: string }[]

function formatText(text) {
  if (!text) return ''
  return String(text).replace(/\n/g, '<br>')
}

async function sendChat() {
  if (chatLoading.value) return
  const q = String(chatInput.value || '').trim()
  if (!q) {
    return
  }
  chatMessages.value.push({ role: 'user', text: q })
  chatInput.value = ''
  chatLoading.value = true
  try {
    const { data } = await http.post('/ai/chat', { question: q }, { timeout: 60000 })
    chatMessages.value.push({ role: 'assistant', text: data?.data || '（无返回）' })
  } catch (e) {
    chatMessages.value.push({ role: 'assistant', text: '（请求失败，请稍后重试）' })
  } finally {
    chatLoading.value = false
  }
}

// --- 动作 ---
async function ping() {
  loading.value = true
  try {
    const { data } = await http.get('/health')
    // 假设后端返回 true 或 specific code 表示健康
    pingOk.value = !!data
  } catch (e) {
    pingOk.value = false
  } finally {
    loading.value = false
  }
}

// 页面加载时自动检测一次
onMounted(() => {
  ping()
})
</script>

<style scoped>
.dashboard-container {
  padding: 24px;
  background-color: #f6f8f9; /* 浅灰底色，更护眼 */
  min-height: 100vh;
}

/* 欢迎头部 */
.welcome-header {
  background: white;
  padding: 24px 32px;
  border-radius: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.03);
  margin-bottom: 24px;
}

.header-content h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  color: #303133;
}

.header-content .subtitle {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.system-status {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-tag {
  padding: 16px 20px;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 导航卡片 */
.nav-grid {
  margin-top: 16px;
}

.nav-card {
  border: none;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  height: 100%;
  position: relative;
  overflow: hidden;
}

.nav-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08);
}

.card-content {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 12px;
}

.icon-wrapper {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  margin-bottom: 16px;
  transition: transform 0.3s;
}

.nav-card:hover .icon-wrapper {
  transform: scale(1.1);
}

.text-wrapper h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  color: #303133;
}

.text-wrapper p {
  margin: 0;
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
}

/* 卡片右下角箭头 */
.card-arrow {
  position: absolute;
  right: 20px;
  top: 20px;
  color: #dcdfe6;
  font-size: 20px;
  transition: all 0.3s;
}

.nav-card:hover .card-arrow {
  color: #409eff;
  transform: translateX(4px);
}

/* AI 聊天入口卡片 */
.ai-chat-entry {
  margin-top: 24px;
}

.ai-card {
  border-radius: 12px;
}

.ai-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.ai-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #303133;
}

.ai-title .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #67c23a;
  box-shadow: 0 0 0 6px rgba(103, 194, 58, 0.2);
}

.ai-desc {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

/* 聊天弹窗样式，复用成绩页的风格以统一体验 */
.chat-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chat-history {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  max-height: 360px;
  overflow: auto;
  background: #fafafa;
}

.chat-msg {
  display: flex;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px dashed #ebeef5;
}

.chat-msg:last-child {
  border-bottom: none;
}

.chat-role {
  width: 46px;
  flex: 0 0 46px;
  font-weight: 600;
  color: #606266;
}

.chat-text {
  flex: 1;
  color: #303133;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.chat-input {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.chat-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>