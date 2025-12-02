<template>
  <div style="padding:24px;">
    <el-page-header content="在线监考" @back="$router.back()" />
    <el-form inline style="margin-top:12px;">
      <el-form-item label="会话ID">
        <el-input v-model="sid" placeholder="sessionId" style="width:220px;" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :disabled="isConnected" @click="connect">连接并加入</el-button>
        <el-button :disabled="!isConnected" @click="disconnect">断开</el-button>
      </el-form-item>
    </el-form>

    <el-card style="margin-top:12px;">状态：{{ isConnected ? '已连接' : '未连接' }}</el-card>

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

function connect(){
  ws = new WebSocket((location.protocol==='https:'?'wss://':'ws://')+location.host+'/ws/proctor')
  ws.onopen = ()=>{
    isConnected.value = true
    join()
  }
  ws.onmessage = (e)=>{
    messages.value.push(e.data)
    if (String(e.data).startsWith('event:')) events.value.unshift(e.data.replace('event:',''))
  }
  ws.onclose = ()=>{ isConnected.value = false }
}
function join(){ if (ws && isConnected.value) ws.send(JSON.stringify({type:'join', sessionId: sid.value})) }
function disconnect(){ if (ws) { ws.close(); ws=null } }
function handleSend(){ if (ws && isConnected.value) ws.send(JSON.stringify({type:'event', sessionId: sid.value, payload: inputMsg.value})) }

async function pull(){ if(!sid.value) return; const { data } = await http.get(`/exams/events/${sid.value}/1`); const arr = data?.data||[]; events.value = arr.concat(events.value).slice(0,200) }
let poller
onMounted(()=>{ poller = setInterval(pull, 5000) })
</script>
