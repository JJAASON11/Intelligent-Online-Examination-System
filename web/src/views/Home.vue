<template>
  <div style="padding:24px;">
    <h2>欢迎使用智能在线考试系统</h2>
    <div style="margin-top:8px; display:flex; gap:8px; align-items:center;">
      <el-button size="small" @click="ping">连接检测</el-button>
      <el-tag :type="pingOk?'success':'danger'">{{ pingOk ? '后端可用' : '不可用/未登录' }}</el-tag>
    </div>
    <el-row :gutter="16" style="margin-top:16px;">
      <el-col :span="6" v-if="can(['ADMIN','TEACHER'])">
        <el-card class="nav-card" @click="$router.push('/banks')">题库管理</el-card>
      </el-col>
      <el-col :span="6" v-if="can(['ADMIN','TEACHER'])">
        <el-card class="nav-card" @click="$router.push('/assemble')">自动组卷</el-card>
      </el-col>
      <el-col :span="6" v-if="can(['ADMIN','TEACHER','STUDENT'])">
        <el-card class="nav-card" @click="$router.push('/exams')">考试与监考</el-card>
      </el-col>
      <el-col :span="6" v-if="can(['ADMIN','TEACHER'])">
        <el-card class="nav-card" @click="$router.push('/grading')">智能阅卷</el-card>
      </el-col>
    </el-row>
  </div>
  </template>

<script setup>
import { computed, ref } from 'vue'
import http from '../api/http'
const roles = computed(()=> JSON.parse(localStorage.getItem('roles')||'[]'))
const can = (rs)=> roles.value.some(r=>rs.includes(r))
const pingOk = ref(false)
async function ping(){ try{ const { data } = await http.get('/health'); pingOk.value = !!data } catch{ pingOk.value = false } }
</script>

<style>
.nav-card{ cursor:pointer; }
.nav-card:hover{ box-shadow: 0 2px 12px rgba(0,0,0,0.1); }
</style>
