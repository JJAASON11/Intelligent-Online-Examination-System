<template>
  <div style="padding:24px;">
    <el-page-header content="考试与监考" @back="$router.back()" />
    <el-card style="margin-top:16px; max-width:720px;">
      <el-form label-width="100px">
        <el-form-item label="选择试卷">
          <el-select v-model="paperId" placeholder="请选择试卷" style="width:360px;" @change="onPaperChange">
            <el-option v-for="p in papers" :key="p.id" :label="p.name + '（' + (p.totalScore||0) + '分，'+ (p.durationMin||0) +'分钟）'" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="考试日期">
          <el-date-picker v-model="examDate" type="date" placeholder="请选择日期"
                          value-format="YYYY-MM-DD" :disabled-date="disabledDate" />
        </el-form-item>
        <el-form-item label="时间段">
          <el-time-picker v-model="timeRange" is-range range-separator="至"
                          start-placeholder="开始时间" end-placeholder="结束时间"
                          value-format="HH:mm:ss" />
        </el-form-item>
        <el-form-item label="监考会话ID">
          <el-input v-model="proctorSid" placeholder="可手动填写会话ID" style="width:360px;" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="creating" @click="createSession">创建会话</el-button>
          <el-button :disabled="!sessionId" @click="copySid">复制会话ID</el-button>
          <el-button type="warning" :disabled="!(sessionId||proctorSid)" @click="openProctor">打开监考</el-button>
          <el-button @click="goRecords">我的考试记录</el-button>
        </el-form-item>
        <div v-if="sessionId">会话：{{ sessionId }}</div>
      </el-form>
    </el-card>

    <el-card style="margin-top:16px; max-width:960px;">
      <template #header>已创建的考试会话</template>
      <div style="margin-bottom:12px; display:flex; gap:8px; align-items:center;">
        <el-date-picker v-model="listDay" type="date" value-format="YYYY-MM-DD" />
        <el-button size="small" @click="loadSessions">刷新</el-button>
      </div>
      <el-table :data="sessions">
        <el-table-column prop="id" label="会话" width="160" />
        <el-table-column prop="paperId" label="试卷ID" width="100" />
        <el-table-column prop="startAt" label="开始" width="180" />
        <el-table-column prop="endAt" label="结束" width="180" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button size="small" type="warning" @click="openProctorWith(row)">监考</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import router from '../router'
import { ElMessage } from 'element-plus'
import http from '../api/http'
const paperId = ref(null)
const examDate = ref('')
const timeRange = ref([])
const creating = ref(false)
const sessionId = ref(null)
const papers = ref([])
const durationMin = ref(60)
const proctorSid = ref('')
const sessions = ref([])
const listDay = ref(new Date().toISOString().slice(0,10))
function disabledDate(d){
  const now = Date.now()
  const t = new Date(d).getTime()
  // 只允许从今天起未来30天内选择
  return t < now - 24*3600*1000 || t > now + 30*24*3600*1000
}
async function loadPapers(){
  const { data } = await http.get('/papers')
  papers.value = data?.data || []
}
function onPaperChange(id){
  const p = papers.value.find(x=>x.id===id)
  durationMin.value = p?.durationMin || 60
}
async function createSession(){
  if (!examDate.value) { ElMessage.warning('请选择考试日期'); return }
  if (!timeRange.value || timeRange.value.length < 2) { ElMessage.warning('请选择考试时间段'); return }
  if (!paperId.value) { ElMessage.warning('请选择试卷'); return }
  const [startT, endT] = timeRange.value
  const startAt = `${examDate.value} ${startT}`
  const endAt = `${examDate.value} ${endT}`
  // 校验：窗口不得超过1天，且结束必须晚于开始
  const diff = new Date(endAt).getTime() - new Date(startAt).getTime()
  if (diff <= 0) { ElMessage.error('结束时间必须晚于开始时间'); return }
  if (diff > 24*3600*1000) { ElMessage.error('考试窗口不能超过1天'); return }
  creating.value = true
  try{
    const { data } = await http.post('/exams/sessions', { paperId: paperId.value, startAt, endAt })
    sessionId.value = data?.data?.sessionId || null
    proctorSid.value = sessionId.value
    ElMessage.success('已创建考试会话')
  } catch(e){ ElMessage.error('创建失败') } finally { creating.value = false }
}

function startExam(){ /* 教师不进入考试，保留方法供需要时手动调用 */ }

function openProctor(){
  const sid = proctorSid.value || sessionId.value
  if (!sid) { ElMessage.warning('请填写会话ID或先创建会话'); return }
  router.push(`/proctor?sessionId=${sid}`)
}
function goRecords(){ location.href = '/records' }
async function copySid(){
  try{ await navigator.clipboard.writeText(String(sessionId.value||'')); ElMessage.success('已复制会话ID') }
  catch{ ElMessage.error('复制失败') }
}
function openProctorWith(row){ router.push(`/proctor?sessionId=${row.id}`) }
async function loadSessions(){
  try{
    const { data } = await http.get('/exams/sessions')
    let arr = data?.data || []
    if (paperId.value) arr = arr.filter(x=> x.paperId===paperId.value)
    if (listDay.value) {
      arr = arr.filter(x=> String(x.startAt||'').slice(0,10)===listDay.value || String(x.endAt||'').slice(0,10)===listDay.value)
    }
    sessions.value = arr
  } catch(e){ sessions.value = [] }
}

onMounted(()=>{ loadPapers(); loadSessions() })
</script>
