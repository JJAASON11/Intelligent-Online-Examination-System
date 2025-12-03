<template>
  <div style="padding:24px;">
    <el-page-header content="考试中心（到时可进入）" @back="$router.back()" />
    <el-card style="margin-top:16px;">
      <div style="margin-bottom:12px; display:flex; gap:8px; align-items:center;">
        <el-date-picker v-model="day" type="date" value-format="YYYY-MM-DD" />
        <el-button type="primary" @click="load">刷新</el-button>
      </div>
      <el-table :data="rows">
        <el-table-column prop="id" label="会话" width="160" />
        <el-table-column prop="paperId" label="试卷ID" width="120" />
        <el-table-column prop="startAt" label="开始时间" width="180" />
        <el-table-column prop="endAt" label="结束时间" width="180" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button size="small" type="primary" :disabled="!canEnter(row)" @click="enter(row)">进入考试</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import http from '../api/http'
const rows = ref([])
const day = ref(new Date().toISOString().slice(0,10))
function canEnter(r){ const now = Date.now(); const s = new Date(r.startAt).getTime(); const e = new Date(r.endAt).getTime(); return now>=s && now<=e }
async function load(){
  try {
    const { data } = await http.get('/exams/sessions/available', { params: { date: day.value } });
    rows.value = data?.data||[]
  } catch(e) {
    try {
      const { data } = await http.get('/exams/sessions/open');
      rows.value = data?.data||[]
    } catch(err) {
      rows.value = []
    }
  }
}
function enter(r){ location.href = `/exam/take?sessionId=${r.id}&paperId=${r.paperId}&duration=60` }
onMounted(load)
</script>
