<template>
  <div class="exam-center-container">
    <el-page-header content="考试中心" @back="$router.back()" title="返回首页" class="page-header" />

    <el-card shadow="never" class="filter-card">
      <div class="filter-bar">
        <div class="left-filter">
          <span class="label">选择考试日期：</span>
          <el-date-picker
              v-model="queryDate"
              type="date"
              placeholder="选择日期"
              value-format="YYYY-MM-DD"
              :clearable="false"
              @change="loadData"
          />
          <el-button type="primary" :icon="Refresh" @click="loadData" :loading="loading">刷新列表</el-button>
        </div>
        <div class="right-info">
          <el-tag type="info" effect="plain"><el-icon><Clock /></el-icon> 当前系统时间：{{ currentTimeStr }}</el-tag>
        </div>
      </div>
    </el-card>

    <div class="exam-list" v-loading="loading">
      <el-empty v-if="!loading && list.length === 0" description="当前日期暂无考试安排" />

      <el-row :gutter="20" v-else>
        <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="item in list" :key="item.id">
          <el-card class="exam-card" shadow="hover" :class="{ 'is-active': getExamStatus(item).type === 'success' }">
            <div class="card-header">
              <div class="exam-icon">
                <el-icon><img v-if="false" src="" /><Document v-else /></el-icon>
              </div>
              <div class="exam-meta">
                <div class="exam-title">考试场次 #{{ item.id }}</div>
                <div class="paper-id">试卷编号: {{ item.paperId }}</div>
              </div>
              <div class="status-badge">
                <el-tag :type="getExamStatus(item).type" effect="dark" size="small">
                  {{ getExamStatus(item).text }}
                </el-tag>
              </div>
            </div>

            <el-divider style="margin: 12px 0;" />

            <div class="card-body">
              <div class="time-row">
                <span class="label">开始：</span>
                <span class="value">{{ formatTime(item.startAt) }}</span>
              </div>
              <div class="time-row">
                <span class="label">结束：</span>
                <span class="value">{{ formatTime(item.endAt) }}</span>
              </div>
            </div>

            <div class="card-footer">
              <el-button
                  type="primary"
                  class="enter-btn"
                  :disabled="getExamStatus(item).disabled"
                  :type="getExamStatus(item).btnType"
                  @click="enterExam(item)"
              >
                {{ getExamStatus(item).btnText }}
                <el-icon class="el-icon--right"><Right /></el-icon>
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api/http'
import { Refresh, Clock, Document, Right } from '@element-plus/icons-vue'
import dayjs from 'dayjs' // 如果项目没装dayjs，可以用原生Date代替，下面代码已做兼容

const router = useRouter()
const list = ref([])
const loading = ref(false)
const queryDate = ref(dayjs().format('YYYY-MM-DD'))
const currentTimeStr = ref('')
let timer = null

// --- 核心逻辑 ---

// 计算当前考试状态
function getExamStatus(row) {
  const now = Date.now()
  const start = new Date(row.startAt).getTime()
  const end = new Date(row.endAt).getTime()

  if (now < start) {
    return { type: 'info', text: '未开始', btnText: '等待开始', btnType: 'info', disabled: true }
  } else if (now > end) {
    return { type: 'danger', text: '已结束', btnText: '无法进入', btnType: 'danger', disabled: true }
  } else {
    return { type: 'success', text: '进行中', btnText: '进入考试', btnType: 'primary', disabled: false }
  }
}

// 时间格式化
function formatTime(isoStr) {
  return dayjs(isoStr).format('HH:mm') // 只显示时分，因为外层已选了日期
}

// 进入考试
function enterExam(row) {
  // 使用 router.push 而不是 location.href，体验更好
  router.push({
    path: '/exam/take',
    query: {
      sessionId: row.id,
      paperId: row.paperId,
      // 如果后端没有返回duration，这里可以暂时不传或传默认值
      // duration: 60
    }
  })
}

// 加载数据
async function loadData() {
  loading.value = true
  list.value = []
  try {
    // 优先尝试按日期查询
    const { data } = await http.get('/exams/sessions/available', { params: { date: queryDate.value } })
    list.value = data?.data || []
  } catch (e) {
    // 降级策略：查询所有开放的会话（保留原逻辑）
    try {
      const { data } = await http.get('/exams/sessions/open')
      list.value = data?.data || []
    } catch (err) {
      list.value = []
    }
  } finally {
    loading.value = false
  }
}

// 更新右上角系统时间
function startClock() {
  const update = () => currentTimeStr.value = dayjs().format('HH:mm:ss')
  update()
  timer = setInterval(update, 1000)
}

onMounted(() => {
  loadData()
  startClock()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.exam-center-container {
  padding: 24px;
  background-color: #f6f8f9;
  min-height: 100vh;
}

.page-header {
  margin-bottom: 20px;
}

/* 筛选栏 */
.filter-card {
  margin-bottom: 24px;
  border-radius: 8px;
  border: none;
}
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}
.left-filter {
  display: flex;
  align-items: center;
  gap: 12px;
}
.label {
  font-size: 14px;
  color: #606266;
}

/* 考试卡片 */
.exam-card {
  border-radius: 12px;
  border: 1px solid #ebeef5;
  transition: all 0.3s;
  margin-bottom: 20px;
  position: relative;
  overflow: hidden;
}
.exam-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 20px rgba(0,0,0,0.08);
}

/* 进行中的考试高亮边框 */
.exam-card.is-active {
  border-color: #b3e19d;
  background: #f0f9eb;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.exam-icon {
  width: 48px;
  height: 48px;
  background: #ecf5ff;
  color: #409eff;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}
.is-active .exam-icon {
  background: #67c23a;
  color: #fff;
}

.exam-meta {
  flex: 1;
}
.exam-title {
  font-weight: bold;
  font-size: 16px;
  color: #303133;
  margin-bottom: 4px;
}
.paper-id {
  font-size: 12px;
  color: #909399;
}

.card-body {
  padding: 0 4px;
}
.time-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  margin-bottom: 8px;
  color: #606266;
}
.time-row .value {
  font-weight: 500;
  font-family: 'Helvetica Neue', sans-serif;
}

.card-footer {
  margin-top: 16px;
}
.enter-btn {
  width: 100%;
  font-weight: bold;
  border-radius: 8px;
  height: 40px;
}
</style>