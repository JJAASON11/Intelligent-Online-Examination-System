<template>
  <div class="page-container">
    <el-page-header content="题库管理" @back="$router.back()" title="返回" class="page-header" />

    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar-content">
        <div class="left">
          <el-button type="primary" :icon="Refresh" @click="load" :loading="loading">刷新列表</el-button>
          <el-button type="success" :icon="Plus" @click="openCreate=true">新建题目</el-button>
        </div>
        <div class="right">
          <div class="search-filters">
            <el-input 
              v-model="searchKeyword" 
              placeholder="搜索题干关键词..." 
              style="width: 240px" 
              clearable 
              @clear="handleSearch"
              @keyup.enter="handleSearch">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
            <el-select 
              v-model="searchType" 
              placeholder="全部题型" 
              clearable 
              style="width: 140px; margin-left: 12px;"
              @change="handleSearch">
              <el-option 
                v-for="(label, value) in QUESTION_TYPES" 
                :key="value" 
                :label="label" 
                :value="value" />
            </el-select>
            <el-select 
              v-model="searchDifficulty" 
              placeholder="全部难度" 
              clearable 
              style="width: 140px; margin-left: 12px;"
              @change="handleSearch">
              <el-option label="简单" :value="1" />
              <el-option label="较易" :value="2" />
              <el-option label="中等" :value="3" />
              <el-option label="较难" :value="4" />
              <el-option label="困难" :value="5" />
            </el-select>
            <el-button 
              type="primary" 
              :icon="Search" 
              style="margin-left: 12px;"
              @click="handleSearch">
              搜索
            </el-button>
            <el-button 
              v-if="hasActiveFilters"
              type="info" 
              plain 
              style="margin-left: 8px;"
              @click="clearFilters">
              清除筛选
            </el-button>
          </div>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-empty v-if="items.length===0 && !loading" description="暂无题目数据" />

      <el-table v-else :data="items" style="width:100%" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" align="center" />

        <el-table-column prop="type" label="题型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getTypeTag(row.type)" effect="light" disable-transitions>
              {{ QUESTION_TYPES[row.type] || row.type }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="difficulty" label="难度" width="160" align="center">
          <template #default="{ row }">
            <el-rate v-model="row.difficulty" disabled text-color="#ff9900" score-template="{value}" />
          </template>
        </el-table-column>

        <el-table-column prop="stem" label="题干内容" min-width="300" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="stem-text">{{ row.stem }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" :icon="View" @click="viewQuestion(row)">查看</el-button>
            <el-popconfirm title="确定要删除这道题目吗？" @confirm="deleteQuestion(row.id)">
              <template #reference>
                <el-button size="small" type="danger" :icon="Delete" plain>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
            background
            layout="total, prev, pager, next, sizes"
            :total="total"
            :page-sizes="[10, 20, 50]"
            v-model:page-size="size"
            v-model:current-page="page"
            @current-change="onPageChange"
            @size-change="load"
        />
      </div>
    </el-card>

    <el-dialog v-model="openCreate" title="新建题目" width="600px" top="5vh" :close-on-click-modal="false">
      <el-form label-position="top" size="large">
        <el-form-item label="题干内容">
          <el-input v-model="form.stem" type="textarea" :rows="3" placeholder="请输入题目描述..." />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="题目类型">
              <el-select v-model="form.type" style="width: 100%;">
                <el-option v-for="(label, value) in QUESTION_TYPES" :key="value" :label="label" :value="value">
                  <span style="float: left">{{ label }}</span>
                  <span style="float: right; color: #8492a6; font-size: 13px">{{ value }}</span>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="难度等级">
              <el-rate v-model="form.difficulty" show-text :texts="['简单', '较易', '中等', '较难', '困难']" style="margin-top: 8px;" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">答案与选项配置</el-divider>

        <div v-if="form.type==='SINGLE' || form.type==='MULTI'" class="options-area">
          <div v-for="(opt, idx) in options" :key="idx" class="option-row">
            <div class="opt-label">{{ String.fromCharCode(65+idx) }}</div>
            <el-input v-model="opt.text" placeholder="输入选项内容" />
            <el-button type="danger" :icon="Delete" circle plain @click="removeOption(idx)" :disabled="options.length<=2" />
          </div>
          <div class="opt-actions">
            <el-button type="primary" link :icon="Plus" @click="addOption">添加选项</el-button>
          </div>

          <el-form-item label="正确答案" style="margin-top: 16px;">
            <el-radio-group v-if="form.type==='SINGLE'" v-model="correctSingle">
              <el-radio-button v-for="(opt, idx) in options" :key="'r'+idx" :label="idx">{{ String.fromCharCode(65+idx) }}</el-radio-button>
            </el-radio-group>
            <el-checkbox-group v-else v-model="correctMulti">
              <el-checkbox-button v-for="(opt, idx) in options" :key="'c'+idx" :label="idx">{{ String.fromCharCode(65+idx) }}</el-checkbox-button>
            </el-checkbox-group>
          </el-form-item>
        </div>

        <div v-if="form.type==='JUDGE'" class="judge-area">
          <el-form-item label="设定正确答案">
            <el-radio-group v-model="judgeAnswer" size="large">
              <el-radio border label="TRUE">正确 (True)</el-radio>
              <el-radio border label="FALSE">错误 (False)</el-radio>
            </el-radio-group>
          </el-form-item>
        </div>

        <div v-if="form.type==='FILL'" class="fill-area">
          <div v-for="(ans, idx) in fillAnswers" :key="'f'+idx" class="fill-row">
            <span class="fill-label">第 {{ idx + 1 }} 空：</span>
            <el-input v-model="fillAnswers[idx]" placeholder="请输入该空的标准答案" />
            <el-button type="danger" :icon="Delete" circle plain @click="removeFill(idx)" :disabled="fillAnswers.length<=1" />
          </div>
          <el-button type="primary" link :icon="Plus" @click="addFill">增加填空项</el-button>
        </div>

        <div v-if="form.type==='SHORT'">
          <el-form-item label="参考解析/答案">
            <el-input type="textarea" :rows="4" v-model="shortAnswer" placeholder="请输入参考答案或评分标准" />
          </el-form-item>
        </div>

      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="openCreate=false">取消</el-button>
          <el-button type="primary" :loading="creating" @click="create">立即创建</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="openView" title="题目详情预览" width="700px" custom-class="paper-preview-dialog">
      <div class="preview-card">
        <div class="preview-meta">
          <el-tag :type="getTypeTag(view?.type)">{{ QUESTION_TYPES[view?.type] || view?.type }}</el-tag>
          <el-divider direction="vertical" />
          <el-rate :model-value="view?.difficulty || 0" disabled text-color="#ff9900" size="small" />
          <span class="meta-text">难度系数</span>
        </div>

        <div class="preview-stem">
          <span class="q-seq">Q.</span>
          {{ view?.stem }}
        </div>

        <div class="preview-body">
          <div v-if="view?.type==='SINGLE' || view?.type==='MULTI'" class="options-list">
            <div v-for="(opt, idx) in viewOptions" :key="'vo'+idx"
                 :class="['option-item', isCorrect(idx) && showAnswer ? 'is-answer' : '']">
              <span class="opt-idx">{{ String.fromCharCode(65+idx) }}.</span>
              <span class="opt-content">{{ opt }}</span>
              <el-icon v-if="isCorrect(idx) && showAnswer" class="check-icon"><Check /></el-icon>
            </div>
          </div>

          <div v-else-if="view?.type==='JUDGE'" class="judge-block">
            <el-tag :type="judgeTrue ? 'success' : 'danger'" effect="dark" size="large">
              答案：{{ judgeTrue ? '正确' : '错误' }}
            </el-tag>
          </div>

          <div v-else-if="view?.type==='FILL'" class="fill-block">
            <div v-for="(ans, idx) in fillView" :key="idx" class="fill-item">
              <span class="label">空 {{idx+1}}:</span>
              <span class="val">{{ showAnswer ? ans : '_____' }}</span>
            </div>
          </div>

          <div v-else class="text-block">
            <div class="label">参考答案：</div>
            <div class="content">{{ showAnswer ? (shortView || codeView) : '点击下方按钮显示答案' }}</div>
          </div>
        </div>

        <div class="preview-footer-ctrl">
          <el-switch v-model="showAnswer" active-text="显示答案与解析" inactive-text="隐藏" inline-prompt />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import http from '../api/http'
import { QUESTION_TYPES } from '../utils/constants'
import { Plus, Refresh, Delete, View, Search, Check } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const items = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)
const searchKeyword = ref('')
const searchType = ref('')
const searchDifficulty = ref(null)

const openCreate = ref(false)
const creating = ref(false)
const form = ref({ stem:'', type:'SINGLE', difficulty:3 })

// 选项与答案状态
const options = ref([{text:''},{text:''},{text:''},{text:''}])
const correctSingle = ref(0)
const correctMulti = ref([])
const judgeAnswer = ref('TRUE')
const fillAnswers = ref([''])
const shortAnswer = ref('')

// 获取标签颜色
function getTypeTag(type) {
  const map = { SINGLE: '', MULTI: 'success', JUDGE: 'warning', FILL: 'info', SHORT: 'danger' }
  return map[type] || ''
}

function addOption(){ options.value.push({text:''}) }
function removeOption(i){ options.value.splice(i,1) }
function addFill(){ fillAnswers.value.push('') }
function removeFill(i){ fillAnswers.value.splice(i,1) }

watch(()=>form.value.type, (t)=>{
  // 切换类型时重置特定状态，保留通用状态
  if (t==='SINGLE'){ correctSingle.value = 0 }
  if (t==='MULTI'){ correctMulti.value = [] }
  if (t==='JUDGE'){ judgeAnswer.value = 'TRUE' }
  if (t==='FILL'){ fillAnswers.value = [''] }
  if (t==='SHORT'){ shortAnswer.value = '' }
})

// 检查是否有激活的筛选条件
const hasActiveFilters = computed(() => {
  return !!(searchKeyword.value || searchType.value || searchDifficulty.value)
})

// 搜索处理
function handleSearch() {
  page.value = 1 // 重置到第一页
  load()
}

// 清除所有筛选
function clearFilters() {
  searchKeyword.value = ''
  searchType.value = ''
  searchDifficulty.value = null
  handleSearch()
}

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if(searchKeyword.value) params.keyword = searchKeyword.value
    if(searchType.value) params.type = searchType.value
    if(searchDifficulty.value) params.difficulty = searchDifficulty.value
    const {data} = await http.get('/questions', { params })
    items.value = data?.data?.records || []
    total.value = Number(data?.data?.total || 0)
  } finally {
    loading.value = false
  }
}
function onPageChange(p){ page.value = p; load() }

async function create() {
  if(!form.value.stem) return ElMessage.warning('请输入题干内容')

  creating.value = true
  try {
    const payload = {
      stem: form.value.stem,
      type: form.value.type,
      difficulty: form.value.difficulty
    }
    // 组装选项与答案 JSON
    if (form.value.type==='SINGLE'){
      payload.optionsJson = JSON.stringify(options.value.map(o=>o.text))
      payload.answerJson = JSON.stringify({index: correctSingle.value})
    } else if (form.value.type==='MULTI'){
      payload.optionsJson = JSON.stringify(options.value.map(o=>o.text))
      payload.answerJson = JSON.stringify({indexes: correctMulti.value})
    } else if (form.value.type==='JUDGE'){
      payload.optionsJson = JSON.stringify(['正确','错误'])
      payload.answerJson = JSON.stringify({value: judgeAnswer.value==='TRUE'})
    } else if (form.value.type === 'FILL') {
      payload.answerJson = JSON.stringify(fillAnswers.value)
    } else if (form.value.type === 'SHORT') {
      payload.answerJson = JSON.stringify({text: shortAnswer.value})
    }
    await http.post('/questions', payload)
    ElMessage.success('题目创建成功')
    openCreate.value = false
    // 重置部分表单防止下次打开有残留
    form.value.stem = ''
    options.value = [{text: ''}, {text: ''}, {text: ''}, {text: ''}]
    await load()
  } finally {
    creating.value = false
  }
}

async function deleteQuestion(id) {
  try {
    await http.delete(`/questions/${id}`)
    ElMessage.success('已删除')
    await load()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

// 预览相关
const openView = ref(false)
const view = ref(null)
const viewOptions = ref([])
const correctIndexes = ref([])
const judgeTrue = ref(true)
const fillView = ref([])
const shortView = ref('')
const codeView = ref('')
const showAnswer = ref(true)

function safeParse(s) {
  try {
    return s ? JSON.parse(s) : null
  } catch (e) {
    return null
  }
}

function viewQuestion(row) {
  view.value = row
  viewOptions.value = []
  correctIndexes.value = []
  judgeTrue.value = true
  fillView.value = []
  shortView.value = ''
  codeView.value = ''
  const type = row.type
  const opts = safeParse(row.optionsJson)
  const ans = safeParse(row.answerJson)

  if (type === 'SINGLE') {
    viewOptions.value = Array.isArray(opts) ? opts : []
    correctIndexes.value = [ans?.index ?? -1]
  } else if (type === 'MULTI') {
    viewOptions.value = Array.isArray(opts) ? opts : []
    correctIndexes.value = Array.isArray(ans?.indexes) ? ans.indexes : []
  } else if (type === 'JUDGE') {
    judgeTrue.value = !!(ans?.value)
  } else if (type === 'FILL') {
    fillView.value = Array.isArray(ans) ? ans : []
  } else if (type === 'SHORT') {
    shortView.value = ans?.text || ''
  } else if (type === 'CODE') {
    codeView.value = ans?.code || ans?.text || ''
  }
  openView.value = true
}

function isCorrect(i) {
  return correctIndexes.value.includes(i)
}

load()
</script>

<style scoped>
.page-container {
  padding: 24px;
  background-color: #f6f8f9;
  min-height: 100vh;
}

.page-header {
  margin-bottom: 20px;
}

/* 顶部操作栏 */
.toolbar-card {
  margin-bottom: 16px;
  border: none;
  border-radius: 8px;
}

.toolbar-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.left {
  display: flex;
  gap: 12px;
}

.right {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.search-filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

/* 表格卡片 */
.table-card {
  border: none;
  border-radius: 8px;
}

.stem-text {
  font-weight: 500;
  color: #303133;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 新建题目弹窗样式 */
.options-area, .fill-area {
  background: #f8f9fa;
  padding: 16px;
  border-radius: 6px;
  border: 1px dashed #dcdfe6;
}

.option-row, .fill-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.opt-label {
  font-weight: bold;
  color: #909399;
  width: 24px;
  text-align: center;
}

.fill-label {
  width: 80px;
  font-size: 13px;
  color: #606266;
}

/* 预览卡片样式 */
.preview-card {
  background: #fff;
  padding: 8px;
}

.preview-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  background: #fdf6ec;
  padding: 8px 16px;
  border-radius: 4px;
}

.meta-text {
  font-size: 12px;
  color: #e6a23c;
}

.preview-stem {
  font-size: 16px;
  line-height: 1.6;
  color: #303133;
  margin-bottom: 20px;
  font-weight: 600;
}

.q-seq {
  font-size: 18px;
  color: #409eff;
  margin-right: 8px;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-item {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  background: #f5f7fa;
  border-radius: 6px;
  border: 1px solid transparent;
  transition: all 0.3s;
}

.option-item.is-answer {
  background: #f0f9eb;
  border-color: #67c23a;
  color: #67c23a;
}

.opt-idx {
  font-weight: bold;
  margin-right: 12px;
}

.check-icon {
  margin-left: auto;
  font-size: 18px;
}

.judge-block, .fill-block, .text-block {
  margin-top: 16px;
  padding: 16px;
  background: #f9fafc;
  border-radius: 6px;
}

.fill-item {
  margin-bottom: 8px;
  font-size: 14px;
}

.fill-item .val {
  font-weight: bold;
  border-bottom: 1px solid #409eff;
  padding: 0 8px;
}

.preview-footer-ctrl {
  margin-top: 24px;
  border-top: 1px solid #ebeef5;
  padding-top: 16px;
  text-align: right;
}
</style>