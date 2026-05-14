<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

function loginAsDemoCounselor() {
  localStorage.removeItem('accessToken')
  router.push('/dashboard')
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
        <p class="desc">开发环境可直接进入</p>
        <el-button type="primary" size="large" class="enter-btn" @click="loginAsDemoCounselor">
          进入管理后台
        </el-button>
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
  padding: 24px 32px 40px;
  text-align: center;
}

.desc {
  color: #64748b;
  margin-bottom: 20px;
  font-size: 14px;
}

.enter-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  border-radius: 8px;
}
</style>
