<template>
  <div class="exam-record-container">
    <el-page-header content="我的考试记录" @back="$router.back()" class="page-header" />

    <el-card shadow="hover" class="main-card">
      <div class="toolbar">
        <div class="left-tools">
          <el-input
              v-model="searchKeyword"
              placeholder="搜索试卷名称..."
              prefix-icon="Search"
              clearable
              style="width: 240px"
          />
          <el-select
              v-model="filterStatus"
              placeholder="考试状态"
              clearable
              style="width: 140px; margin-left: 12px;"
          >
            <el-option label="进行中" value="START" />
            <el-option label="已提交" value="SUBMIT" />
            <el-option label="已阅卷" value="REVIEWED" />
          </el-select>
        </div>
        <el-button type="primary" :icon="Refresh" @click="handleRefresh" :loading="loading">
          刷新列表
        </el-button>
      </div>

      <el-table
          v-loading="loading"
          :data="pagedSessions"
          style="width: 100%; margin-top: 20px;"
          stripe
      >
        <el-table-column label="ID" prop="id" min-width="80" align="center" />

        <el-table-column label="试卷名称" min-width="260">
          <template #default="{ row }">
            <span class="paper-title">{{ getPaperName(row.paperId) }}</span>
            <div class="time-info">
              {{ formatDateTime(row.startAt) }}
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" min-width="140" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" effect="light" round>
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="得分情况" min-width="200" align="center">
          <template #default="{ row }">
            <div v-if="sessionSummaries[row.id]" class="score-display">
              <span class="total-score">{{ sessionSummaries[row.id].total || 0 }}<span class="unit">分</span></span>
              <div class="score-detail">
                <span>客:{{ sessionSummaries[row.id].objective || 0 }}</span>
                <el-divider direction="vertical" />
                <span>主:{{ sessionSummaries[row.id].subjective || 0 }}</span>
              </div>
            </div>
            <span v-else-if="row.status === 'REVIEWED'" class="loading-text">加载中...</span>
            <span v-else class="no-score">未评分</span>
          </template>
        </el-table-column>

        <el-table-column label="答题进度" min-width="220">
          <template #default="{ row }">
            <div v-if="sessionProgress[row.id]" class="progress-wrapper">
              <el-progress
                  :percentage="sessionProgress[row.id].progress"
                  :status="sessionProgress[row.id].progress === 100 ? 'success' : ''"
                  :stroke-width="8"
              />
              <span class="progress-text">
                已答 {{ sessionProgress[row.id].answered }} / {{ sessionProgress[row.id].total }}
              </span>
            </div>
            <span v-else class="loading-text">计算中...</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" min-width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
                size="small"
                type="primary"
                link
                :icon="Document"
                @click="viewAnswers(row)"
            >
              答卷
            </el-button>
            <el-divider direction="vertical" />
            <el-button
                size="small"
                type="primary"
                link
                :icon="DataAnalysis"
                @click="viewDetails(row)"
            >
              详情
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无考试记录" />
        </template>
      </el-table>

      <div class="pagination-container">
        <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="filteredSessions.length"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog
        v-model="detailDialogVisible"
        title="考试详情概览"
        width="600px"
        align-center
    >
      <div v-if="currentSession" class="dialog-content">
        <div class="dialog-header-info">
          <h3>{{ getPaperName(currentSession.paperId) }}</h3>
          <el-tag :type="getStatusType(currentSession.status)">
            {{ getStatusText(currentSession.status) }}
          </el-tag>
        </div>

        <el-descriptions :column="2" border class="mt-4">
          <el-descriptions-item label="会话 ID">{{ currentSession.id }}</el-descriptions-item>
          <el-descriptions-item label="总耗时">-</el-descriptions-item>
          <el-descriptions-item label="开始时间">
            {{ formatDateTime(currentSession.startAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">
            {{ formatDateTime(currentSession.endAt) || '尚未提交' }}
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="sessionSummaries[currentSession.id]" class="score-card">
          <div class="score-item main">
            <div class="label">总得分</div>
            <div class="value">{{ sessionSummaries[currentSession.id].total || 0 }}</div>
          </div>
          <div class="score-divider"></div>
          <div class="score-item">
            <div class="label">客观题</div>
            <div class="value sub">{{ sessionSummaries[currentSession.id].objective || 0 }}</div>
          </div>
          <div class="score-item">
            <div class="label">主观题</div>
            <div class="value sub">{{ sessionSummaries[currentSession.id].subjective || 0 }}</div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="viewAnswers(currentSession)">查看完整试卷</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Document, DataAnalysis } from '@element-plus/icons-vue'
import http from '../api/http'
import dayjs from 'dayjs' // 建议引入dayjs处理时间，如果没有请用原生的

const router = useRouter()
const loading = ref(false)
const sessions = ref([])
const papers = ref([])
const sessionSummaries = ref({})
const sessionProgress = ref({})
const detailDialogVisible = ref(false)
const currentSession = ref(null)

// 筛选与分页状态
const searchKeyword = ref('')
const filterStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

// 1. 计算属性：筛选后的数据
const filteredSessions = computed(() => {
  return sessions.value.filter(session => {
    const paperName = getPaperName(session.paperId)
    const matchName = !searchKeyword.value || paperName.includes(searchKeyword.value)
    const matchStatus = !filterStatus.value || session.status === filterStatus.value
    return matchName && matchStatus
  })
})

// 2. 计算属性：当前页展示的数据
const pagedSessions = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredSessions.value.slice(start, end)
})

// 3. 监听当前页数据变化，懒加载详情（核心优化）
watch(pagedSessions, (newRows) => {
  if (newRows.length > 0) {
    loadDetailsForRows(newRows)
  }
}, { immediate: true })

// 工具函数
const formatDateTime = (dt) => {
  if (!dt) return '-'
  return dayjs(dt).format('YYYY-MM-DD HH:mm:ss')
}

const getPaperName = (paperId) => {
  const paper = papers.value.find(p => p.id == paperId)
  return paper ? paper.name : `未知试卷 (#${paperId})`
}

const getStatusType = (status) => {
  const map = {
    'SUBMIT': 'success',
    'START': 'warning',
    'REVIEWED': 'primary', // 已阅卷用蓝色更合适
    'SCHEDULED': 'info'
  }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = {
    'START': '答题中',
    'SUBMIT': '待批改',
    'REVIEWED': '已出分',
    'SCHEDULED': '未开始'
  }
  return map[status] || status
}

// 仅仅获取指定行的详情数据，而不是全部
async function loadDetailsForRows(rows) {
  rows.forEach(row => {
    // 如果已经有了数据，就不重复请求
    if (!sessionSummaries.value[row.id] && !sessionProgress.value[row.id]) {
      loadSessionSingleData(row.id, row.paperId)
    }
  })
}

// 具体的单个请求逻辑
async function loadSessionSingleData(sessionId, paperId) {
  try {
    // 1. 获取得分 (只有已阅卷或已提交的才查分)
    // 这里并行发送请求
    const requests = []

    // 请求分数
    const summaryReq = http.get(`/exams/sessions/${sessionId}/summary`)
        .then(res => {
          if (res.data && res.data.data) {
            sessionSummaries.value[sessionId] = res.data.data
          }
        })
        .catch(() => {}) // 忽略404或错误
    requests.push(summaryReq)

    // 请求进度
    // 如果没有缓存该试卷的题目数量，需要先获取
    // 简化逻辑：这里假设必须实时获取
    const progressReq = Promise.all([
      http.get(`/exams/papers/${paperId}/preview`).catch(()=>({data:{data:[]}})),
      http.get(`/exams/sessions/${sessionId}/answers`).catch(()=>({data:{data:[]}}))
    ]).then(([paperRes, answersRes]) => {
      const total = paperRes?.data?.data?.length || 0
      const answered = answersRes?.data?.data?.length || 0
      sessionProgress.value[sessionId] = {
        total,
        answered,
        progress: total > 0 ? Math.round((answered / total) * 100) : 0
      }
    })
    requests.push(progressReq)

    await Promise.all(requests)

  } catch (e) {
    console.warn(`Details load failed for session ${sessionId}`)
  }
}

// 核心加载逻辑
async function loadData() {
  loading.value = true
  try {
    // 并行加载试卷和会话列表
    const [papersRes, sessionsRes] = await Promise.all([
      http.get('/papers'),
      http.get('/exams/sessions')
    ])

    papers.value = papersRes.data?.data || []
    sessions.value = (sessionsRes.data?.data || []).sort((a, b) => b.id - a.id)

  } catch (error) {
    ElMessage.error('数据加载失败')
  } finally {
    loading.value = false
  }
}

const handleRefresh = () => {
  // 清空缓存，强制重新加载
  sessionSummaries.value = {}
  sessionProgress.value = {}
  loadData()
}

const handleSizeChange = (val) => {
  pageSize.value = val
}
const handleCurrentChange = (val) => {
  currentPage.value = val
}

const viewAnswers = (row) => {
  router.push({ path: '/grading', query: { sessionId: row.id } })
}

const viewDetails = (row) => {
  currentSession.value = row
  detailDialogVisible.value = true
  // 打开详情时，确保数据是最新的
  loadSessionSingleData(row.id, row.paperId)
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.exam-record-container {
  padding: 24px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.page-header {
  margin-bottom: 24px;
}

.main-card {
  border-radius: 8px;
  overflow: visible; /* 允许下拉菜单溢出 */
}

/* 工具栏 */
.toolbar {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 16px;
}

.left-tools {
  display: flex;
  align-items: center;
}

/* 表格内容样式 */
.paper-title {
  font-weight: 600;
  color: #303133;
  font-size: inherit; /* 与表格主体字号保持一致 */
}

.time-info {
  font-size: 0.9em; /* 只比标题略小一点，避免大小差太多 */
  color: #909399;
  margin-top: 4px;
}

.score-display {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.total-score {
  font-size: 20px;
  font-weight: bold;
  color: #409EFF;
  line-height: 1.2;
}

.total-score .unit {
  font-size: 12px;
  font-weight: normal;
  margin-left: 2px;
}

.score-detail {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.loading-text {
  font-size: 12px;
  color: #c0c4cc;
}

.no-score {
  color: #909399;
  font-style: italic;
}

/* 进度条微调 */
.progress-wrapper {
  max-width: 140px;
}
.progress-text {
  font-size: 12px;
  color: #909399;
  display: block;
  margin-top: 4px;
}

/* 分页 */
.pagination-container {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}

/* 详情弹窗样式 */
.dialog-header-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.dialog-header-info h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.score-card {
  margin-top: 24px;
  background: #fdf6ec; /* 浅橙色背景，或改为 #ecf5ff 浅蓝 */
  background: linear-gradient(135deg, #ecf5ff 0%, #f0f9eb 100%);
  border-radius: 8px;
  padding: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.score-item {
  text-align: center;
  padding: 0 20px;
}

.score-item.main .value {
  font-size: 32px;
  color: #409EFF;
  font-weight: bold;
}

.score-item .label {
  font-size: 13px;
  color: #606266;
  margin-bottom: 4px;
}

.score-item .value.sub {
  font-size: 20px;
  color: #303133;
  font-weight: 600;
}

.score-divider {
  width: 1px;
  height: 40px;
  background-color: #dcdfe6;
}
</style>