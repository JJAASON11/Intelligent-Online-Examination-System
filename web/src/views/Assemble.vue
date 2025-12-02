<template>
  <div style="padding:24px;">
    <el-page-header content="自动组卷" @back="$router.back()" />

    <el-card style="margin-top:16px;">
      <div style="font-weight:600; margin-bottom:8px;">一次性创建并生成</div>
      <el-form inline>
        <el-form-item label="名称"><el-input v-model="paperName" placeholder="试卷名称" /></el-form-item>
        <el-form-item label="时长"><el-input-number v-model="durationMin" :min="10" /></el-form-item>
        <el-form-item label="单选数量"><el-input-number v-model="strategy.SINGLE" :min="0" /></el-form-item>
        <el-form-item label="多选数量"><el-input-number v-model="strategy.MULTI" :min="0" /></el-form-item>
        <el-form-item label="判断数量"><el-input-number v-model="strategy.JUDGE" :min="0" /></el-form-item>
        <el-form-item label="填空数量"><el-input-number v-model="strategy.FILL" :min="0" /></el-form-item>
        <el-form-item label="简答数量"><el-input-number v-model="strategy.SHORT" :min="0" /></el-form-item>
        <el-form-item label="单选分值"><el-input-number v-model="typeScores.SINGLE" :min="1" /></el-form-item>
        <el-form-item label="多选分值"><el-input-number v-model="typeScores.MULTI" :min="1" /></el-form-item>
        <el-form-item label="判断分值"><el-input-number v-model="typeScores.JUDGE" :min="1" /></el-form-item>
        <el-form-item label="填空分值"><el-input-number v-model="typeScores.FILL" :min="1" /></el-form-item>
        <el-form-item label="简答分值"><el-input-number v-model="typeScores.SHORT" :min="1" /></el-form-item>
        <el-button type="primary" :loading="creating" :disabled="creating" @click="createAndAssemble">创建并生成新试卷</el-button>
        <el-button type="success" :loading="creating" :disabled="creating || !currentPaperId" @click="regenerateCurrent">用当前试卷重新生成</el-button>
        <el-button type="text" @click="currentPaperId=null" :disabled="!currentPaperId">清除当前</el-button>
      </el-form>
      <div style="margin-top:8px; color:#909399;">当前试卷ID：{{ currentPaperId || '-' }}；预计算总分：{{ calcTotal }}</div>
    </el-card>

    <el-card style="margin-top:16px;">
      <div style="font-weight:600; margin-bottom:8px;">试卷列表</div>
      <el-table :data="papers" @row-click="setCurrent">
        <el-table-column label="编号" width="80">
          <template #default="{ $index }">{{ $index + 1 }}</template>
        </el-table-column>
        
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="totalScore" label="总分" width="100" />
        <el-table-column prop="durationMin" label="时长(分钟)" width="120" />
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button size="small" @click="previewPaper(row.id)">预览</el-button>
            <el-button size="small" type="warning" @click="editPaper(row.id)">编辑</el-button>
            <el-button size="small" type="danger" @click="deletePaper(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="openPreview" :fullscreen="fullscreen" title="试卷预览（含答案）" width="80%">
      <div v-for="(q, idx) in previewItems" :key="q.id" style="margin-bottom:12px;">
        <el-card>
          <div style="font-weight:600;">第{{ idx+1 }}题（{{ q.type }}，{{ q._score || 1 }}分）</div>
          <div style="margin-top:8px;">{{ q.stem }}</div>
          <div v-if="q.type==='SINGLE'">
            <el-radio-group :model-value="-1">
              <el-radio v-for="(opt,i) in parse(q.optionsJson)" :key="i" :label="i" disabled>{{ String.fromCharCode(65+i) }}. {{ opt }}</el-radio>
            </el-radio-group>
          </div>
          <div v-else-if="q.type==='MULTI'">
            <el-checkbox-group :model-value="[]" disabled>
              <el-checkbox v-for="(opt,i) in parse(q.optionsJson)" :key="i" :label="i">{{ String.fromCharCode(65+i) }}. {{ opt }}</el-checkbox>
            </el-checkbox-group>
          </div>
          <div v-else-if="q.type==='JUDGE'">
            <el-radio-group :model-value="'TRUE'" disabled>
              <el-radio label="TRUE">正确</el-radio>
              <el-radio label="FALSE">错误</el-radio>
            </el-radio-group>
          </div>
          <div v-else-if="q.type==='FILL'">
            <div v-for="(ans,i) in parse(q.answerJson)||[]" :key="i" style="margin-bottom:8px;">
              <el-input :model-value="''" disabled />
            </div>
          </div>
          <div v-else-if="q.type==='SHORT'">
            <el-input type="textarea" :model-value="''" disabled />
          </div>
          <div style="margin-top:8px;" v-if="showAnswer">
            <div style="font-weight:600;">答案</div>
            <div v-if="q.type==='SINGLE'">{{ toLetters([parse(q.answerJson)?.index]) }}</div>
            <div v-else-if="q.type==='MULTI'">{{ toLetters(parse(q.answerJson)?.indexes||[]) }}</div>
            <div v-else-if="q.type==='JUDGE'">{{ (parse(q.answerJson)?.value)? '正确':'错误' }}</div>
            <div v-else-if="q.type==='FILL'">{{ (parse(q.answerJson)||[]).join('；') }}</div>
            <div v-else-if="q.type==='SHORT'">{{ parse(q.answerJson)?.text || '-' }}</div>
            <div style="margin-top:8px; display:flex; align-items:center; gap:8px;">
              <span>分值</span>
              <el-input-number v-model="q._score" :min="0" @change="onScoreChange(q)" />
              <el-button size="small" @click="saveScore(q)">保存分值</el-button>
            </div>
          </div>
        </el-card>
      </div>
      <template #footer>
        <el-space>
          <el-button @click="fullscreen=!fullscreen" type="primary" plain>{{ fullscreen?'退出全屏':'全屏' }}</el-button>
          <el-switch v-model="showAnswer" active-text="显示答案" inactive-text="隐藏答案" />
          <el-button type="primary" plain @click="saveAllScores">保存全部分值</el-button>
          <el-button @click="openPreview=false">关闭</el-button>
        </el-space>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import http from '../api/http'
import { ElMessage } from 'element-plus'
import { computed } from 'vue'
const paperName = ref('新试卷')
const durationMin = ref(60)
const creating = ref(false)
const currentPaperId = ref(null)
const strategy = ref({ SINGLE:5, MULTI:5, JUDGE:5, FILL:5, SHORT:0 })
const typeScores = ref({ SINGLE:1, MULTI:1, JUDGE:1, FILL:1, SHORT:1 })
const calcTotal = computed(()=> {
  const s = strategy.value, t = typeScores.value
  return (s.SINGLE*t.SINGLE) + (s.MULTI*t.MULTI) + (s.JUDGE*t.JUDGE) + (s.FILL*t.FILL) + (s.SHORT*t.SHORT)
})
const actionLabel = computed(()=> currentPaperId.value ? '用当前试卷重新生成' : '创建并生成')
const papers = ref([])
const openPreview = ref(false)
const previewItems = ref([])
const showAnswer = ref(true)
const fullscreen = ref(false)
const pendingScores = ref({})

function parse(s){ try{ return s? JSON.parse(s) : null }catch(e){ return null } }
function toLetters(arr){ return (arr||[]).map(i=> String.fromCharCode(65+i)).join(', ') || '-' }

async function createAndAssemble(){
  if (creating.value) return
  creating.value = true
  try{
    const body = { name: paperName.value, durationMin: durationMin.value, strategy: strategy.value, typeScores: typeScores.value }
    const { data } = await http.post('/papers/create-and-assemble', body)
    if (data?.code===0){ currentPaperId.value = data.data; ElMessage.success('试卷已创建并生成'); await loadPapers(); await previewPaper(currentPaperId.value) }
    else ElMessage.error(data?.message||'创建失败')
  } finally { creating.value = false }
}

async function regenerateCurrent(){
  if (!currentPaperId.value) { ElMessage.warning('请先选择试卷'); return }
  if (creating.value) return
  creating.value = true
  try{
    const body = { strategy: strategy.value, typeScores: typeScores.value }
    const { data } = await http.post(`/papers/${currentPaperId.value}/assemble-type-scores`, body)
    if (data?.code===0){ ElMessage.success('当前试卷已重新生成'); await loadPapers(); await previewPaper(currentPaperId.value) }
    else ElMessage.error(data?.message||'生成失败')
  } finally { creating.value = false }
}

function createOrRegenerate(){
  if (currentPaperId.value) regenerateCurrent(); else createAndAssemble();
}

async function loadPapers(){
  const { data } = await http.get('/papers')
  papers.value = data?.data || []
}

async function previewPaper(id){
  const { data } = await http.get(`/papers/${id}/preview`)
  previewItems.value = (data?.data || []).map(q => ({ ...q, _score: q.score || 1 }))
  // 应用未保存的临时分值
  for (const q of previewItems.value) {
    const ps = pendingScores.value[q.id]
    if (ps !== undefined) q._score = ps
  }
  openPreview.value = true
}

function setCurrent(row){ currentPaperId.value = row.id }
async function saveScore(q){
  if (!currentPaperId.value) return
  const { data } = await http.put(`/papers/${currentPaperId.value}/question/${q.id}/score`, null, { params:{ score: q._score } })
  if (data?.code===0){ ElMessage.success('分值已保存'); await loadPapers(); delete pendingScores.value[q.id] } else { ElMessage.error(data?.message||'保存失败') }
}
function onScoreChange(q){ pendingScores.value[q.id] = q._score }
async function saveAllScores(){
  if (!currentPaperId.value) return
  const ids = Object.keys(pendingScores.value)
  for (const id of ids) {
    const score = pendingScores.value[id]
    await http.put(`/papers/${currentPaperId.value}/question/${id}/score`, null, { params:{ score } })
  }
  ElMessage.success('全部分值已保存')
  pendingScores.value = {}
  await loadPapers()
}

async function deletePaper(id){
  const { data } = await http.delete(`/papers/${id}`)
  if (data?.code===0){ ElMessage.success('已删除'); if (currentPaperId.value===id) currentPaperId.value=null; await loadPapers() } else { ElMessage.error(data?.message||'删除失败') }
}
function editPaper(id){ currentPaperId.value = id; previewPaper(id) }

// 本地草稿，避免切换页面丢失
const DRAFT_KEY = 'assemble_draft'
function saveDraft(){
  const draft = {
    paperName: paperName.value,
    durationMin: durationMin.value,
    currentPaperId: currentPaperId.value,
    strategy: strategy.value,
    typeScores: typeScores.value,
    pendingScores: pendingScores.value
  }
  localStorage.setItem(DRAFT_KEY, JSON.stringify(draft))
}
function loadDraft(){
  try {
    const raw = localStorage.getItem(DRAFT_KEY)
    if (!raw) return
    const d = JSON.parse(raw)
    if (d.paperName) paperName.value = d.paperName
    if (d.durationMin) durationMin.value = d.durationMin
    if (d.currentPaperId) currentPaperId.value = d.currentPaperId
    if (d.strategy) strategy.value = d.strategy
    if (d.typeScores) typeScores.value = d.typeScores
    if (d.pendingScores) pendingScores.value = d.pendingScores
  } catch {}
}
onMounted(()=>{ loadPapers(); loadDraft() })
watch([paperName,durationMin,currentPaperId,strategy,typeScores,pendingScores], saveDraft, { deep:true })
</script>
