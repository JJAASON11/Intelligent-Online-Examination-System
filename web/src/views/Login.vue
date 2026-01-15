<template>
  <div class="login-container">
    <div class="decoration-circle circle-1"></div>
    <div class="decoration-circle circle-2"></div>

    <div class="login-box animate-entry">
      <div class="login-header">
        <div class="logo-wrapper">
          <span class="logo">🎓</span>
        </div>
        <h2>智能在线考试系统</h2>
        <p class="subtitle">Intelligent Online Exam System</p>
      </div>

      <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          size="large"
          class="login-form"
          @submit.prevent
      >

        <el-form-item prop="username">
          <el-input
              v-model="form.username"
              placeholder="请输入用户名 / 学号 / 工号"
              :prefix-icon="User"
              clearable
              @keyup.enter="handleFocusPassword"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
              ref="passwordInputRef"
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              show-password
              :prefix-icon="Lock"
              @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item class="role-group" prop="role">
          <el-radio-group v-model="form.role" class="custom-radio-group">
            <el-radio-button label="STUDENT">学生</el-radio-button>
            <el-radio-button label="TEACHER">教师</el-radio-button>
            <el-radio-button label="ADMIN">管理员</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <div class="action-links">
          <el-checkbox v-model="rememberMe">记住我</el-checkbox>
          <el-link type="primary" :underline="false">忘记密码？</el-link>
        </div>

        <el-button
            type="primary"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
            round
        >
          {{ loading ? '正 在 登 录...' : '立 即 登 录' }}
        </el-button>
      </el-form>

      <div class="register-link">
        <span>还没有账号？</span>
        <el-link type="primary" :underline="false" @click="showRegisterDialog = true">立即创建账号</el-link>
      </div>

      <!-- 注册对话框 -->
      <el-dialog
          v-model="showRegisterDialog"
          title="创建账号"
          width="420px"
          :close-on-click-modal="false"
      >
        <el-form
            ref="registerFormRef"
            :model="registerForm"
            :rules="registerRules"
            label-width="80px"
            size="large"
        >
          <el-form-item label="用户名" prop="username">
            <el-input
                v-model="registerForm.username"
                placeholder="请输入用户名 / 学号 / 工号"
                :prefix-icon="User"
                clearable
            />
          </el-form-item>
          <el-form-item label="昵称" prop="nickname">
            <el-input
                v-model="registerForm.nickname"
                placeholder="请输入昵称"
                clearable
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="请输入密码"
                show-password
                :prefix-icon="Lock"
            />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                placeholder="请再次输入密码"
                show-password
                :prefix-icon="Lock"
            />
          </el-form-item>
          <el-form-item label="身份" prop="role">
            <el-radio-group v-model="registerForm.role" class="custom-radio-group">
              <el-radio-button label="STUDENT">学生</el-radio-button>
              <el-radio-button label="TEACHER">教师</el-radio-button>
              <el-radio-button label="ADMIN">管理员</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showRegisterDialog = false">取消</el-button>
          <el-button type="primary" :loading="registerLoading" @click="handleRegister">创建账号</el-button>
        </template>
      </el-dialog>

      <div class="login-footer">
        © 2026 Intelligent Exam System. All Rights Reserved.
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import http from '../api/http'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const formRef = ref(null)
const passwordInputRef = ref(null)
const rememberMe = ref(false)

const form = reactive({
  username: '',
  password: '',
  role: 'STUDENT'
})

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  role: [{ required: true, message: '请选择登录身份', trigger: 'change' }]
}

// 注册相关
const showRegisterDialog = ref(false)
const registerLoading = ref(false)
const registerFormRef = ref(null)
const registerForm = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
  role: 'STUDENT'
})

// 密码确认验证
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  role: [{ required: true, message: '请选择身份', trigger: 'change' }]
}

// 用户名回车跳转到密码框
const handleFocusPassword = () => {
  passwordInputRef.value?.focus()
}

const handleLogin = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        loading.value = true

        // 优化点：将 role 传递给后端，以便后端校验身份匹配
        const payload = {
          username: form.username,
          password: form.password,
          role: form.role
        }

        const { data } = await http.post('/auth/login', payload)

        if (data && data.code === 0) {
          // 1. 存储 Token
          localStorage.setItem('token', data.data.token)

          // 2. 存储用户信息 (根据实际后端返回结构调整)
          if (data.data.userId) localStorage.setItem('userId', String(data.data.userId))

          // 3. 存储角色 (如果后端返回了角色列表，使用后端的；否则使用用户选择的兜底)
          const rolesToSave = data.data.roles || [form.role]
          localStorage.setItem('roles', JSON.stringify(rolesToSave))

          // 4. 记住密码逻辑 (模拟)
          if (rememberMe.value) {
            localStorage.setItem('saved_username', form.username)
          } else {
            localStorage.removeItem('saved_username')
          }

          ElMessage.success({
            message: `登录成功，欢迎回来，${form.role === 'ADMIN' ? '管理员' : '同学'}！`,
            duration: 2000
          })

          // 延迟跳转，让用户看清成功提示
          setTimeout(() => {
          router.push('/')
          }, 500)

        } else {
          ElMessage.error(data.message || '登录失败，请检查账号密码或身份')
        }
      } catch (error) {
        console.error('Login Error:', error)
        // 容错处理
        const errorMsg = error.response?.data?.message || error.message || '服务连接超时'
        ElMessage.error(errorMsg)
      } finally {
        loading.value = false
      }
    }
  })
}

// 注册处理
const handleRegister = async () => {
  if (!registerFormRef.value) return

  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        registerLoading.value = true

        const payload = {
          username: registerForm.username,
          nickname: registerForm.nickname,
          password: registerForm.password,
          role: registerForm.role
        }

        const { data } = await http.post('/auth/register', payload)

        if (data && data.code === 0) {
          // 注册成功，自动登录
          localStorage.setItem('token', data.data.token)
          if (data.data.userId) localStorage.setItem('userId', String(data.data.userId))
          const rolesToSave = data.data.roles || [registerForm.role]
          localStorage.setItem('roles', JSON.stringify(rolesToSave))
          localStorage.setItem('username', data.data.username)
          localStorage.setItem('selectedRole', registerForm.role)

          ElMessage.success({
            message: '账号创建成功，已自动登录！',
            duration: 2000
          })

          // 关闭对话框并清空表单
          showRegisterDialog.value = false
          registerForm.username = ''
          registerForm.nickname = ''
          registerForm.password = ''
          registerForm.confirmPassword = ''
          registerForm.role = 'STUDENT'

          // 跳转到首页
          setTimeout(() => {
            router.push('/')
          }, 500)
        } else {
          ElMessage.error(data.message || '注册失败，请检查输入信息')
        }
      } catch (error) {
        console.error('Register Error:', error)
        const errorMsg = error.response?.data?.message || error.message || '服务连接超时'
        ElMessage.error(errorMsg)
      } finally {
        registerLoading.value = false
      }
    }
  })
}

// 可选：组件挂载时读取记住的账号
// import { onMounted } from 'vue'
// onMounted(() => {
//   const savedUser = localStorage.getItem('saved_username')
//   if (savedUser) {
//     form.username = savedUser
//     rememberMe.value = true
//   }
// })
</script>

<style scoped>
/* 容器布局 */
.login-container {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  width: 100vw;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  /* 或者使用深色渐变 */
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  overflow: hidden;
}

/* 装饰背景球 */
.decoration-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  z-index: 1;
}
.circle-1 {
  width: 300px;
  height: 300px;
  top: -50px;
  left: -50px;
  background: linear-gradient(to right, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05));
}
.circle-2 {
  width: 200px;
  height: 200px;
  bottom: 50px;
  right: 50px;
  background: linear-gradient(to left, rgba(255, 255, 255, 0.15), rgba(255, 255, 255, 0.05));
}

/* 登录卡片 - 毛玻璃效果 */
.login-box {
  position: relative;
  z-index: 2;
  width: 420px;
  padding: 45px 50px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.15);
  text-align: center;
  backdrop-filter: blur(20px); /* 关键：毛玻璃 */
  border: 1px solid rgba(255, 255, 255, 0.8);
}

/* 入场动画 */
.animate-entry {
  animation: fadeInUp 0.8s cubic-bezier(0.2, 0.8, 0.2, 1);
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Header */
.logo-wrapper {
  width: 80px;
  height: 80px;
  margin: 0 auto 15px;
  background: linear-gradient(135deg, #e0c3fc 0%, #8ec5fc 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 16px rgba(118, 75, 162, 0.2);
}

.logo {
  font-size: 40px;
}

.login-header h2 {
  margin: 0;
  font-size: 26px;
  color: #2c3e50;
  font-weight: 700;
}

.subtitle {
  margin: 8px 0 25px;
  color: #909399;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 1.5px;
}

/* Form Styles */
.role-group {
  margin: 25px 0;
}
.custom-radio-group {
  width: 100%;
  display: flex;
  justify-content: center;
}
/* 强制覆盖 Element Plus Radio Button 样式使其更撑满 */
:deep(.el-radio-button__inner) {
  width: 106px; /* 调整宽度以适应三列 */
  border-radius: 0;
}
:deep(.el-radio-button:first-child .el-radio-button__inner) {
  border-radius: 6px 0 0 6px;
}
:deep(.el-radio-button:last-child .el-radio-button__inner) {
  border-radius: 0 6px 6px 0;
}

.action-links {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
  padding: 0 5px;
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 2px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 8px 20px rgba(118, 75, 162, 0.4);
  transition: all 0.3s ease;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(118, 75, 162, 0.5);
  background: linear-gradient(90deg, #764ba2 0%, #667eea 100%);
}

.login-footer {
  margin-top: 30px;
  font-size: 12px;
  color: #bdc3c7;
}

.register-link {
  margin-top: 20px;
  text-align: center;
  font-size: 14px;
  color: #606266;
}

.register-link span {
  margin-right: 8px;
}

/* 响应式适配 */
@media (max-width: 480px) {
  .login-box {
    width: 90%;
    padding: 30px 20px;
  }
  .login-header h2 {
    font-size: 22px;
  }
  :deep(.el-radio-button__inner) {
    padding: 10px 15px; /* 移动端缩小内边距 */
    width: auto;
  }
}
</style>