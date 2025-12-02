<template>
  <div style="padding:24px;">
    <el-page-header content="教师阅卷" @back="$router.back()" />
    <el-form inline style="margin-top:12px;">
      <el-form-item label="会话">
        <el-select v-model="sessionId" placeholder="选择考试会话" style="width:260px;" @change="loadAnswers">
          <el-option v-for="s in sessions" :key="s.id" :label="`会话${s.id} 试卷${s.paperId} 分数${s.totalScore??0}`" :value="s.id" />
        </el-select>
      </el-form-item>
      <el-button @click="loadSessions">刷新会话</el-button>
    </el-form>

    <el-card style="margin-top:16px;">
      <template #header>主观题待评分</template>
      <div style="margin-bottom:8px;">汇总：客观分 {{ summary.objective ?? 0 }}，主观分 {{ summary.subjective ?? 0 }}，总分 {{ summary.total ?? 0 }}</div>
      <el-table :data="subjective">
        <el-table-column prop="questionId" label="题目ID" width="100" />
        <el-table-column label="考生答案">
          <template #default="{ row }">{{ parse(row.answerJson)?.text || (parse(row.answerJson)?.values||[]).join('；') }}</template>
        </el-table-column>
        <el-table-column label="得分" width="150">
          <template #default="{ row }">
            <el-input-number v-model="row._score" :min="0" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="saveScore(row)">保存</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:8px;">
        <el-button type="success" :disabled="!sessionId" @click="markReviewed">标记为已阅</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'
const sessions = ref([])
const sessionId = ref(null)
const subjective = ref([])
const summary = ref({objective:0,subjective:0,total:0})
function parse(s){ try{ return s? JSON.parse(s):null }catch(e){ return null } }
async function loadSessions(){ const { data } = await http.get('/exams/sessions'); sessions.value = data?.data||[] }
async function loadAnswers(){ if(!sessionId.value) return; const { data } = await http.get(`/exams/sessions/${sessionId.value}/answers`); const arr=data?.data||[]; subjective.value = arr.filter(a=> a.score_auto==null).map(a=> ({...a, _score: a.score_final||0})) ; const sm = await http.get(`/exams/sessions/${sessionId.value}/summary`); summary.value = sm?.data?.data || {objective:0,subjective:0,total:0} }
async function saveScore(row){ const { data } = await http.put(`/exams/sessions/${sessionId.value}/answers/${row.questionId}/score`, null, { params:{ score: row._score } }); if (data?.code===0){ ElMessage.success('已保存'); await loadAnswers(); await loadSessions() } else ElMessage.error(data?.message||'保存失败') }
async function markReviewed(){ const { data } = await http.put(`/exams/sessions/${sessionId.value}/status`, null, { params:{ status: 'REVIEWED' } }); if (data?.code===0){ ElMessage.success('已标记'); await loadSessions() } }
onMounted(loadSessions)
</script>
