<template>
  <div class="page-container">
    <el-page-header content="智能组卷中心" @back="$router.back()" class="page-header" title="返回" />

    <el-card class="main-card" shadow="never">
      <div class="config-header">
        <div class="header-left">
          <div class="header-icon-box"><el-icon><Setting /></el-icon></div>
          <span class="title-text">组卷策略配置</span>
        </div>
        <div class="header-right">
          <div class="score-badge">
            <span class="label">预估总分</span>
            <span class="value">{{ calcTotal }}</span>
            <span class="unit">分</span>
          </div>
        </div>
      </div>

      <div class="basic-form-area">
        <el-row :gutter="20" align="middle">
          <el-col :span="8">
            <div class="input-group">
              <span class="input-label">试卷名称</span>
              <el-input v-model="paperName" placeholder="例如：2026年期末模拟考试" size="large" clearable>
                <template #prefix><el-icon><Document /></el-icon></template>
              </el-input>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="input-group">
              <span class="input-label">考试时长(分钟)</span>
              <el-input-number v-model="durationMin" :min="10" :step="10" size="large" style="width: 100%;" controls-position="right" />
            </div>
          </el-col>
          <el-col :span="5">
            <div class="input-group">
              <span class="input-label">目标难度</span>
              <el-select v-model="selectedDifficulty" placeholder="选择难度" size="large" style="width: 100%;">
                <el-option
                  v-for="opt in difficultyOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </div>
          </el-col>
          <el-col :span="5" class="action-col">
            <el-button type="primary" size="large" :loading="creating" :disabled="creating" icon="Plus" @click="createAndAssemble" class="action-btn">
              一键生成试卷
            </el-button>
            <el-button
                v-if="currentPaperId"
                type="warning"
                plain
                size="large"
                :loading="creating"
                :disabled="creating"
                icon="Refresh"
                @click="regenerateCurrent"
                class="action-btn"
            >
              重置当前
            </el-button>
            <el-button v-if="currentPaperId" type="info" link @click="currentPaperId=null">取消选中</el-button>
          </el-col>
        </el-row>

        <transition name="el-fade-in">
          <div v-if="currentPaperId" class="current-hint">
            <el-alert :title="'正在编辑试卷 ID: ' + currentPaperId" type="success" :closable="false" show-icon />
          </div>
        </transition>
      </div>

      <el-divider content-position="left"><span class="divider-text">题型分布与分值设定</span></el-divider>

      <div class="strategy-container">
        <div class="strategy-card card-blue">
          <div class="card-top">
            <div class="icon-box"><el-icon><CircleCheck /></el-icon></div>
            <span class="card-title">单选题</span>
          </div>
          <div class="card-body">
            <div class="control-row">
              <span class="sub-label">题数</span>
              <el-input-number v-model="strategy.SINGLE" :min="0" size="small" controls-position="right" />
            </div>
            <div class="control-row">
              <span class="sub-label">分值</span>
              <el-input-number v-model="typeScores.SINGLE" :min="0.5" :step="0.5" size="small" controls-position="right" />
            </div>
          </div>
        </div>

        <div class="strategy-card card-green">
          <div class="card-top">
            <div class="icon-box"><el-icon><CopyDocument /></el-icon></div>
            <span class="card-title">多选题</span>
          </div>
          <div class="card-body">
            <div class="control-row">
              <span class="sub-label">题数</span>
              <el-input-number v-model="strategy.MULTI" :min="0" size="small" controls-position="right" />
            </div>
            <div class="control-row">
              <span class="sub-label">分值</span>
              <el-input-number v-model="typeScores.MULTI" :min="0.5" :step="0.5" size="small" controls-position="right" />
            </div>
          </div>
        </div>

        <div class="strategy-card card-orange">
          <div class="card-top">
            <div class="icon-box"><el-icon><Switch /></el-icon></div>
            <span class="card-title">判断题</span>
          </div>
          <div class="card-body">
            <div class="control-row">
              <span class="sub-label">题数</span>
              <el-input-number v-model="strategy.JUDGE" :min="0" size="small" controls-position="right" />
            </div>
            <div class="control-row">
              <span class="sub-label">分值</span>
              <el-input-number v-model="typeScores.JUDGE" :min="0.5" :step="0.5" size="small" controls-position="right" />
            </div>
          </div>
        </div>

        <div class="strategy-card card-purple">
          <div class="card-top">
            <div class="icon-box"><el-icon><EditPen /></el-icon></div>
            <span class="card-title">填空题</span>
          </div>
          <div class="card-body">
            <div class="control-row">
              <span class="sub-label">题数</span>
              <el-input-number v-model="strategy.FILL" :min="0" size="small" controls-position="right" />
            </div>
            <div class="control-row">
              <span class="sub-label">分值</span>
              <el-input-number v-model="typeScores.FILL" :min="0.5" :step="0.5" size="small" controls-position="right" />
            </div>
          </div>
        </div>

        <div class="strategy-card card-red">
          <div class="card-top">
            <div class="icon-box"><el-icon><ChatLineSquare /></el-icon></div>
            <span class="card-title">简答题</span>
          </div>
          <div class="card-body">
            <div class="control-row">
              <span class="sub-label">题数</span>
              <el-input-number v-model="strategy.SHORT" :min="0" size="small" controls-position="right" />
            </div>
            <div class="control-row">
              <span class="sub-label">分值</span>
              <el-input-number v-model="typeScores.SHORT" :min="0.5" :step="0.5" size="small" controls-position="right" />
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <el-card class="list-card" shadow="never">
      <template #header>
        <div class="list-header">
          <span class="list-title"><el-icon><List /></el-icon> 试卷管理列表</span>
          <el-button icon="Refresh" circle @click="loadPapers" title="刷新列表" />
        </div>
      </template>

      <el-table :data="papers" stripe highlight-current-row @row-click="setCurrent" row-key="id">
        <el-table-column label="序号" width="70" align="center">
          <template #default="{ $index }"><span class="index-badge">{{ $index + 1 }}</span></template>
        </el-table-column>
        <el-table-column prop="name" label="试卷名称" min-width="200">
          <template #default="{ row }">
            <span style="font-weight: 600; color: #303133;">{{ row.name }}</span>
            <el-tag v-if="row.id === currentPaperId" size="small" type="success" effect="light" style="margin-left:8px;">当前选中</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalScore" label="总分" width="100" align="center">
          <template #default="{ row }"><span class="score-cell">{{ row.totalScore }} 分</span></template>
        </el-table-column>
        <el-table-column prop="durationMin" label="时长" width="120" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small"><el-icon><Timer /></el-icon> {{ row.durationMin }}m</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button-group>
              <el-button type="primary" link size="small" @click.stop="previewPaper(row.id)">预览</el-button>
              <el-button type="warning" link size="small" @click.stop="editPaper(row.id)">配置</el-button>
              <el-button type="danger" link size="small" @click.stop="deletePaper(row.id)">删除</el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="openPreview" :fullscreen="fullscreen" title="试卷预览" width="850px" top="5vh" class="preview-dialog">
      <div class="preview-content">
        <div class="paper-header">
          <h2>{{ previewPaperName || '试卷预览' }}</h2>
          <div class="paper-meta">
            <span>总分: {{ previewTotalScore }}</span>
            <el-divider direction="vertical" />
            <span>时长: {{ durationMin }}分钟</span>
            <el-divider direction="vertical" />
            <span>
              难度：
              <el-tag :type="getDifficultyTagType(selectedDifficulty)" size="small" effect="plain">
                {{ getDifficultyText(selectedDifficulty) }}
              </el-tag>
            </span>
          </div>
        </div>

        <div v-for="(q, idx) in previewItems" :key="q.id" class="q-item">
          <div class="q-header-row">
            <span class="q-idx">第 {{ idx+1 }} 题</span>
            <el-tag size="small" effect="plain">{{ q.type }}</el-tag>
            <el-tag
              v-if="q.difficulty"
              size="small"
              :type="getQuestionDifficultyTagType(q.difficulty)"
              effect="light"
              class="q-diff-tag"
            >
              难度：{{ getQuestionDifficultyText(q.difficulty) }}
            </el-tag>
            <div class="q-score-edit">
              分值: <el-input-number v-model="q._score" size="small" :min="0" :step="0.5" style="width:90px; margin:0 8px;" @change="onScoreChange(q)" />
              <el-button v-if="pendingScores[q.id]!==undefined" type="primary" link size="small" @click="saveScore(q)">保存</el-button>
            </div>
          </div>

          <div class="q-content">
            <div class="stem" v-html="q.stem"></div>

            <div class="options-area">
              <template v-if="q.type==='SINGLE' || q.type==='MULTI'">
                <div v-for="(opt,i) in parse(q.optionsJson)" :key="i" class="opt-row">
                  <span class="opt-letter">{{ String.fromCharCode(65+i) }}.</span>
                  <span class="opt-text">{{ opt }}</span>
                </div>
              </template>
              <template v-else-if="q.type==='JUDGE'">
                <el-radio-group disabled>
              <el-radio label="TRUE">正确</el-radio>
              <el-radio label="FALSE">错误</el-radio>
            </el-radio-group>
              </template>
              <template v-else>
                <el-input disabled placeholder="学生作答区域..." style="width: 50%;" />
              </template>
            </div>

            <div v-if="showAnswer" class="answer-section">
              <span class="ans-label">参考答案：</span>
              <span class="ans-val" v-if="q.type==='SINGLE'">{{ toLetters([parse(q.answerJson)?.index]) }}</span>
              <span class="ans-val" v-else-if="q.type==='MULTI'">{{ toLetters(parse(q.answerJson)?.indexes||[]) }}</span>
              <span class="ans-val" v-else-if="q.type==='JUDGE'">
                <el-tag :type="parse(q.answerJson)?.value ? 'success':'danger'" size="small">{{ (parse(q.answerJson)?.value)? '正确':'错误' }}</el-tag>
              </span>
              <span class="ans-val" v-else-if="q.type==='FILL'">{{ (parse(q.answerJson)||[]).join(' ； ') }}</span>
              <span class="ans-val" v-else-if="q.type==='SHORT'">{{ parse(q.answerJson)?.text || '略' }}</span>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <div class="left">
            <el-switch v-model="showAnswer" active-text="显示答案解析" />
          </div>
          <div class="right">
            <el-button @click="fullscreen=!fullscreen" icon="FullScreen" circle />
          <el-button @click="openPreview=false">关闭</el-button>
            <el-button type="primary" @click="saveAllScores">保存所有分值修改</el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import http from '../api/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Document, Plus, Refresh, Setting, List, Timer,
  CircleCheck, CopyDocument, Switch, EditPen, ChatLineSquare, FullScreen
} from '@element-plus/icons-vue'

const route = useRoute()

// --- State ---
const paperName = ref('')
const durationMin = ref(60)
const creating = ref(false)
const currentPaperId = ref(null)
const strategy = ref({ SINGLE:5, MULTI:5, JUDGE:5, FILL:5, SHORT:2 })
const typeScores = ref({ SINGLE:2, MULTI:4, JUDGE:2, FILL:2, SHORT:10 })

// 试卷目标难度（1-5）
const selectedDifficulty = ref(3)
const difficultyOptions = [
  { value: 1, label: '很容易 (★☆☆☆☆)' },
  { value: 2, label: '较容易 (★★☆☆☆)' },
  { value: 3, label: '适中 (★★★☆☆)' },
  { value: 4, label: '较困难 (★★★★☆)' },
  { value: 5, label: '很困难 (★★★★★)' }
]

const papers = ref([])
const openPreview = ref(false)
const previewItems = ref([])
const showAnswer = ref(true)
const fullscreen = ref(false)
const pendingScores = ref({})
const previewPaperName = ref('')

// --- Computed ---
const calcTotal = computed(()=> {
  const s = strategy.value, t = typeScores.value
  return (s.SINGLE*t.SINGLE) + (s.MULTI*t.MULTI) + (s.JUDGE*t.JUDGE) + (s.FILL*t.FILL) + (s.SHORT*t.SHORT)
})
const previewTotalScore = computed(() => previewItems.value.reduce((acc, cur) => acc + (Number(cur._score) || 0), 0))

// --- Helpers ---
function parse(s){ try{ return s? JSON.parse(s) : null }catch(e){ return null } }
function toLetters(arr){ return (arr||[]).map(i=> String.fromCharCode(65+i)).join(', ') || '-' }

function getDifficultyText(level) {
  switch (Number(level)) {
    case 1: return '很容易'
    case 2: return '较容易'
    case 3: return '适中'
    case 4: return '较困难'
    case 5: return '很困难'
    default: return '未设定'
  }
}
function getDifficultyTagType(level) {
  const v = Number(level)
  if (v <= 1) return 'success'
  if (v === 2) return 'info'
  if (v === 3) return 'warning'
  if (v >= 4) return 'danger'
  return 'info'
}
function getQuestionDifficultyText(level) {
  if (!level || level <= 0) return '未设定'
  return getDifficultyText(level)
}
function getQuestionDifficultyTagType(level) {
  return getDifficultyTagType(level)
}

// --- Actions ---
async function createAndAssemble(){
  if(!paperName.value) paperName.value = '未命名试卷'
  if (creating.value) return
  creating.value = true
  try{
    const body = {
      name: paperName.value,
      durationMin: durationMin.value,
      strategy: strategy.value,
      typeScores: typeScores.value,
      difficulty: selectedDifficulty.value
    }
    const { data } = await http.post('/papers/create-and-assemble', body)
    if (data?.code===0){
      currentPaperId.value = data.data;
      ElMessage.success('试卷生成成功')
      await loadPapers()
    } else ElMessage.error(data?.message||'创建失败')
  } finally { creating.value = false }
}

async function regenerateCurrent(){
  if (!currentPaperId.value) { ElMessage.warning('请先在下方列表选择一个试卷'); return }
  if (creating.value) return
  creating.value = true
  try{
    const body = {
      strategy: strategy.value,
      typeScores: typeScores.value,
      difficulty: selectedDifficulty.value
    }
    const { data } = await http.post(`/papers/${currentPaperId.value}/assemble-type-scores`, body)
    if (data?.code===0){
      ElMessage.success('试卷已根据新策略重组')
      await loadPapers()
      await previewPaper(currentPaperId.value)
    } else ElMessage.error(data?.message||'生成失败')
  } finally { creating.value = false }
}

async function loadPapers(){
  const { data } = await http.get('/papers')
  papers.value = data?.data || []
}

async function previewPaper(id){
  const { data } = await http.get(`/papers/${id}/preview`)
  previewItems.value = (data?.data || []).map(q => ({ ...q, _score: q.score || 1 }))
  const currentPaper = papers.value.find(p => p.id === id)
  if(currentPaper) previewPaperName.value = currentPaper.name

  // 应用未保存的临时分值
  for (const q of previewItems.value) {
    if (pendingScores.value[q.id] !== undefined) q._score = pendingScores.value[q.id]
  }
  openPreview.value = true
}

function setCurrent(row){ currentPaperId.value = row.id }

// --- Score Editing ---
function onScoreChange(q){ pendingScores.value[q.id] = q._score }

async function saveScore(q){
  if (!currentPaperId.value) return
  const { data } = await http.put(`/papers/${currentPaperId.value}/question/${q.id}/score`, null, { params:{ score: q._score } })
  if (data?.code===0){
    ElMessage.success('分值已保存')
    delete pendingScores.value[q.id]
    await loadPapers()
  } else ElMessage.error('保存失败')
}

async function saveAllScores(){
  if (!currentPaperId.value) return
  const ids = Object.keys(pendingScores.value)
  if(ids.length===0) return
  for (const id of ids) {
    const score = pendingScores.value[id]
    await http.put(`/papers/${currentPaperId.value}/question/${id}/score`, null, { params:{ score } })
  }
  ElMessage.success('全部分值已更新')
  pendingScores.value = {}
  await loadPapers()
}

async function deletePaper(id){
  try {
    await ElMessageBox.confirm('确定要删除该试卷吗？', '警告', { type:'warning' })
  const { data } = await http.delete(`/papers/${id}`)
    if (data?.code===0){
      ElMessage.success('已删除')
      if (currentPaperId.value===id) currentPaperId.value=null
      await loadPapers()
}
  } catch {}
}
function editPaper(id){
  currentPaperId.value = id
  ElMessage.info('已选中，请在上方调整策略')
}

// --- Lifecycle ---
onMounted(async () => {
  await loadPapers()
  // 如果路由参数中有 paperId，自动设置为当前试卷
  if (route.query.paperId) {
    const paperId = Number(route.query.paperId)
    const paper = papers.value.find(p => p.id === paperId)
    if (paper) {
      currentPaperId.value = paperId
      // 加载试卷预览
      await previewPaper(paperId)
    } else {
      ElMessage.warning('未找到指定的试卷')
    }
  }
})
</script>

<style scoped lang="scss">
/* 页面容器 */
.page-container {
  padding: 24px;
  background-color: #f6f8f9;
  min-height: 100vh;
}

.page-header {
  margin-bottom: 20px;
}

/* 核心大卡片 */
.main-card {
  border-radius: 12px;
  border: none;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
  margin-bottom: 24px;
  overflow: visible;

  :deep(.el-card__body) {
    padding: 24px;
  }
}

.config-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  .header-left {
    display: flex;
    align-items: center;
    gap: 8px;

    .header-icon-box {
      font-size: 20px;
      color: #409eff;
      background: rgba(64,158,255,0.1);
      padding: 6px;
      border-radius: 6px;
      display: flex;
    }
    .title-text {
      font-size: 18px;
      font-weight: 600;
      color: #1f2f3d;
    }
  }

  .score-badge {
    background: #ecf5ff;
    border: 1px solid #d9ecff;
    padding: 6px 16px;
    border-radius: 50px;
    color: #409eff;
    display: flex;
    align-items: baseline;
    gap: 6px;

    .label { font-size: 13px; color: #5f738c; }
    .value { font-size: 24px; font-weight: bold; line-height: 1; }
    .unit { font-size: 12px; }
  }
}

/* 基础表单区 */
.basic-form-area {
  margin-bottom: 24px;

  .input-group {
    display: flex;
    flex-direction: column;
    gap: 8px;

    .input-label {
      font-size: 13px;
      font-weight: 500;
      color: #606266;
    }
  }

  .action-col {
    display: flex;
    align-items: flex-end;
    gap: 12px;
    height: 100%;
    padding-bottom: 2px;
  }

  .current-hint {
    margin-top: 16px;
  }
}

.divider-text {
  font-size: 13px;
  color: #909399;
}

/* 策略卡片网格 - 核心视觉点 */
.strategy-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.strategy-card {
  background: white;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  padding: 16px;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  position: relative;
  overflow: hidden;

  /* 顶部装饰条 */
  &::before {
    content: '';
    position: absolute;
    top: 0; left: 0; right: 0;
    height: 4px;
    background: #ccc;
  }

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 20px rgba(0,0,0,0.06);
  }

  .card-top {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 16px;

    .icon-box {
      width: 32px; height: 32px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 16px;
    }
    .card-title {
      font-weight: 600;
      font-size: 15px;
      color: #303133;
    }
  }

  /* 颜色变体 */
  &.card-blue {
    &::before { background: #409eff; }
    .icon-box { background: rgba(64,158,255,0.1); color: #409eff; }
  }
  &.card-green {
    &::before { background: #67c23a; }
    .icon-box { background: rgba(103,194,58,0.1); color: #67c23a; }
  }
  &.card-orange {
    &::before { background: #e6a23c; }
    .icon-box { background: rgba(230,162,60,0.1); color: #e6a23c; }
  }
  &.card-purple {
    &::before { background: #a0cfff; }
    .icon-box { background: #f0f5ff; color: #79bbff; }
  }
  &.card-red {
    &::before { background: #f56c6c; }
    .icon-box { background: rgba(245,108,108,0.1); color: #f56c6c; }
  }

  .card-body {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .control-row {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .sub-label {
      font-size: 13px;
      color: #606266;
    }
  }
}

/* 列表区域 */
.list-card {
  border-radius: 8px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .list-title {
    font-size: 16px;
    font-weight: 600;
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.index-badge {
  display: inline-block;
  width: 24px; height: 24px;
  line-height: 24px;
  background: #f0f2f5;
  color: #909399;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
}

.score-cell {
  font-family: 'DIN Alternate', sans-serif;
  font-weight: bold;
  color: #409eff;
}

/* 预览弹窗 */
.preview-dialog {
  :deep(.el-dialog__body) {
    padding: 0;
  }
}

.preview-content {
  background: #f5f7fa;
  padding: 24px;
  height: 60vh;
  overflow-y: auto;
}

.paper-header {
  text-align: center;
  margin-bottom: 24px;

  h2 { margin: 0 0 12px 0; color: #303133; }
  .paper-meta {
    display: inline-flex;
    align-items: center;
    background: white;
    padding: 8px 24px;
    border-radius: 50px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.05);
    color: #606266;
    font-size: 14px;
  }
}

.q-item {
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);

  .q-header-row {
    display: flex;
    align-items: center;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid #f0f2f5;

    .q-idx { font-weight: bold; font-size: 16px; margin-right: 8px; }
    .q-score-edit { margin-left: auto; display: flex; align-items: center; font-size: 13px; color: #606266; }
  }

  .q-content {
    .stem { font-size: 15px; line-height: 1.6; color: #303133; margin-bottom: 16px; }

    .options-area {
      padding-left: 8px;
      .opt-row { margin-bottom: 8px; display: flex; }
      .opt-letter { font-weight: bold; width: 24px; color: #409eff; }
      .opt-text { color: #555; }
    }

    .answer-section {
      margin-top: 16px;
      background: #fdf6ec;
      border: 1px solid #faecd8;
      border-radius: 4px;
      padding: 12px;
      font-size: 13px;

      .ans-label { font-weight: bold; color: #e6a23c; }
      .ans-val { color: #606266; margin-left: 4px; }
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 10px;
}
</style>