<template>
  <div style="padding:24px;">
    <el-page-header content="题库管理" @back="$router.back()" />
    <el-space style="margin:12px 0;">
      <el-button type="primary" @click="load">刷新列表</el-button>
      <el-button @click="openCreate=true">新建题目</el-button>
    </el-space>
    <el-empty v-if="items.length===0" description="暂无题目" />
    <el-table v-else :data="items" style="width:100%">
      <el-table-column prop="id" label="ID" width="80" />
      <!-- 整合题型列 -->
      <el-table-column prop="type" label="题型" width="100">
        <template #default="{ row }">
          <el-tag>{{ QUESTION_TYPES[row.type] || row.type }}</el-tag>
        </template>
      </el-table-column>
      
      <!-- 整合难度列 -->
      <el-table-column prop="difficulty" label="难度" width="100">
        <template #default="{ row }">
          <el-rate v-model="row.difficulty" disabled text-color="#ff9900"/>
        </template>
      </el-table-column>
      <el-table-column prop="stem" label="题干" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button size="small" @click="viewQuestion(row)">查看</el-button>
          <el-button size="small" type="danger" @click="deleteQuestion(row.id)">删除</el-button>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button size="small" @click="viewQuestion(row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div style="margin-top:12px; text-align:right;">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="size"
        :current-page="page"
        @current-change="onPageChange"
      />
    </div>

    <el-dialog v-model="openCreate" title="新建题目" width="500px">
      <el-form>
        <el-form-item label="题干"><el-input v-model="form.stem" /></el-form-item>
        <el-form-item label="题型">
          <!-- 整合题型选择器 -->
          <el-select v-model="form.type">
            <el-option
                v-for="(label, value) in QUESTION_TYPES"
                :key="value"
                :label="label"
                :value="value"
            />
          </el-select>
        </el-form-item>
        <!-- 单选/多选 选项与答案 -->
        <template v-if="form.type==='SINGLE' || form.type==='MULTI'">
          <el-form-item label="选项">
            <div style="width:100%">
              <div v-for="(opt, idx) in options" :key="idx" style="display:flex; gap:8px; margin-bottom:8px;">
                <el-input v-model="opt.text" placeholder="选项内容" />
                <el-button @click="removeOption(idx)" :disabled="options.length<=2">删</el-button>
              </div>
              <el-button @click="addOption">添加选项</el-button>
            </div>
          </el-form-item>
          <el-form-item label="正确答案">
            <el-radio-group v-if="form.type==='SINGLE'" v-model="correctSingle">
              <el-radio v-for="(opt, idx) in options" :key="'r'+idx" :label="idx">{{ String.fromCharCode(65+idx) }}</el-radio>
            </el-radio-group>
            <el-checkbox-group v-else v-model="correctMulti">
              <el-checkbox v-for="(opt, idx) in options" :key="'c'+idx" :label="idx">{{ String.fromCharCode(65+idx) }}</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
        </template>
        <!-- 判断题答案 -->
        <template v-if="form.type==='JUDGE'">
          <el-form-item label="正确答案">
            <el-radio-group v-model="judgeAnswer">
              <el-radio label="TRUE">正确</el-radio>
              <el-radio label="FALSE">错误</el-radio>
            </el-radio-group>
          </el-form-item>
        </template>
        <!-- 填空题答案 -->
        <template v-if="form.type==='FILL'">
          <el-form-item label="填空答案">
            <div style="width:100%">
              <div v-for="(ans, idx) in fillAnswers" :key="'f'+idx" style="display:flex; gap:8px; margin-bottom:8px;">
                <el-input v-model="fillAnswers[idx]" placeholder="答案" />
                <el-button @click="removeFill(idx)" :disabled="fillAnswers.length<=1">删</el-button>
              </div>
              <el-button @click="addFill">添加空</el-button>
            </div>
          </el-form-item>
        </template>
        <!-- 简答题参考答案 -->
        <template v-if="form.type==='SHORT'">
          <el-form-item label="参考答案"><el-input type="textarea" v-model="shortAnswer" /></el-form-item>
        </template>
        
        <el-form-item label="难度"><el-input-number v-model="form.difficulty" :min="1" :max="5" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="openCreate=false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="create">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="openView" title="题目预览（含答案）" width="720px">
      <div style="margin-bottom:12px; display:flex; justify-content:space-between; align-items:center;">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="ID">{{ view?.id }}</el-descriptions-item>
          <el-descriptions-item label="题型">
            <el-tag>{{ QUESTION_TYPES[view?.type] || view?.type }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="分值">{{ view?.score }}</el-descriptions-item>
          <el-descriptions-item label="难度">
            <el-rate :model-value="view?.difficulty || 0" disabled text-color="#ff9900" />
          </el-descriptions-item>
        </el-descriptions>
        <el-switch v-model="showAnswer" active-text="显示答案" inactive-text="隐藏答案" />
      </div>
      <div style="margin-top:12px;">题干：{{ view?.stem }}</div>

      <div v-if="view?.type==='SINGLE' || view?.type==='MULTI'" style="margin-top:12px;">
        <el-radio-group v-if="view?.type==='SINGLE'" :model-value="-1">
          <el-radio v-for="(opt, idx) in viewOptions" :key="'vo'+idx" :label="idx" disabled>
            <span style="margin-left:8px;">{{ String.fromCharCode(65+idx) }}. {{ opt }}</span>
          </el-radio>
        </el-radio-group>
        <el-checkbox-group v-else :model-value="[]" disabled>
          <el-checkbox v-for="(opt, idx) in viewOptions" :key="'vo'+idx" :label="idx">
            <span style="margin-left:8px;">{{ String.fromCharCode(65+idx) }}. {{ opt }}</span>
          </el-checkbox>
        </el-checkbox-group>
      </div>
      <div v-else-if="view?.type==='JUDGE'" style="margin-top:12px;">
        <el-radio-group :model-value="judgeTrue ? 'TRUE' : 'FALSE'" disabled>
          <el-radio label="TRUE">正确</el-radio>
          <el-radio label="FALSE">错误</el-radio>
        </el-radio-group>
      </div>
      <div v-else-if="view?.type==='FILL'" style="margin-top:12px;">
        <div v-for="(ans, idx) in fillView" :key="'fv'+idx" style="margin-bottom:8px;">
          <div style="margin-bottom:4px;">空{{ idx+1 }}</div>
          <el-input :model-value="''" disabled />
        </div>
      </div>
      <div v-else-if="view?.type==='SHORT'" style="margin-top:12px;">
        <el-input type="textarea" :model-value="''" disabled />
      </div>
      <div v-else-if="view?.type==='CODE'" style="margin-top:12px;">
        <el-card>
          <pre style="white-space:pre-wrap;">{{ codeView }}</pre>
        </el-card>
      </div>
      <div v-if="showAnswer" style="margin-top:16px;">
        <div style="font-weight:600; margin-bottom:8px;">答案</div>
        <div v-if="view?.type==='SINGLE'">正确选项：{{ correctIndexes.length ? String.fromCharCode(65+correctIndexes[0]) : '-' }}</div>
        <div v-else-if="view?.type==='MULTI'">正确选项：{{ correctIndexes.map(i=>String.fromCharCode(65+i)).join(', ') || '-' }}</div>
        <div v-else-if="view?.type==='JUDGE'">{{ judgeTrue ? '正确' : '错误' }}</div>
        <div v-else-if="view?.type==='FILL'">{{ fillView.join('；') || '-' }}</div>
        <div v-else-if="view?.type==='SHORT'">{{ shortView || '-' }}</div>
        <div v-else-if="view?.type==='CODE'">{{ codeView || '-' }}</div>
      </div>
      <template #footer>
        <el-button @click="openView=false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import http from '../api/http'
// 导入常量
import { QUESTION_TYPES } from '../utils/constants'

const items = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
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

function addOption(){ options.value.push({text:''}) }
function removeOption(i){ options.value.splice(i,1) }
function addFill(){ fillAnswers.value.push('') }
function removeFill(i){ fillAnswers.value.splice(i,1) }

watch(()=>form.value.type, (t)=>{
  if (t==='SINGLE'){ correctSingle.value = 0 }
  if (t==='MULTI'){ correctMulti.value = [] }
  if (t==='JUDGE'){ judgeAnswer.value = 'TRUE' }
  if (t==='FILL'){ fillAnswers.value = [''] }
  if (t==='SHORT'){ shortAnswer.value = '' }
})

async function load() {
  const {data} = await http.get('/questions', { params: { page: page.value, size: size.value } })
  items.value = data?.data?.records || []
  total.value = Number(data?.data?.total || 0)
}
function onPageChange(p){ page.value = p; load() }

async function create() {
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
    } else if (form.value.type==='FILL'){
      payload.answerJson = JSON.stringify(fillAnswers.value)
    } else if (form.value.type==='SHORT'){
      payload.answerJson = JSON.stringify({text: shortAnswer.value})
    }
    await http.post('/questions', payload)
    openCreate.value = false
    await load()
  } finally {
    creating.value = false
  }
}
async function deleteQuestion(id){
  await http.delete(`/questions/${id}`)
  await load()
}

const openView = ref(false)
const view = ref(null)
const viewOptions = ref([])
const correctIndexes = ref([])
const judgeTrue = ref(true)
const fillView = ref([])
const shortView = ref('')
const codeView = ref('')
const showAnswer = ref(true)
function safeParse(s){ try{ return s ? JSON.parse(s) : null }catch(e){ return null } }
function viewQuestion(row){
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
  if (type==='SINGLE'){
    viewOptions.value = Array.isArray(opts) ? opts : []
    correctIndexes.value = [ans?.index ?? -1]
  } else if (type==='MULTI'){
    viewOptions.value = Array.isArray(opts) ? opts : []
    correctIndexes.value = Array.isArray(ans?.indexes) ? ans.indexes : []
  } else if (type==='JUDGE'){
    judgeTrue.value = !!(ans?.value)
  } else if (type==='FILL'){
    fillView.value = Array.isArray(ans) ? ans : []
  } else if (type==='SHORT'){
    shortView.value = ans?.text || ''
  } else if (type==='CODE'){
    codeView.value = ans?.code || ans?.text || ''
  }
  openView.value = true
}
function isCorrect(i){ return correctIndexes.value.includes(i) }

load()
</script>
