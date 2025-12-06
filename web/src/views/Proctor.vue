<template>
  <div style="padding:24px;">
    <el-page-header content="在线监考" @back="$router.back()" />
  <el-form inline style="margin-top:12px;">
    <el-form-item label="会话ID">
      <el-input v-model="sid" placeholder="sessionId" style="width:220px;" />
    </el-form-item>
    <el-form-item label="学生ID">
      <el-input v-model="studentId" placeholder="studentId" style="width:160px;" />
    </el-form-item>
    <el-form-item>
      <el-button type="primary" :disabled="isConnected" @click="connect">连接并加入</el-button>
      <el-button :disabled="!isConnected" @click="disconnect">断开</el-button>
    </el-form-item>
  </el-form>

    <el-card style="margin-top:12px;">状态：{{ isConnected ? '已连接' : '未连接' }}，切屏次数：<el-tag type="danger">{{ switchCount }}</el-tag></el-card>

    <el-card style="margin-top:12px;">
      <template #header>
        <div class="card-header">事件流</div>
      </template>
      <div style="margin-bottom:8px;">累计事件：<el-tag type="danger">{{ events.length }}</el-tag></div>
      <div v-if="events.length===0" style="color:#909399;">暂无事件</div>
      <el-timeline v-else>
        <el-timeline-item v-for="(ev,i) in events" :key="i">{{ ev }}</el-timeline-item>
      </el-timeline>
    </el-card>

    <el-card style="margin-top:12px;">
      <template #header>
        <div class="card-header">发送告警/消息</div>
      </template>
      <el-space>
        <el-input v-model="inputMsg" placeholder="输入内容" style="width:260px;" />
        <el-button type="success" :disabled="!isConnected" @click="handleSend">发送到房间</el-button>
        <el-button type="warning" :disabled="!isConnected" @click="sendWarn">发送告警</el-button>
        <el-button type="danger" :disabled="!isConnected" @click="kickExam">终止考试</el-button>
      </el-space>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import http from '../api/http'

let ws
const isConnected = ref(false)
const messages = ref([])
const events = ref([])
const inputMsg = ref('')
const sid = ref(new URLSearchParams(location.search).get('sessionId')||'')
const studentId = ref('1')
const switchCount = ref(0)

function connect(){
  ws = new WebSocket((location.protocol==='https:'?'wss://':'ws://')+location.host+'/ws/proctor')
  ws.onopen = ()=>{
    isConnected.value = true
    join()
    // 连接成功则停止轮询
    if (poller) { clearInterval(poller); poller = null }
  }
  ws.onmessage = (e)=>{
    messages.value.push(e.data)
    if (String(e.data).startsWith('event:')) events.value.unshift(e.data.replace('event:',''))
  }
  ws.onclose = ()=>{
    isConnected.value = false
    // 断开后启用兜底轮询，降低频率
    if (!poller) poller = setInterval(pull, 15000)
  }
}
function join(){ if (ws && isConnected.value) ws.send(JSON.stringify({type:'join', sessionId: sid.value})) }
function disconnect(){ if (ws) { ws.close(); ws=null } }
function handleSend(){ if (ws && isConnected.value) ws.send(JSON.stringify({type:'event', sessionId: sid.value, payload: inputMsg.value})) }
function sendWarn(){ if (ws && isConnected.value) ws.send(JSON.stringify({type:'event', sessionId: sid.value, payload: 'warn:'+ (inputMsg.value||'请勿切屏') })) }
function kickExam(){ if (ws && isConnected.value) ws.send(JSON.stringify({type:'event', sessionId: sid.value, payload: 'kick' })) }

async function pull(){
  if(!sid.value || !studentId.value) return;
  const ev = await http.get(`/exams/events/${sid.value}/${studentId.value}`);
  const arr = ev?.data?.data||[]; events.value = arr.concat(events.value).slice(0,200)
  const sc = await http.get(`/exams/events/${sid.value}/${studentId.value}/switches`);
  switchCount.value = Number(sc?.data?.data||0)
}
let poller
onMounted(()=>{ poller = setInterval(pull, 15000) })
</script>
