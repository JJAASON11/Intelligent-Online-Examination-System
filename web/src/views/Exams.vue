<template>
  <div class="exam-proctor-container">
    <el-page-header content="考务管理" @back="$router.back()" class="page-header" />

    <el-card shadow="hover" class="action-card">
      <template #header>
        <div class="card-header">
          <span><el-icon><Calendar /></el-icon> 发布新考试</span>
          <el-button type="primary" link @click="resetForm">重置表单</el-button>
        </div>
      </template>

      <el-form
          ref="formRef"
          label-width="100px"
          size="large"
      >
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="选择试卷">
              <el-select
                  v-model="paperId"
                  placeholder="请选择试卷"
                  style="width: 100%;"
                  filterable
                  @change="onPaperChange"
              >
                <el-option
                    v-for="p in papers"
                    :key="p.id"
                    :label="p.name"
                    :value="p.id"
                >
                  <span style="float: left">{{ p.name }}</span>
                  <span style="float: right; color: #8492a6; font-size: 13px">
                    {{ p.totalScore }}分 / {{ p.durationMin }}分钟
                  </span>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="考试日期">
              <el-date-picker
                  v-model="examDate"
                  type="date"
                  placeholder="选择日期"
                  value-format="YYYY-MM-DD"
                  :disabled-date="disabledDate"
                  style="width: 100%;"
              />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="有效时段">
              <el-time-picker
                  v-model="timeRange"
                  is-range
                  range-separator="至"
                  start-placeholder="开始"
                  end-placeholder="结束"
                  value-format="HH:mm:ss"
                  style="width: 100%;"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="form-actions">
          <el-button
              type="primary"
              :loading="creating"
              icon="Plus"
              @click="createSession"
          >
            立即创建会话
          </el-button>

          <div class="manual-proctor">
            <el-input
                v-model="proctorSid"
                placeholder="输入会话ID进入监考"
                style="width: 200px; margin-right: 10px;"
            />
            <el-button
                type="warning"
                icon="Monitor"
                plain
                :disabled="!(sessionId||proctorSid)"
                @click="openProctor"
            >
              进入监考
            </el-button>
          </div>
        </div>

        <transition name="el-zoom-in-top">
          <el-alert
              v-if="sessionId"
              type="success"
              show-icon
              :closable="false"
              class="success-alert"
          >
            <template #title>
              考试会话创建成功！ID: <strong style="font-size: 16px;">{{ sessionId }}</strong>
            </template>
            <div class="alert-actions">
              <el-button link type="primary" icon="CopyDocument" @click="copySid">复制ID</el-button>
              <el-button link type="warning" icon="Monitor" @click="openProctor">立即监考</el-button>
            </div>
          </el-alert>
        </transition>
      </el-form>
    </el-card>

    <el-card shadow="never" class="list-card">
      <template #header>
        <div class="card-header">
          <span><el-icon><List /></el-icon> 考试会话记录</span>
          <div class="filter-area">
            <el-date-picker
                v-model="listDay"
                type="date"
                placeholder="筛选日期"
                value-format="YYYY-MM-DD"
                size="small"
                style="width: 140px"
            />
            <el-button icon="Refresh" circle size="small" @click="loadSessions" />
          </div>
        </div>
      </template>

      <el-table :data="sessions" stripe style="width: 100%" v-loading="loadingList">
        <el-table-column prop="id" label="会话ID" width="120" align="center" />

        <el-table-column label="试卷ID" width="100" prop="paperId" align="center" />

        <el-table-column label="考试时间窗口" min-width="240">
          <template #default="{ row }">
            <div class="time-cell">
              <span>{{ row.startAt }}</span>
              <span class="separator">至</span>
              <span>{{ row.endAt ? row.endAt.split(' ')[1] : '' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row)" effect="light">
              {{ row.status || '未知' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain icon="Monitor" @click="openProctorWith(row)">
              监考
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import router from '../router'
import { ElMessage } from 'element-plus'
import { Calendar, List, Plus, Monitor, Refresh, CopyDocument } from '@element-plus/icons-vue'
import http from '../api/http'

// 保持原有变量名不变
const paperId = ref(null)
const examDate = ref('')
const timeRange = ref([])
const creating = ref(false)
const sessionId = ref(null)
const papers = ref([])
const durationMin = ref(60)
const proctorSid = ref('')
const sessions = ref([])
const listDay = ref(new Date().toISOString().slice(0,10))
const loadingList = ref(false) // 新增：列表加载状态

// 原有逻辑保持不变
function disabledDate(d){
  const now = Date.now()
  const t = new Date(d).getTime()
  return t < now - 24*3600*1000 || t > now + 30*24*3600*1000
}

async function loadPapers(){
  const { data } = await http.get('/papers')
  papers.value = data?.data || []
}

function onPaperChange(id){
  const p = papers.value.find(x=>x.id===id)
  durationMin.value = p?.durationMin || 60
}

async function createSession(){
  if (!examDate.value) { ElMessage.warning('请选择考试日期'); return }
  if (!timeRange.value || timeRange.value.length < 2) { ElMessage.warning('请选择考试时间段'); return }
  if (!paperId.value) { ElMessage.warning('请选择试卷'); return }

  const [startT, endT] = timeRange.value
  const startAt = `${examDate.value} ${startT}`
  const endAt = `${examDate.value} ${endT}`

  const diff = new Date(endAt).getTime() - new Date(startAt).getTime()
  if (diff <= 0) { ElMessage.error('结束时间必须晚于开始时间'); return }
  if (diff > 24*3600*1000) { ElMessage.error('考试窗口不能超过1天'); return }

  creating.value = true
  // 创建前清空之前的sessionId，触发动画
  sessionId.value = null
  proctorSid.value = ''

  try{
    const { data } = await http.post('/exams/sessions', { paperId: paperId.value, startAt, endAt })
    sessionId.value = data?.data?.sessionId || null
    proctorSid.value = sessionId.value
    ElMessage.success('已创建考试会话')
    // 创建成功后自动刷新列表
    loadSessions()
  } catch(e){ ElMessage.error('创建失败') } finally { creating.value = false }
}

function openProctor(){
  const sid = proctorSid.value || sessionId.value
  if (!sid) { ElMessage.warning('请填写会话ID或先创建会话'); return }
  router.push(`/proctor?sessionId=${sid}`)
}

// 辅助功能：重置表单（不影响原有逻辑，仅清空UI）
function resetForm() {
  paperId.value = null
  examDate.value = ''
  timeRange.value = []
  sessionId.value = null
  proctorSid.value = ''
}

// 辅助功能：状态颜色
function getStatusType(row) {
  // 这里简单根据文本返回颜色，实际可以根据业务逻辑调整
  if (row.status === 'FINISHED') return 'info'
  if (row.status === 'RUNNING') return 'success'
  return 'primary'
}

function goRecords(){ location.href = '/records' } // 注意：尽量使用router.push而不是location.href

async function copySid(){
  try{ await navigator.clipboard.writeText(String(sessionId.value||'')); ElMessage.success('已复制会话ID') }
  catch{ ElMessage.error('复制失败') }
}

function openProctorWith(row){ router.push(`/proctor?sessionId=${row.id}`) }

async function loadSessions(){
  loadingList.value = true
  try{
    const { data } = await http.get('/exams/sessions')
    let arr = data?.data || []
    if (paperId.value) arr = arr.filter(x=> x.paperId===paperId.value)
    if (listDay.value) {
      arr = arr.filter(x=> String(x.startAt||'').slice(0,10)===listDay.value || String(x.endAt||'').slice(0,10)===listDay.value)
    }
    sessions.value = arr
  } catch(e){ sessions.value = [] }
  finally { loadingList.value = false }
}

onMounted(()=>{ loadPapers(); loadSessions() })
</script>

<style scoped>
.exam-proctor-container {
  padding: 24px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.page-header {
  margin-bottom: 24px;
}

.action-card {
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}
.card-header .el-icon {
  vertical-align: middle;
  margin-right: 6px;
}

.form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
  padding-top: 20px;
  border-top: 1px dashed #ebeef5;
}

.manual-proctor {
  display: flex;
  align-items: center;
}

.success-alert {
  margin-top: 20px;
}
.alert-actions {
  margin-top: 8px;
}

.list-card {
  /* box-shadow: none; */
}

.filter-area {
  display: flex;
  gap: 12px;
  align-items: center;
}

.time-cell {
  color: #606266;
}
.separator {
  margin: 0 8px;
  color: #909399;
  font-size: 12px;
}
</style>