<template>
  <div style="padding:24px; text-align:center;">
    <el-result icon="success" title="提交成功" sub-title="试卷已提交，祝您取得好成绩">
      <template #extra>
        <el-space>
          <el-button type="primary" @click="goHome">返回首页</el-button>
          <el-button @click="goExams">继续查看考试</el-button>
        </el-space>
      </template>
    </el-result>
    <div style="margin-top:16px; color:#909399;">会话：{{ sessionId }}</div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import router from '../router'
const sessionId = ref(new URLSearchParams(location.search).get('sessionId'))
const paperId = ref(new URLSearchParams(location.search).get('paperId'))
function goHome(){ router.push('/') }
function goExams(){ router.push('/exams') }
</script>

