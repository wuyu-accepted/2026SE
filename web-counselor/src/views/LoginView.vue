<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register } from '../api/auth'

const router = useRouter()
const loading = ref(false)
const registerLoading = ref(false)
const activeTab = ref('login')
const form = ref({ studentNo: '', password: '' })
const loginAccountError = ref('')
const registerForm = ref({
  realName: '',
  studentNo: '',
  password: '',
  confirmPassword: '',
  phone: '',
  email: '',
})

async function doLogin() {
  if (!form.value.studentNo || !form.value.password) {
    ElMessage.warning('请输入工号/管理员账号和密码')
    return
  }
  if (!isValidWebAccount(form.value.studentNo)) {
    loginAccountError.value = '用户名不合法，请输入数字工号'
    form.value.studentNo = ''
    ElMessage.warning('工号只能填写数字，管理员账号固定为 admin')
    return
  }
  loading.value = true
  try {
    const res = await login(form.value)
    localStorage.setItem('accessToken', res.token)
    if (res.user) {
      localStorage.setItem('currentUser', JSON.stringify(res.user))
    }
    router.push('/dashboard')
  } catch (e) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}

async function doRegister() {
  if (!registerForm.value.realName || !registerForm.value.studentNo || !registerForm.value.password) {
    ElMessage.warning('请输入姓名、工号和密码')
    return
  }
  if (registerForm.value.password.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  if (registerForm.value.studentNo.toLowerCase() === 'admin') {
    ElMessage.warning('管理员账号不允许注册')
    return
  }
  if (!/^\d+$/.test(registerForm.value.studentNo)) {
    ElMessage.warning('辅导员工号只能填写数字')
    return
  }
  registerLoading.value = true
  try {
    const res = await register(registerForm.value)
    if (res.token) {
      localStorage.setItem('accessToken', res.token)
      if (res.user) {
        localStorage.setItem('currentUser', JSON.stringify(res.user))
      }
      router.push('/dashboard')
      return
    }
    ElMessage.success('注册已提交，请等待管理员审核通过后登录')
    activeTab.value = 'login'
    form.value = { studentNo: registerForm.value.studentNo, password: '' }
  } catch (e) {
    ElMessage.error(e.message || '注册失败')
  } finally {
    registerLoading.value = false
  }
}

function onLoginAccountInput() {
  loginAccountError.value = ''
}

function validateLoginAccountOnBlur() {
  form.value.studentNo = String(form.value.studentNo || '').trim()
  if (!form.value.studentNo || isValidWebAccount(form.value.studentNo)) {
    loginAccountError.value = ''
    return
  }
  loginAccountError.value = '用户名不合法，请输入数字工号'
  form.value.studentNo = ''
}

function normalizeRegisterJobNo(value) {
  registerForm.value.studentNo = String(value || '').replace(/\D/g, '')
}

function isValidWebAccount(value) {
  return value === 'admin' || /^\d+$/.test(value)
}
</script>

<template>
  <div class="page">
    <div class="login-bg"></div>
    <el-card class="card" shadow="xl">
      <div class="card-header">
        <div class="logo-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2L2 7l10 5 10-5-10-5z"/>
            <path d="M2 17l10 5 10-5"/>
            <path d="M2 12l10 5 10-5"/>
          </svg>
        </div>
        <h2>学院服务平台</h2>
        <p class="subtitle">辅导员 / 管理员端</p>
      </div>
      <div class="card-body">
        <el-tabs v-model="activeTab" stretch>
          <el-tab-pane label="登录" name="login">
            <el-form @submit.prevent="doLogin">
              <el-form-item>
                <el-input
                  v-model="form.studentNo"
                  placeholder="数字工号"
                  size="large"
                  :prefix-icon="'User'"
                  @input="onLoginAccountInput"
                  @blur="validateLoginAccountOnBlur"
                />
                <p class="field-error">{{ loginAccountError }}</p>
              </el-form-item>
              <el-form-item>
                <el-input v-model="form.password" type="password" placeholder="密码" size="large" :prefix-icon="'Lock'" show-password />
              </el-form-item>
              <el-button type="primary" size="large" class="login-btn" :loading="loading" native-type="submit">登 录</el-button>
            </el-form>
          </el-tab-pane>
          <el-tab-pane label="辅导员注册" name="register">
            <el-form @submit.prevent="doRegister">
              <el-form-item>
                <el-input v-model="registerForm.realName" placeholder="姓名" size="large" :prefix-icon="'User'" />
              </el-form-item>
              <el-form-item>
                <el-input
                  v-model="registerForm.studentNo"
                  placeholder="数字辅导员工号"
                  size="large"
                  :prefix-icon="'Tickets'"
                  @input="normalizeRegisterJobNo"
                />
              </el-form-item>
              <el-form-item>
                <el-input v-model="registerForm.phone" placeholder="手机号（选填）" size="large" :prefix-icon="'Phone'" />
              </el-form-item>
              <el-form-item>
                <el-input v-model="registerForm.email" placeholder="邮箱（选填）" size="large" :prefix-icon="'Message'" />
              </el-form-item>
              <el-form-item>
                <el-input v-model="registerForm.password" type="password" placeholder="密码" size="large" :prefix-icon="'Lock'" show-password />
              </el-form-item>
              <el-form-item>
                <el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" size="large" :prefix-icon="'Lock'" show-password />
              </el-form-item>
              <el-button type="primary" size="large" class="login-btn" :loading="registerLoading" native-type="submit">提交注册申请</el-button>
            </el-form>
            <p class="register-note">注册后需管理员审核通过才能登录；管理员账号由系统内置，不开放注册。</p>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  background: linear-gradient(135deg, #1a365d 0%, #153e75 50%, #1e40af 100%);
  overflow: hidden;
}

.login-bg {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(ellipse at 20% 50%, rgba(59, 130, 246, 0.15) 0%, transparent 50%),
                    radial-gradient(ellipse at 80% 50%, rgba(99, 102, 241, 0.1) 0%, transparent 50%);
}

.card {
  width: 400px;
  border-radius: 16px;
  position: relative;
  overflow: hidden;
}

.card :deep(.el-card__body) {
  padding: 0;
}

.card-header {
  text-align: center;
  padding: 40px 32px 24px;
  background: linear-gradient(135deg, #f8faff 0%, #eef2ff 100%);
}

.logo-icon {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #1677ff, #6366f1);
  border-radius: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin-bottom: 16px;
}

.logo-icon svg {
  width: 28px;
  height: 28px;
}

.card-header h2 {
  margin: 0;
  font-size: 22px;
  color: #1a1a1a;
}

.subtitle {
  margin: 6px 0 0;
  color: #666;
  font-size: 14px;
}

.card-body {
  padding: 24px 32px 32px;
}

.field-error {
  width: 100%;
  min-height: 16px;
  margin: 4px 0 -2px;
  color: #f56c6c;
  font-size: 12px;
  line-height: 16px;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  border-radius: 8px;
  margin-top: 4px;
}

.register-note {
  margin: 12px 0 0;
  font-size: 12px;
  color: #999;
  line-height: 1.6;
}
</style>
