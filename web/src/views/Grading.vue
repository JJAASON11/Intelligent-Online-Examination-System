<template>
  <div class="grading-layout">
    <el-container style="height: 100vh;">
      <el-header class="grading-header">
        <div class="header-left">
          <el-button icon="ArrowLeft" circle @click="$router.back()" style="margin-right: 12px" />
          <span class="page-title">智能阅卷中心</span>
        </div>
        <div class="header-right">
          <el-select v-model="paperId" placeholder="切换试卷" style="width: 240px" @change="handlePaperChange">
          <el-option v-for="p in papers" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
        </div>
      </el-header>

      <el-container style="overflow: hidden;">
        <el-aside width="300px" class="session-sidebar">
          <div class="sidebar-header">
            <el-input v-model="searchText" placeholder="搜索会话ID..." prefix-icon="Search" clearable />
            <div class="filter-row">
              <el-checkbox v-model="onlyPending" label="仅显示待评" @change="loadSessions" />
              <el-button link type="primary" icon="Refresh" @click="loadSessions">刷新</el-button>
            </div>
          </div>

          <el-scrollbar>
            <div v-loading="loadingSessions" class="session-list">
              <div
                  v-for="s in filteredSessions"
                  :key="s.id"
                  class="session-item"
                  :class="{ active: sessionId === s.id }"
                  @click="selectSession(s.id)"
              >
                <div class="session-info">
                  <span class="session-id">#{{ s.id }}</span>
                  <span class="session-time">{{ formatDate(s.endAt) }}</span>
                </div>
                <div class="session-status">
                  <el-tag v-if="s._pending > 0" type="danger" size="small" effect="dark">{{ s._pending }} 待评</el-tag>
                  <el-tag v-else type="success" size="small" effect="plain">已完成</el-tag>
                </div>
              </div>
              <el-empty v-if="filteredSessions.length === 0" description="暂无数据" image-size="60" />
            </div>
          </el-scrollbar>
        </el-aside>

        <el-main class="grading-main" v-loading="loadingAnswers">
          <div v-if="!sessionId" class="empty-state">
            <el-empty description="请从左侧选择一个考试会话开始阅卷" />
          </div>

          <div v-else class="grading-content">
            <el-card shadow="never" class="score-dashboard">
              <div class="dashboard-content">
                <div class="score-stat">
                  <span class="label">客观题得分</span>
                  <span class="value">{{ summary.objective }}</span>
                </div>
                <el-divider direction="vertical" />
                <div class="score-stat">
                  <span class="label">主观题得分</span>
                  <span class="value highlight">{{ summary.subjective }}</span>
                </div>
                <el-divider direction="vertical" />
                <div class="score-stat total">
                  <span class="label">总分</span>
                  <span class="value">{{ summary.total }}</span>
                </div>

                <div class="actions">
                  <el-radio-group v-model="viewFilter" size="small">
                    <el-radio-button label="all">全部</el-radio-button>
                    <el-radio-button label="pending">仅待评</el-radio-button>
                  </el-radio-group>
                  <el-button type="primary" @click="saveAllScores" :loading="saving">保存所有评分</el-button>
                  <el-button type="success" plain @click="markAsReviewed">完成阅卷</el-button>
                </div>
              </div>
            </el-card>

            <div class="questions-list">
              <div
                  v-for="(row, index) in displayAnswers"
                  :key="row.questionId"
                  class="question-card"
                  :class="{ 'is-auto': isAutoGraded(row) }"
              >
                <div class="q-header">
                  <div class="q-meta">
                    <span class="q-index">Q{{ index + 1 }}</span>
                    <el-tag :type="getTypeTag(meta(row.questionId)?.type)" effect="dark">
                      {{ meta(row.questionId)?.type }}
                    </el-tag>
                    <el-tag v-if="isAutoGraded(row)" :type="getAutoScoreTagType(row)" effect="dark" size="small">
                      <el-icon style="margin-right: 2px; vertical-align: middle;"><component :is="getAutoScoreIcon(row)" /></el-icon>
                      已自动判题
                    </el-tag>
                    <el-tag v-else type="warning" effect="plain" size="small">待手动评分</el-tag>
                    <span class="q-score-limit">满分: {{ meta(row.questionId)?.score }}</span>
                  </div>
                  <div class="q-score-input">
                    <span v-if="isAutoGraded(row)" class="auto-score">
                      <el-tag :type="getAutoScoreTagType(row)" size="large" effect="dark">
                        <el-icon style="margin-right: 4px;"><component :is="getAutoScoreIcon(row)" /></el-icon>
                        自动得分: <strong>{{ getAutoScore(row) }}</strong> / {{ meta(row.questionId)?.score }}
                      </el-tag>
                    </span>
                    <div v-else class="manual-score">
                      <span>得分:</span>
                      <el-input-number
                          v-model="row._score"
                          :min="0"
                          :max="meta(row.questionId)?.score"
                          controls-position="right"
                          size="small"
                          style="width: 100px"
                      />
                      <el-button-group class="quick-score">
                        <el-button size="small" @click="row._score=0">0</el-button>
                        <el-button size="small" @click="row._score=meta(row.questionId)?.score">满</el-button>
                      </el-button-group>
                      <el-button
                          type="primary"
                          link
                          icon="Check"
                          @click="saveSingleScore(row)"
                      >保存</el-button>
                    </div>
                  </div>
                </div>

                <div class="q-stem">{{ meta(row.questionId)?.stem }}</div>

                <div class="q-answer-box">
                  <div class="answer-header">
                    <span class="label">考生答案:</span>
                    <el-tag v-if="isAutoGraded(row)" :type="getAutoScoreTagType(row)" size="small" effect="dark" style="margin-left: 12px;">
                      {{ getAutoResultText(row) }}
                    </el-tag>
                  </div>
                  <div class="content">
                    <template v-if="meta(row.questionId)?.type === 'CODE'">
                      <pre class="code-block">{{ parseJson(row.answerJson)?.text || '未作答' }}</pre>
                    </template>
                    <template v-else-if="meta(row.questionId)?.type === 'SINGLE' || meta(row.questionId)?.type === 'MULTI'">
                      <div class="text-answer">{{ formatChoiceAnswer(row) }}</div>
            </template>
            <template v-else>
                      <div class="text-answer">{{ formatAnswerText(row) }}</div>
            </template>
                  </div>
                </div>
              </div>
            </div>
      </div>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, ArrowLeft, Check, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import http from '../api/http'
import dayjs from 'dayjs' // Recommend installing: npm install dayjs

// Data
const papers = ref([])
const paperId = ref(null)
const sessions = ref([])
const sessionId = ref(null)
const allAnswers = ref([])
const summary = ref({ objective: 0, subjective: 0, total: 0 })
const questionMap = ref({})
const loadingSessions = ref(false)
const loadingAnswers = ref(false)
const saving = ref(false)

// Filters
const searchText = ref('')
const onlyPending = ref(true)
const viewFilter = ref('all') // 'all' or 'pending' - 默认显示全部，包括自动判题的

// Computed
const filteredSessions = computed(() => {
  let list = sessions.value
  if (searchText.value) {
    list = list.filter(s => String(s.id).includes(searchText.value))
  }
  return list
})

const displayAnswers = computed(() => {
  if (viewFilter.value === 'pending') {
    return allAnswers.value.filter(a => a.score_auto == null) // Show only manual grading needed
  }
  return allAnswers.value
})

// Lifecycle
onMounted(async () => {
  await loadPapers()
})

// Methods
const loadPapers = async () => {
  const { data } = await http.get('/papers')
  papers.value = data?.data || []
  if (papers.value.length > 0) {
    paperId.value = papers.value[0].id
    handlePaperChange()
  }
}

const handlePaperChange = async () => {
  sessionId.value = null
  allAnswers.value = []
  await loadPaperMeta() // Pre-load question structure
  await loadSessions()
}

const loadPaperMeta = async () => {
  if (!paperId.value) return
  const { data } = await http.get(`/exams/papers/${paperId.value}/preview`)
  const list = data?.data || []
  const map = {}
  list.forEach(q => {
    map[q.id] = {
      type: q.type,
      stem: q.stem,
      score: q.score,
      optionsJson: q.optionsJson
    }
  })
  questionMap.value = map
}

const loadSessions = async () => {
  if (!paperId.value) return
  loadingSessions.value = true
  try {
    const { data } = await http.get('/exams/sessions', {
      params: { paperId: paperId.value, onlyPending: onlyPending.value }
    })
    const list = data?.data || []

    // Optimize: Parallel requests for pending counts
    const promises = list.map(s =>
        http.get(`/exams/sessions/${s.id}/pending-count`)
            .then(res => ({ ...s, _pending: Number(res?.data?.data || 0) }))
            .catch(() => ({ ...s, _pending: 0 }))
    )
    sessions.value = await Promise.all(promises)
  } finally {
    loadingSessions.value = false
  }
}

const selectSession = async (id) => {
  sessionId.value = id
  loadingAnswers.value = true
  try {
    const { data } = await http.get(`/exams/sessions/${id}/answers`)
    const rawAnswers = data?.data || []

    // Initialize manual score with current final score
    // 注意：后端可能返回 scoreAuto（驼峰）或 score_auto（下划线）
    allAnswers.value = rawAnswers.map(a => ({
      ...a,
      score_auto: a.score_auto ?? a.scoreAuto ?? null, // 兼容两种命名
      _score: a.score_final || a.scoreFinal || 0
    }))

    // 调试：打印答案数据，查看自动判题信息
    console.log('[Grading] 加载的答案数据:', allAnswers.value.map(a => ({
      questionId: a.questionId,
      score_auto: a.score_auto,
      scoreAuto: a.scoreAuto,
      isAutoGraded: a.score_auto != null || a.scoreAuto != null
    })))

    await loadSummary()
  } finally {
    loadingAnswers.value = false
  }
}

const loadSummary = async () => {
  if (!sessionId.value) return
  const { data } = await http.get(`/exams/sessions/${sessionId.value}/summary`)
  summary.value = data?.data || { objective: 0, subjective: 0, total: 0 }
}

const saveSingleScore = async (row) => {
  try {
    await http.put(`/exams/sessions/${sessionId.value}/answers/${row.questionId}/score`, null, {
      params: { score: row._score }
    })
    ElMessage.success('已保存')
    await loadSummary() // Refresh summary
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const saveAllScores = async () => {
  saving.value = true
  try {
    const pendingRows = displayAnswers.value.filter(row => !isAutoGraded(row))
    // Use Promise.all for parallel saving (or create a batch API)
    await Promise.all(pendingRows.map(row =>
        http.put(`/exams/sessions/${sessionId.value}/answers/${row.questionId}/score`, null, {
          params: { score: row._score }
        })
    ))
    ElMessage.success('全部保存成功')
    await loadSummary()

    // Refresh session list pending count
    const idx = sessions.value.findIndex(s => s.id === sessionId.value)
    if (idx !== -1) {
      const { data } = await http.get(`/exams/sessions/${sessionId.value}/pending-count`)
      sessions.value[idx]._pending = Number(data?.data || 0)
    }

  } catch (e) {
    ElMessage.error('部分保存失败，请检查网络')
  } finally {
    saving.value = false
  }
}

const markAsReviewed = async () => {
  try {
    await http.put(`/exams/sessions/${sessionId.value}/status`, null, {
      params: { status: 'REVIEWED' }
    })
    ElMessage.success('已完成阅卷')
    loadSessions() // Refresh sidebar
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

// Helpers
const meta = (qid) => questionMap.value[qid]
// 判断是否已自动判题：兼容两种字段命名（score_auto 和 scoreAuto）
const isAutoGraded = (row) => {
  const scoreAuto = row.score_auto ?? row.scoreAuto
  return scoreAuto != null && scoreAuto !== undefined
}
const parseJson = (str) => { try { return JSON.parse(str) } catch { return {} } }
const formatDate = (d) => d ? dayjs(d).format('MM-DD HH:mm') : '-'

const getTypeTag = (type) => {
  const map = { SINGLE: 'info', MULTI: 'success', CODE: 'danger', SHORT: 'warning' }
  return map[type] || ''
}

// 格式化选择题答案（单选/多选）
const formatChoiceAnswer = (row) => {
  const ans = parseJson(row.answerJson)
  const q = meta(row.questionId)
  if (!ans || !q) return '未作答'
  
  try {
    const options = q.optionsJson ? JSON.parse(q.optionsJson) : []
    const qType = q.type
    
    if (qType === 'SINGLE' && ans.index != null) {
      const idx = Number(ans.index)
      if (idx >= 0 && idx < options.length) {
        const optionText = options[idx].replace(/^[A-Z]\.\s*/, '') // 移除选项前缀
        return `${String.fromCharCode(65 + idx)}. ${optionText}`
      }
      return `选项 ${String.fromCharCode(65 + idx)}`
    } else if (qType === 'MULTI' && Array.isArray(ans.indexes)) {
      return ans.indexes.map((idx) => {
        const i = Number(idx)
        if (i >= 0 && i < options.length) {
          const optionText = options[i].replace(/^[A-Z]\.\s*/, '')
          return `${String.fromCharCode(65 + i)}. ${optionText}`
        }
        return String.fromCharCode(65 + i)
      }).join('；')
    }
  } catch (e) {
    console.error('解析选项失败:', e)
  }
  
  // 降级显示
  if (ans.index != null) return `选项 ${String.fromCharCode(65 + Number(ans.index))}`
  if (Array.isArray(ans.indexes)) return ans.indexes.map(i => String.fromCharCode(65 + Number(i))).join('、')
  return '未作答'
}

// 格式化其他类型答案
const formatAnswerText = (row) => {
  const ans = parseJson(row.answerJson)
  const q = meta(row.questionId)
  if (!ans) return '未作答'
  
  const qType = q?.type
  
  if (qType === 'JUDGE') {
    const value = ans.value === true || ans.value === 'true' || ans.value === 1
    return value ? '✓ 正确' : '✗ 错误'
  } else if (qType === 'FILL') {
    if (Array.isArray(ans.values)) {
      return ans.values.filter(v => v && v.trim()).join('；') || '未作答'
    }
    return ans.values || '未作答'
  } else if (qType === 'SHORT' || qType === 'CODE') {
    return ans.text || '未作答'
  }
  
  return JSON.stringify(ans)
}

// 获取自动判题得分（兼容两种字段命名）
const getAutoScore = (row) => {
  return row.score_auto ?? row.scoreAuto ?? 0
}

// 获取自动判题结果的标签类型
const getAutoScoreTagType = (row) => {
  const score = getAutoScore(row)
  const maxScore = meta(row.questionId)?.score || 0
  if (score >= maxScore) return 'success'
  if (score > 0) return 'warning'
  return 'danger'
}

// 获取自动判题结果的图标
const getAutoScoreIcon = (row) => {
  const score = getAutoScore(row)
  const maxScore = meta(row.questionId)?.score || 0
  if (score >= maxScore) return CircleCheck
  return CircleClose
}

// 获取自动判题结果文本
const getAutoResultText = (row) => {
  const score = getAutoScore(row)
  const maxScore = meta(row.questionId)?.score || 0
  if (score >= maxScore) return '✓ 正确'
  if (score > 0) return '部分正确'
  return '✗ 错误'
}
</script>

<style scoped>
.grading-layout {
  height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}

.grading-header {
  background: #fff;
  border-bottom: 1px solid #dcdfe6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 60px;
}
.header-left { display: flex; align-items: center; }
.page-title { font-size: 18px; font-weight: 600; color: #303133; }

/* Sidebar */
.session-sidebar {
  background: #fff;
  border-right: 1px solid #dcdfe6;
  display: flex;
  flex-direction: column;
}
.sidebar-header { padding: 16px; border-bottom: 1px solid #ebeef5; }
.filter-row { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; }
.session-list { padding: 10px; }
.session-item {
  padding: 12px;
  margin-bottom: 8px;
  border-radius: 6px;
  background: #f9fafc;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.2s;
}
.session-item:hover { background: #ecf5ff; }
.session-item.active { background: #ecf5ff; border-color: #409eff; }
.session-info { display: flex; justify-content: space-between; font-size: 14px; margin-bottom: 6px; }
.session-id { font-weight: bold; color: #303133; }
.session-time { font-size: 12px; color: #909399; }
.session-status { display: flex; justify-content: flex-end; }

/* Main Area */
.grading-main { padding: 20px; overflow-y: auto; background: #f0f2f5; }
.score-dashboard {
  position: sticky;
  top: 0;
  z-index: 10;
  margin-bottom: 20px;
  border-radius: 8px;
}
.dashboard-content { display: flex; align-items: center; justify-content: space-between; }
.score-stat { text-align: center; padding: 0 20px; }
.score-stat .label { display: block; font-size: 12px; color: #909399; margin-bottom: 4px; }
.score-stat .value { font-size: 20px; font-weight: bold; color: #303133; }
.score-stat .value.highlight { color: #e6a23c; }
.actions { display: flex; gap: 12px; align-items: center; }

/* Question Cards */
.question-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
  border-left: 4px solid #409eff;
}
.question-card.is-auto { 
  border-left-color: #67c23a; 
  background: linear-gradient(to right, #f0f9ff 0%, #ffffff 10%);
}
.question-card.is-auto .q-header { background: rgba(103, 194, 58, 0.05); }

.q-header { display: flex; justify-content: space-between; margin-bottom: 16px; border-bottom: 1px dashed #ebeef5; padding-bottom: 12px; }
.q-meta { display: flex; gap: 10px; align-items: center; }
.q-index { font-weight: bold; font-size: 16px; }
.q-score-limit { color: #909399; font-size: 12px; }

.q-score-input { display: flex; align-items: center; }
.auto-score { display: flex; align-items: center; }
.auto-score .el-tag { 
  font-size: 16px; 
  font-weight: 600;
  padding: 8px 16px;
}
.manual-score { display: flex; gap: 8px; align-items: center; }
.quick-score { margin: 0 8px; }

.q-stem { font-size: 15px; line-height: 1.6; margin-bottom: 20px; color: #303133; }

.q-answer-box { background: #f8faff; border-radius: 6px; padding: 16px; border: 1px solid #eef1f6; }
.q-answer-box .answer-header { display: flex; align-items: center; margin-bottom: 8px; }
.q-answer-box .label { font-size: 12px; color: #909399; font-weight: 500; }
.code-block {
  background: #282c34;
  color: #abb2bf;
  padding: 12px;
  border-radius: 4px;
  font-family: 'Fira Code', monospace;
  overflow-x: auto;
  margin: 0;
}
.text-answer { font-size: 15px; color: #303133; }
</style>