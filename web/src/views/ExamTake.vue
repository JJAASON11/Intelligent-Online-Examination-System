<template>
  <div style="padding:24px;">
    <el-page-header content="考试" @back="$router.back()" />
    <el-alert v-if="notice" :title="notice" type="warning" show-icon style="margin-top:12px;" />
    <div style="margin-top:12px;">倒计时：{{ remain }} 分钟</div>
    <el-empty v-if="items.length===0" description="试卷暂无题目或加载失败" />
    <el-card v-for="(q, idx) in items" :key="q.id" style="margin-top:12px;">
      <div style="font-weight:600;">第{{ idx+1 }}题（{{ q.type }}）</div>
      <div style="margin-top:8px;">{{ q.stem }}</div>
      <div v-if="q.type==='SINGLE'">
        <el-radio-group v-model="answers[q.id]">
          <el-radio v-for="(opt,i) in parse(q.optionsJson)" :key="i" :label="i">{{ String.fromCharCode(65+i) }}. {{ opt }}</el-radio>
        </el-radio-group>
      </div>
      <div v-else-if="q.type==='MULTI'">
        <el-checkbox-group v-model="answers[q.id]">
          <el-checkbox v-for="(opt,i) in parse(q.optionsJson)" :key="i" :label="i">{{ String.fromCharCode(65+i) }}. {{ opt }}</el-checkbox>
        </el-checkbox-group>
      </div>
      <div v-else-if="q.type==='JUDGE'">
        <el-radio-group v-model="answers[q.id]">
          <el-radio label="TRUE">正确</el-radio>
          <el-radio label="FALSE">错误</el-radio>
        </el-radio-group>
      </div>
      <div v-else-if="q.type==='FILL'">
        <div v-for="(ans,i) in (parse(q.answerJson)||[])" :key="i" style="margin-bottom:8px;">
          <el-input v-model="fillInputs[q.id][i]" />
        </div>
      </div>
      <div v-else-if="q.type==='SHORT'">
        <el-input type="textarea" v-model="answers[q.id]" />
      </div>
      <div v-else-if="q.type==='CODE'">
        <el-input
          type="textarea"
          v-model="answers[q.id]"
          :autosize="{ minRows: 8, maxRows: 20 }"
          placeholder="在此编写代码..."
          style="font-family: Menlo, Consolas, 'Courier New', monospace;"
        />
      </div>
      <div style="margin-top:8px;">
        <el-button size="small" @click="save(q)">保存本题</el-button>
      </div>
    </el-card>
    <div style="margin-top:12px;">
      <el-button type="primary" @click="submit">提交试卷</el-button>
    </div>
    <el-dialog v-model="openLock" title="考试提醒" :close-on-click-modal="false" :close-on-press-escape="false" :show-close="false">
      <div>检测到切换屏幕或退出全屏，未交卷前请保持在考试页面。</div>
      <template #footer>
        <el-button type="primary" @click="unlock">我已返回</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="kicked" title="考试已终止" :close-on-click-modal="false" :close-on-press-escape="false" :show-close="false">
      <div>监考已终止本次考试，请联系监考老师。</div>
      <template #footer>
        <el-button type="primary" @click="goHome">返回首页</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import router from '../router'
import http from '../api/http'
import { ElMessage } from 'element-plus'
const sessionId = ref(Number(new URLSearchParams(location.search).get('sessionId'))||Date.now())
const paperId = ref(Number(new URLSearchParams(location.search).get('paperId'))||0)
const studentId = ref(Number(localStorage.getItem('userId')||0))
const remain = ref(Number(new URLSearchParams(location.search).get('duration'))||60)
let timer
let endTime = 0
const items = ref([])
const answers = ref({})
const fillInputs = ref({})
const submitted = ref(false)
const openLock = ref(false)
const kicked = ref(false)
const notice = ref('')
let ws
function parse(s){ try{ return s? JSON.parse(s):null }catch(e){ return null } }
async function load(){
  try{
    await http.post('/exams/start', { sessionId: sessionId.value, paperId: paperId.value, studentId: studentId.value })
  } catch(e){ /* 启动失败也继续加载试卷，避免页面空白 */ }
  const { data } = await http.get(`/exams/papers/${paperId.value}/preview`)
  items.value = data?.data || []
  for (const q of items.value) {
    if (q.type === 'FILL') {
      const arr = parse(q.answerJson) || []
      fillInputs.value[q.id] = Array(arr.length).fill('')
    }
  }
}
async function save(q){
  let shape
  if (q.type === 'FILL') shape = { values: (fillInputs.value[q.id]||[]) }
  else if (q.type === 'SINGLE') shape = { index: answers.value[q.id] }
  else if (q.type === 'MULTI') shape = { indexes: (answers.value[q.id]||[]) }
  else if (q.type === 'JUDGE') shape = { value: answers.value[q.id] === 'TRUE' }
  else if (q.type === 'CODE') shape = { text: answers.value[q.id] }
  else shape = { text: answers.value[q.id] }
  const payload = { sessionId: sessionId.value, studentId: studentId.value, questionId: q.id, answerJson: JSON.stringify(shape) }
  await http.post('/exams/answers', payload)
  ElMessage.success('已保存')
}
async function submit(){
  for (const q of items.value) {
    try { await save(q) } catch(e) { /* 忽略保存失败，提交时后端仍会处理 */ }
  }
  const { data } = await http.post('/exams/submit', { sessionId: sessionId.value, paperId: paperId.value, studentId: studentId.value })
  submitted.value = true
  router.push(`/exam/success?sessionId=${sessionId.value}&paperId=${paperId.value}`)
}

// 反作弊：失焦/切屏事件上报
function report(type, detail){ http.post('/exams/events', { sessionId: sessionId.value, studentId: studentId.value, type, detail }) }
function lock(reason){ if (!submitted.value) { openLock.value = true; report('lock', reason) } }
window.addEventListener('blur', ()=> { report('blur','window'); lock('blur') })
document.addEventListener('visibilitychange', ()=> { report('visibility', document.visibilityState); if (document.visibilityState==='hidden') lock('visibility') })
document.addEventListener('fullscreenchange', ()=> { if (!document.fullscreenElement) lock('fullscreen-exit') })
function unlock(){ openLock.value = false; document.documentElement.requestFullscreen().catch(()=>{}) }
function connectWs(){
  try{
    ws = new WebSocket((location.protocol==='https:'?'wss://':'ws://')+location.host+'/ws/proctor')
    ws.onopen = ()=>{ ws.send(JSON.stringify({type:'join', sessionId: String(sessionId.value)})) }
    ws.onmessage = (e)=>{
      const msg = String(e.data)
      if (msg.startsWith('event:')) {
        const payload = msg.slice(6)
        if (payload.startsWith('warn:')) notice.value = payload.replace('warn:','')
        else if (payload.startsWith('kick')) { kicked.value = true }
      }
    }
  }catch{}
}

onMounted(()=>{
  load()
  document.documentElement.requestFullscreen().catch(()=>{})
  window.addEventListener('beforeunload', (e)=>{ if (!submitted.value) { e.preventDefault(); e.returnValue = '正在考试，确认离开？' } })
  endTime = Date.now() + remain.value * 60000
  timer = setInterval(()=>{
    const leftSec = Math.max(0, Math.floor((endTime - Date.now()) / 1000))
    remain.value = Math.ceil(leftSec / 60)
    if (leftSec <= 0) { submit(); clearInterval(timer) }
  }, 1000)
  connectWs()
})
function goHome(){ router.push('/') }
</script>
