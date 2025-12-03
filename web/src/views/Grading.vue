<template>
  <div style="padding:24px;">
    <el-page-header content="教师阅卷" @back="$router.back()" />
    <el-form inline style="margin-top:12px;">
      <el-form-item label="试卷">
        <el-select v-model="paperId" placeholder="选择试卷" style="width:260px;" @change="loadSessions">
          <el-option v-for="p in papers" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="仅待评分">
        <el-switch v-model="onlyPending" @change="loadSessions" />
      </el-form-item>
      <el-form-item label="会话">
        <el-select v-model="sessionId" placeholder="选择考试会话" style="width:360px;" @change="loadAnswers">
          <el-option v-for="s in sessions" :key="s.id" :label="`会话${s.id} 试卷${s.paperId} 待评分${s._pending}`" :value="s.id" />
        </el-select>
      </el-form-item>
      <el-button @click="loadSessions">刷新会话</el-button>
      <el-button type="danger" @click="cleanup">清理垃圾数据</el-button>
    </el-form>

    <el-card style="margin-top:16px;">
      <template #header>主观题待评分</template>
      <div style="margin-bottom:8px;">汇总：客观分 {{ summary.objective ?? 0 }}，主观分 {{ summary.subjective ?? 0 }}，总分 {{ summary.total ?? 0 }}</div>
      <el-table :data="subjective">
        <el-table-column prop="questionId" label="题目ID" width="100" />
        <el-table-column label="题型" width="100">
          <template #default="{ row }">{{ meta(row.questionId)?.type || '-' }}</template>
        </el-table-column>
        <el-table-column label="题干">
          <template #default="{ row }">{{ meta(row.questionId)?.stem || '-' }}</template>
        </el-table-column>
        <el-table-column label="考生答案">
          <template #default="{ row }">
            <template v-if="meta(row.questionId)?.type==='CODE'">
              <pre style="background:#f7f9fc;border:1px solid #e5e7eb;border-radius:6px;padding:8px;overflow:auto;white-space:pre-wrap;">{{ parse(row.answerJson)?.text || '' }}</pre>
            </template>
            <template v-else>
              {{ parse(row.answerJson)?.text || (parse(row.answerJson)?.values||[]).join('；') }}
            </template>
          </template>
        </el-table-column>
        <el-table-column label="满分" width="100">
          <template #default="{ row }">{{ meta(row.questionId)?.score ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="得分" width="200">
          <template #default="{ row }">
            <el-input-number v-model="row._score" :min="0" :max="meta(row.questionId)?.score || 100" />
            <el-space style="margin-left:8px;">
              <el-button size="small" @click="row._score=0">零分</el-button>
              <el-button size="small" @click="row._score=Math.floor((meta(row.questionId)?.score||0)/2)">半分</el-button>
              <el-button size="small" type="success" @click="row._score=meta(row.questionId)?.score||0">满分</el-button>
            </el-space>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="saveScore(row)">保存</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:8px;">
        <el-button type="success" :disabled="!sessionId" @click="markReviewed">标记为已阅</el-button>
        <el-button :disabled="!sessionId || subjective.length===0" @click="saveAll">保存全部评分</el-button>
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
const qmap = ref({})
const papers = ref([])
const paperId = ref(null)
const onlyPending = ref(true)
function parse(s){ try{ return s? JSON.parse(s):null }catch(e){ return null } }
async function loadSessions(){
  const { data } = await http.get('/exams/sessions', { params: { paperId: paperId.value||undefined, onlyPending: onlyPending.value } })
  const arr = data?.data||[]
  for (const s of arr){ const pc = await http.get(`/exams/sessions/${s.id}/pending-count`); s._pending = Number(pc?.data?.data||0) }
  sessions.value = arr
}
async function loadAnswers(){
  if(!sessionId.value) return;
  const { data } = await http.get(`/exams/sessions/${sessionId.value}/answers`);
  const arr=data?.data||[];
  subjective.value = arr.filter(a=> a.score_auto==null).map(a=> ({...a, _score: a.score_final||0}))
  const sm = await http.get(`/exams/sessions/${sessionId.value}/summary`); summary.value = sm?.data?.data || {objective:0,subjective:0,total:0}
  // 载入题目元信息（题型/题干/分值）
  const ses = sessions.value.find(s=> s.id===sessionId.value)
  if (ses?.paperId) {
    const pv = await http.get(`/exams/papers/${ses.paperId}/preview`)
    const list = pv?.data?.data || []
    const m = {}
    for (const q of list) m[q.id] = { type: q.type, stem: q.stem, score: q.score }
    qmap.value = m
  }
}
async function saveScore(row){ const { data } = await http.put(`/exams/sessions/${sessionId.value}/answers/${row.questionId}/score`, null, { params:{ score: row._score } }); if (data?.code===0){ ElMessage.success('已保存'); await loadAnswers(); await loadSessions() } else ElMessage.error(data?.message||'保存失败') }
async function markReviewed(){ const { data } = await http.put(`/exams/sessions/${sessionId.value}/status`, null, { params:{ status: 'REVIEWED' } }); if (data?.code===0){ ElMessage.success('已标记'); await loadSessions() } }
async function saveAll(){ for (const r of subjective.value) { await saveScore(r) } }
function meta(qid){ return qmap.value[qid] || null }
async function cleanup(){ const res = await http.post('/exams/cleanup', null, { params: { dry: false } }); if (res?.data?.code===0) { ElMessage.success('已清理'); await loadSessions() } }
onMounted(async()=>{ const pv = await http.get('/papers'); papers.value = pv?.data?.data||[]; await loadSessions() })
</script>
