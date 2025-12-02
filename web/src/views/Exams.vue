<template>
  <div style="padding:24px;">
    <el-page-header content="考试与监考" @back="$router.back()" />
    <el-card style="margin-top:16px; max-width:720px;">
      <el-form label-width="100px">
        <el-form-item label="选择试卷">
          <el-select v-model="paperId" placeholder="请选择试卷" style="width:360px;" @change="onPaperChange">
            <el-option v-for="p in papers" :key="p.id" :label="p.name + '（' + (p.totalScore||0) + '分，'+ (p.durationMin||0) +'分钟）'" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="考试时间">
          <el-date-picker v-model="dateRange" type="datetimerange" range-separator="至"
                          start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="监考会话ID">
          <el-input v-model="proctorSid" placeholder="可手动填写会话ID" style="width:360px;" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="creating" @click="createSession">创建会话并进入考试</el-button>
          <el-button type="warning" :disabled="!(sessionId||proctorSid)" @click="openProctor">打开监考</el-button>
          <el-button @click="goRecords">我的考试记录</el-button>
        </el-form-item>
        <div v-if="sessionId">会话：{{ sessionId }}</div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import router from '../router'
import { ElMessage } from 'element-plus'
import http from '../api/http'
const paperId = ref(null)
const dateRange = ref([])
const creating = ref(false)
const sessionId = ref(null)
const papers = ref([])
const durationMin = ref(60)
const proctorSid = ref('')
async function loadPapers(){
  const { data } = await http.get('/papers')
  papers.value = data?.data || []
}
function onPaperChange(id){
  const p = papers.value.find(x=>x.id===id)
  durationMin.value = p?.durationMin || 60
}
async function createSession(){
  if (!dateRange.value || dateRange.value.length < 2) { ElMessage.warning('请选择考试时间'); return }
  if (!paperId.value) { ElMessage.warning('请选择试卷'); return }
  const [startAt, endAt] = dateRange.value
  creating.value = true
  try{
    const { data } = await http.post('/exams/sessions', { paperId: paperId.value, startAt, endAt })
    sessionId.value = data?.data?.sessionId || null
    ElMessage.success('已创建考试会话，正在进入考试')
    startExam()
  } catch(e){ ElMessage.error('创建失败') } finally { creating.value = false }
}

function startExam(){
  if (!sessionId.value) return
  // 跳转到学生考试页
  location.href = `/exam/take?sessionId=${sessionId.value}&paperId=${paperId.value}&duration=${durationMin.value}`
}

function openProctor(){
  const sid = proctorSid.value || sessionId.value
  if (!sid) { ElMessage.warning('请填写会话ID或先创建会话'); return }
  router.push(`/proctor?sessionId=${sid}`)
}
function goRecords(){ location.href = '/records' }

onMounted(loadPapers)
</script>
