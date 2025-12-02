<template>
  <div style="padding:24px;">
    <el-page-header content="我的考试记录" @back="$router.back()" />
    <el-card style="margin-top:16px;">
      <el-table :data="rows">
        <el-table-column prop="sessionId" label="会话" width="160" />
        <el-table-column prop="paperId" label="试卷ID" width="100" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column prop="objective" label="客观分" width="120" />
        <el-table-column prop="subjective" label="主观分" width="120" />
        <el-table-column prop="total" label="总分" width="120" />
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">详情</el-button>
            <el-button size="small" type="primary" @click="retake(row)">重新进入</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="open" title="考试详情" width="60%">
      <div v-if="summary">
        <div>会话：{{ current?.sessionId }}，试卷：{{ current?.paperId }}</div>
        <div>客观分：{{ summary.objective }}；主观分：{{ summary.subjective }}；总分：{{ summary.total }}</div>
      </div>
      <template #footer>
        <el-button @click="open=false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import http from '../api/http'
import { ElMessage } from 'element-plus'
const rows = ref([])
const studentId = 1
const open = ref(false)
const current = ref(null)
const summary = ref(null)
async function load(){ const { data } = await http.get(`/exams/students/${studentId}/records`); rows.value = data?.data||[] }
async function openDetail(row){ current.value=row; const { data } = await http.get(`/exams/sessions/${row.sessionId}/summary`); summary.value = data?.data||null; open.value=true }
function retake(row){ location.href = `/exam/take?sessionId=${row.sessionId}&paperId=${row.paperId}&duration=60` }
onMounted(load)
</script>

