<template>
  <div class="exam-container">
    <el-header class="exam-header" height="60px">
      <div class="header-left">
        <span class="exam-title">在线考试中</span>
        <el-tag v-if="notice" type="warning" effect="dark" class="notice-tag">
          <el-icon><Bell /></el-icon> {{ notice }}
        </el-tag>
      </div>

      <div class="header-center">
        <div class="timer-box" :class="{ 'urgent': remainingSeconds < 300 }">
          <el-icon><Timer /></el-icon>
          <span class="time-text">{{ formatTime(remainingSeconds) }}</span>
        </div>
      </div>

      <div class="header-right">
        <el-button type="primary" size="large" @click="handleSubmitCheck">
          交 卷
        </el-button>
      </div>
    </el-header>

    <el-container class="exam-body">
      <el-aside width="280px" class="exam-sidebar">
        <div class="sidebar-content">
          <div class="sidebar-title">答题卡</div>
          <div class="question-grid">
            <div
                v-for="(q, idx) in questions"
                :key="q.id"
                class="q-indicator"
                :class="{
                'answered': isAnswered(q.id),
                'marked': marks[q.id],
                'active': currentAnchor === idx
              }"
                @click="scrollToQuestion(idx)"
            >
              {{ idx + 1 }}
              <div v-if="marks[q.id]" class="mark-dot"></div>
            </div>
          </div>
          <div class="sidebar-legend">
            <span><span class="dot answered"></span> 已答</span>
            <span><span class="dot marked"></span> 标记</span>
            <span><span class="dot"></span> 未答</span>
          </div>
        </div>
      </el-aside>

      <el-main class="exam-main" ref="mainScrollRef" @scroll="handleScroll">
        <el-empty v-if="questions.length === 0" description="试卷加载中或无题目..." />

        <div
            v-for="(q, idx) in questions"
            :key="q.id"
            :id="`question-${idx}`"
            class="question-card"
        >
          <div class="q-header">
            <span class="q-seq">{{ idx + 1 }}</span>
            <el-tag size="small" effect="plain">{{ getTypeText(q.type) }}</el-tag>
            <div class="q-tools">
              <el-checkbox v-model="marks[q.id]" label="标记待查" size="small" />
            </div>
          </div>

          <div class="q-stem">{{ q.stem }}</div>

          <div class="q-body">
            <div v-if="q.type === 'SINGLE'">
              <el-radio-group v-model="answers[q.id]" @change="autoSave(q)">
                <el-radio
                    v-for="(opt, i) in parseJson(q.optionsJson)"
                    :key="i"
                    :label="i"
                    class="block-radio"
                >
                  <span class="opt-label">{{ toLetter(i) }}.</span> {{ opt }}
                </el-radio>
              </el-radio-group>
            </div>

            <div v-else-if="q.type === 'MULTI'">
              <el-checkbox-group v-model="answers[q.id]" @change="autoSave(q)">
                <el-checkbox
                    v-for="(opt, i) in parseJson(q.optionsJson)"
                    :key="i"
                    :label="i"
                    class="block-checkbox"
                >
                  <span class="opt-label">{{ toLetter(i) }}.</span> {{ opt }}
                </el-checkbox>
              </el-checkbox-group>
            </div>

            <div v-else-if="q.type === 'JUDGE'">
              <el-radio-group v-model="answers[q.id]" @change="autoSave(q)">
                <el-radio label="TRUE" border>正确</el-radio>
                <el-radio label="FALSE" border>错误</el-radio>
              </el-radio-group>
            </div>

            <div v-else-if="q.type === 'FILL'">
              <div v-for="(item, i) in (parseJson(q.answerJson) || [])" :key="i" class="fill-row">
                <span class="fill-index">填空 {{ i + 1 }}：</span>
                <el-input
                    v-model="fillInputs[q.id][i]"
                    @input="handleFillInput(q)"
                    placeholder="请输入答案"
                />
              </div>
            </div>

            <div v-else-if="q.type === 'SHORT'">
              <el-input
                  type="textarea"
                  v-model="answers[q.id]"
                  :rows="4"
                  placeholder="请输入您的回答..."
                  @input="autoSave(q)"
              />
            </div>

            <div v-else-if="q.type === 'CODE'">
              <el-input
                  type="textarea"
                  v-model="answers[q.id]"
                  :autosize="{ minRows: 10, maxRows: 30 }"
                  placeholder="// 在此编写代码..."
                  class="code-editor"
                  @input="autoSave(q)"
              />
            </div>
          </div>

          <div class="q-footer">
            <span class="save-status">
              <el-icon v-if="saveStatus[q.id] === 'saving'" class="is-loading"><Loading /></el-icon>
              <el-icon v-else-if="saveStatus[q.id] === 'saved'" color="#67C23A"><Select /></el-icon>
              {{ getSaveStatusText(saveStatus[q.id]) }}
            </span>
          </div>
        </div>

        <div class="bottom-action">
          <el-divider>到底了</el-divider>
          <el-button type="primary" size="large" style="width: 200px" @click="handleSubmitCheck">提交试卷</el-button>
        </div>

      </el-main>
    </el-container>

    <el-dialog v-model="dialogs.lock" title="⚠️ 考试警告" :show-close="false" :close-on-press-escape="false" :close-on-click-modal="false" width="400px" center>
      <div style="text-align: center; padding: 20px 0;">
        <el-icon size="40" color="#E6A23C"><Warning /></el-icon>
        <p style="margin-top: 15px;">检测到您切换了屏幕或退出了全屏。</p>
        <p style="color: #F56C6C; font-size: 12px;">多次触发将被记录为异常行为。</p>
      </div>
      <template #footer>
        <el-button type="primary" @click="unlockExam">我已回到考试页面</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dialogs.kicked" title="🛑 考试终止" :show-close="false" :close-on-press-escape="false" :close-on-click-modal="false" width="400px" center>
      <div style="text-align: center;">
        <p>监考老师已终止您的考试资格。</p>
      </div>
      <template #footer>
        <el-button type="danger" @click="router.push('/')">返回首页</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Timer, Bell, Loading, Select, Warning } from '@element-plus/icons-vue'
import http from '../api/http'
import _ from 'lodash' // 建议引入 lodash 用于 debounce，如果没有请手动实现

const router = useRouter()
// 路由参数
const query = new URLSearchParams(location.search)
const examInfo = {
  sessionId: Number(query.get('sessionId')) || Date.now(),
  paperId: Number(query.get('paperId')) || 0,
  duration: Number(query.get('duration')) || 60,
  studentId: Number(localStorage.getItem('userId') || 0)
}

// 核心数据
const questions = ref([])
const answers = ref({})
const fillInputs = ref({}) // 填空题特殊处理
const marks = ref({}) // 标记题目
const saveStatus = ref({}) // saving, saved, error
const remainingSeconds = ref(examInfo.duration * 60)
const violationCount = ref(0) // 切屏/退出全屏等违规次数
const notice = ref('')
const currentAnchor = ref(0) // 当前滚动到的题目索引

// 弹窗状态
const dialogs = reactive({
  lock: false,
  kicked: false
})
const submitted = ref(false)

// 定时器引用
let timerInterval = null
let ws = null
let antiCheatTimer = null

// --- 初始化与加载 ---
onMounted(async () => {
  await initExam()
  startTimer()
  initWebSocket()
  initAntiCheat()
  enterFullscreen()
})

onBeforeUnmount(() => {
  cleanup()
})

const initExam = async () => {
  try {
    // 1. 告知后端开始考试
    await http.post('/exams/start', {
      sessionId: examInfo.sessionId,
      paperId: examInfo.paperId,
      studentId: examInfo.studentId
    }).catch(() => {})

    // 2. 加载试卷
    const { data } = await http.get(`/exams/papers/${examInfo.paperId}/preview`)
    questions.value = data?.data || []

    // 3. 初始化填空题模型
    questions.value.forEach(q => {
      if (q.type === 'FILL') {
        const arr = parseJson(q.answerJson) || []
        // 初始化数组，防止 undefined
        if (!fillInputs.value[q.id]) {
          fillInputs.value[q.id] = Array(arr.length).fill('')
        }
      }
      // 初始化状态
      saveStatus.value[q.id] = ''
    })
  } catch (error) {
    ElMessage.error('试卷加载失败，请联系管理员')
  }
}

// --- 业务逻辑：答题与保存 ---

// 防抖自动保存 (500ms)
const autoSave = _.debounce((q) => {
  saveAnswer(q, false)
}, 1000)

const handleFillInput = (q) => {
  // 填空题输入也触发防抖保存
  autoSave(q)
}

const saveAnswer = async (q, manual = false) => {
  if (submitted.value) return

  // 1. 构建答案格式
  let answerContent
  if (q.type === 'FILL') {
    answerContent = { values: fillInputs.value[q.id] || [] }
  } else if (q.type === 'SINGLE') {
    answerContent = { index: answers.value[q.id] }
  } else if (q.type === 'MULTI') {
    answerContent = { indexes: answers.value[q.id] || [] }
  } else if (q.type === 'JUDGE') {
    answerContent = { value: answers.value[q.id] === 'TRUE' }
  } else {
    // 简答、代码
    answerContent = { text: answers.value[q.id] || '' }
  }

  // 2. 发送请求
  try {
    saveStatus.value[q.id] = 'saving'
    await http.post('/exams/answers', {
      sessionId: examInfo.sessionId,
      studentId: examInfo.studentId,
      questionId: q.id,
      answerJson: JSON.stringify(answerContent)
    })
    saveStatus.value[q.id] = 'saved'
    if (manual) ElMessage.success('保存成功')
  } catch (e) {
    saveStatus.value[q.id] = 'error'
    if (manual) ElMessage.error('保存失败，请检查网络')
  }
}

const handleSubmitCheck = () => {
  // 检查是否有未答题目
  const total = questions.value.length
  const answeredCount = questions.value.filter(q => isAnswered(q.id)).length

  if (answeredCount < total) {
    ElMessageBox.confirm(
        `您还有 ${total - answeredCount} 道题未作答，确定要交卷吗？`,
        '交卷确认',
        { confirmButtonText: '确定交卷', cancelButtonText: '继续答题', type: 'warning' }
    ).then(() => submitExam())
  } else {
    ElMessageBox.confirm('确认提交试卷吗？提交后无法修改。', '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消'
    }).then(() => submitExam())
  }
}

const submitExam = async () => {
  try {
    // 提交前最后一次保存所有（可选，防止网络延迟导致的自动保存未完成）
    // 这里简化为直接提交
    await http.post('/exams/submit', {
      sessionId: examInfo.sessionId,
      paperId: examInfo.paperId,
      studentId: examInfo.studentId
    })
    submitted.value = true
    cleanup() // 清理定时器和事件
    router.replace(`/exam/success?sessionId=${examInfo.sessionId}`)
  } catch (error) {
    ElMessage.error('提交失败，请重试')
  }
}

// --- 辅助功能 ---

// 检查某题是否已答
const isAnswered = (qid) => {
  const type = questions.value.find(q => q.id === qid)?.type
  if (type === 'FILL') {
    // 填空题只要填了一个空就算答了
    return fillInputs.value[qid]?.some(val => val && val.trim() !== '')
  }
  const val = answers.value[qid]
  if (Array.isArray(val)) return val.length > 0
  return val !== undefined && val !== null && val !== ''
}

const scrollToQuestion = (index) => {
  const el = document.getElementById(`question-${index}`)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
}

// 监听滚动更新侧边栏高亮
const handleScroll = (e) => {
  // 简单的节流处理滚动监听
  const scrollTop = e.target.scrollTop
  // 粗略计算当前在第几个card
  // 实际项目中可以使用 IntersectionObserver 更精确
  // 这里简化处理
}

const formatTime = (seconds) => {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
}

const getTypeText = (type) => {
  const map = { SINGLE: '单选题', MULTI: '多选题', JUDGE: '判断题', FILL: '填空题', SHORT: '简答题', CODE: '编程题' }
  return map[type] || type
}
const getSaveStatusText = (status) => {
  if (status === 'saving') return '保存中...'
  if (status === 'saved') return '已保存'
  if (status === 'error') return '保存失败'
  return ''
}
const toLetter = (i) => String.fromCharCode(65 + i)
const parseJson = (str) => { try { return JSON.parse(str) } catch { return [] } }

// --- 计时器与反作弊 ---

const startTimer = () => {
  timerInterval = setInterval(() => {
    if (remainingSeconds.value > 0) {
      remainingSeconds.value--
    } else {
      submitExam() // 时间到自动交卷
    }
  }, 1000)
}

const initWebSocket = () => {
  try {
    const protocol = location.protocol === 'https:' ? 'wss://' : 'ws://'
    ws = new WebSocket(`${protocol}${location.host}/ws/proctor`)
    ws.onopen = () => ws.send(JSON.stringify({ type: 'join', sessionId: String(examInfo.sessionId) }))
    ws.onmessage = (e) => {
      const msg = String(e.data)
      if (msg.includes('warn:')) {
        notice.value = msg.split('warn:')[1]
        ElMessage.warning(`监考提醒: ${notice.value}`)
      } else if (msg.includes('kick')) {
        dialogs.kicked = true
        submitted.value = true // 防止继续操作
        cleanup()
      }
    }
    // 增加心跳保活或重连逻辑（此处略）
  } catch (e) {
    console.error('WS Error', e)
  }
}

// 反作弊核心
const initAntiCheat = () => {
  document.addEventListener('visibilitychange', handleVisibilityChange)
  document.addEventListener('fullscreenchange', handleFullscreenChange)
  window.addEventListener('blur', handleBlur)
}

const reportEvent = (type, detail) => {
  if (submitted.value) return
  // 发送给后端记录
  http.post('/exams/events', {
    sessionId: examInfo.sessionId,
    studentId: examInfo.studentId,
    type,
    detail
  }).catch(() => {})
}

const handleVisibilityChange = () => {
  if (document.hidden) {
    reportEvent('switch_tab', 'hidden')
    dialogs.lock = true
    recordViolation('switch_tab')
  }
}
const handleBlur = () => {
  if (!document.hasFocus()) {
    // 某些时候 blur 比 visibilityChange 更灵敏
    reportEvent('blur', 'lost_focus')
    recordViolation('blur')
  }
}
const handleFullscreenChange = () => {
  if (!document.fullscreenElement) {
    reportEvent('fullscreen', 'exit')
    dialogs.lock = true
    recordViolation('exit_fullscreen')
  }
}

const enterFullscreen = () => {
  document.documentElement.requestFullscreen().catch(err => {
    ElMessage.warning('为了最佳体验，请允许全屏模式')
  })
}

const unlockExam = () => {
  dialogs.lock = false
  enterFullscreen()
  reportEvent('unlock', 'user_confirmed')
}

// 违规计数，超过3次强制交卷
const recordViolation = (reason) => {
  if (submitted.value) return
  violationCount.value += 1
  if (violationCount.value >= 3) {
    ElMessage.warning('切屏次数过多，系统已自动交卷')
    submitExam()
  } else {
    // 可选：提醒剩余容忍次数
    const remain = 3 - violationCount.value
    if (remain > 0) {
      ElMessage.warning(`请专注考试，再切屏 ${remain} 次将自动交卷（原因：${reason}）`)
    }
  }
}

const cleanup = () => {
  clearInterval(timerInterval)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  window.removeEventListener('blur', handleBlur)
  if (ws) ws.close()
}

</script>

<style scoped>
.exam-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}

/* 顶部样式 */
.exam-header {
  background: #fff;
  border-bottom: 1px solid #dcdfe6;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.exam-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.timer-box {
  background: #f0f9eb;
  color: #67c23a;
  padding: 8px 24px;
  border-radius: 20px;
  font-size: 20px;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s;
}
.timer-box.urgent {
  background: #fef0f0;
  color: #f56c6c;
  animation: pulse 1s infinite;
}
@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.05); }
  100% { transform: scale(1); }
}

/* 主体布局 */
.exam-body {
  height: calc(100vh - 60px);
  overflow: hidden;
}

/* 侧边栏 */
.exam-sidebar {
  background: #fff;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}
.sidebar-content {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
}
.sidebar-title {
  font-weight: 600;
  margin-bottom: 16px;
  padding-left: 8px;
  border-left: 4px solid #409eff;
}

.question-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
  margin-bottom: 20px;
}
.q-indicator {
  width: 36px;
  height: 36px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 14px;
  position: relative;
  transition: all 0.2s;
}
.q-indicator:hover {
  border-color: #409eff;
  color: #409eff;
}
.q-indicator.active {
  border-color: #409eff;
  background: #ecf5ff;
}
.q-indicator.answered {
  background: #f0f9eb;
  border-color: #67c23a;
  color: #67c23a;
}
.q-indicator.marked {
  border-color: #e6a23c;
}
.mark-dot {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 6px;
  height: 6px;
  background: #e6a23c;
  border-radius: 50%;
}

.sidebar-legend {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #606266;
  margin-top: auto;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}
.dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 50%;
  margin-right: 4px;
}
.dot.answered { background: #67c23a; border-color: #67c23a; }
.dot.marked { background: #e6a23c; border-color: #e6a23c; }

/* 题目主区域 */
.exam-main {
  padding: 30px 15%; /* 增加左右留白，聚焦内容 */
  scroll-behavior: smooth;
}
.question-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05);
}
.q-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.q-seq {
  font-size: 20px;
  font-weight: 800;
  color: #303133;
}
.q-tools {
  margin-left: auto;
}
.q-stem {
  font-size: 16px;
  line-height: 1.6;
  color: #303133;
  margin-bottom: 20px;
  white-space: pre-wrap;
}

/* 选项样式 */
.block-radio, .block-checkbox {
  display: flex;
  margin-right: 0;
  margin-bottom: 12px;
  padding: 10px 16px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  transition: all 0.2s;
  white-space: normal;
  height: auto;
}
.block-radio:hover, .block-checkbox:hover {
  background: #f5f7fa;
}
.is-checked {
  border-color: #409eff;
  background: #ecf5ff;
}
.opt-label {
  font-weight: bold;
  margin-right: 8px;
}

/* 填空题样式 */
.fill-row {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}
.fill-index {
  width: 70px;
  font-weight: bold;
  color: #606266;
}

/* 编程题编辑器模拟 */
.code-editor :deep(textarea) {
  background-color: #282c34;
  color: #abb2bf;
  font-family: 'Fira Code', 'Consolas', monospace;
  line-height: 1.5;
  padding: 15px;
}

.q-footer {
  margin-top: 20px;
  padding-top: 12px;
  border-top: 1px dashed #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.save-status {
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 4px;
}
.bottom-action {
  text-align: center;
  margin: 40px 0;
}
</style>