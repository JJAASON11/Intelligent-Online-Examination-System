<template>
  <div class="login-wrapper">
    <div class="login-box">
      <div class="login-header">
        <div class="logo">🎓</div>
        <h2>智能在线考试系统</h2>
        <p>Intelligent Online Exam System</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent>

        <el-form-item prop="username">
          <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              show-password
              :prefix-icon="Lock"
              @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item class="role-group">
          <el-radio-group v-model="form.role" class="custom-radio">
            <el-radio :label="3">我是学生</el-radio>
            <el-radio :label="2">我是教师</el-radio>
            <el-radio :label="1">管理员</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-button
            type="primary"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
        >
          {{ loading ? '登 录 中...' : '立 即 登 录' }}
        </el-button>

        <div class="login-footer">
          <span>后端地址: http://localhost:8080</span>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue' // 引入图标
import http from '../api/http'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const formRef = ref(null)

const form = reactive({
  username: '',
  password: '',
  role: 3 // 默认选择学生
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        loading.value = true
        // 发送请求
        const { data } = await http.post('/auth/login', { username: form.username, password: form.password })

        if (data && data.code === 0) {
          localStorage.setItem('token', data.data.token)
          if (Array.isArray(data.data.roles)) localStorage.setItem('roles', JSON.stringify(data.data.roles))
          ElMessage.success('登录成功，欢迎回来！')
          router.push('/')
        } else {
          ElMessage.error(data.message || '登录失败，请检查账号密码')
        }
      } catch (error) {
        console.error(error)
        ElMessage.error('无法连接到服务器')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.login-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  /* 漂亮的蓝紫色渐变背景 */
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  background-size: cover;
}

.login-box {
  width: 400px;
  padding: 40px;
  background: #ffffff;
  border-radius: 16px; /* 圆角更大更柔和 */
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2); /* 悬浮感阴影 */
  text-align: center;
}

.login-header {
  margin-bottom: 30px;
}
.logo {
  font-size: 48px;
  margin-bottom: 10px;
}
.login-header h2 {
  margin: 0;
  font-size: 24px;
  color: #333;
  font-weight: 600;
}
.login-header p {
  margin: 8px 0 0;
  color: #999;
  font-size: 14px;
  letter-spacing: 1px;
}

.role-group {
  margin-bottom: 20px;
  display: flex;
  justify-content: center;
}

.login-btn {
  width: 100%;
  font-size: 16px;
  padding: 20px 0;
  border-radius: 8px;
  font-weight: bold;
  letter-spacing: 2px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: opacity 0.3s;
}
.login-btn:hover {
  opacity: 0.9;
}

.login-footer {
  margin-top: 20px;
  color: #bbb;
  font-size: 12px;
}
</style>
