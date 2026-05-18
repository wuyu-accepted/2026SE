<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchCurrentUser } from '../api/auth'

const route = useRoute()
const router = useRouter()
const userRoles = ref([])

function loadUserFromStorage() {
  try {
    const saved = localStorage.getItem('currentUser')
    if (saved) {
      userRoles.value = JSON.parse(saved).roles || []
      return true
    }
  } catch (_) {}
  return false
}

// 先尝试从缓存读取
if (!loadUserFromStorage()) {
  // 缓存没有的话，登录后会触发 Dashboard 去拉取，
  // 所以这里用 onMounted 兜底
}

onMounted(async () => {
  if (!loadUserFromStorage()) {
    try {
      const user = await fetchCurrentUser()
      if (user) {
        localStorage.setItem('currentUser', JSON.stringify(user))
        loadUserFromStorage()
      }
    } catch (_) {}
  }
})

const allMenus = [
  { path: '/dashboard', label: '工作台', icon: 'Odometer' },
  { path: '/review/pending', label: '待审批', icon: 'Clock' },
  { path: '/review/processed', label: '已处理', icon: 'Select' },
  { path: '/party/progress', label: '党员发展流程', icon: 'Flag' },
  { path: '/party/reports', label: '思想汇报审核', icon: 'Document' },
  { path: '/party/activities', label: '党团活动审批', icon: 'Calendar' },
  { path: '/knowledge', label: '知识库管理', icon: 'Notebook' },
  { path: '/students', label: '学生管理', icon: 'User' },
  { path: '/notices', label: '通知发布', icon: 'Bell' },
  { path: '/settings', label: '系统设置', icon: 'Setting' },
]

const menus = computed(() => {
  if (userRoles.value.includes('admin')) {
    return allMenus
  }
  return allMenus.filter(m => m.path !== '/settings')
})

const isAdmin = computed(() => userRoles.value.includes('admin'))
const userName = ref(localStorage.getItem('currentUser') ? (JSON.parse(localStorage.getItem('currentUser')).realName || '用户') : '用户')

const currentTitle = computed(() => {
  const m = allMenus.find(m => route.path.startsWith(m.path))
  return m ? m.label : '审批管理'
})

function navTo(path) {
  if (route.path !== path) {
    router.push(path)
  }
}

function doLogout() {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('currentUser')
  router.push('/login')
}
</script>

<template>
  <el-container class="layout">
    <el-aside class="aside" width="220px">
      <div class="brand">
        <div class="brand-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2L2 7l10 5 10-5-10-5z"/>
            <path d="M2 17l10 5 10-5"/>
            <path d="M2 12l10 5 10-5"/>
          </svg>
        </div>
        <div class="brand-text">
          <span class="brand-title">服务平台</span>
          <span class="brand-sub">管理端</span>
        </div>
      </div>

      <el-menu
        :default-active="route.path"
        class="menu"
        @select="navTo"
      >
        <el-menu-item
          v-for="item in menus"
          :key="item.path"
          :index="item.path"
        >
          <el-icon>
            <component :is="item.icon" />
          </el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <h2 class="page-title">{{ currentTitle }}</h2>
        </div>
        <div class="header-right">
          <el-tag v-if="isAdmin" type="danger" effect="dark" size="small">管理员</el-tag>
          <el-tag v-else type="warning" effect="dark" size="small">辅导员</el-tag>
          <el-dropdown @command="(cmd) => cmd === 'logout' && doLogout()">
            <span class="user-info">
              <el-avatar size="small" icon="UserFilled" />
              <span class="username">{{ userName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout {
  min-height: 100vh;
}

.aside {
  background: #001529;
  display: flex;
  flex-direction: column;
}

.brand {
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-icon {
  width: 32px;
  height: 32px;
  background: #1677ff;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.brand-icon svg {
  width: 18px;
  height: 18px;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.brand-title {
  color: #fff;
  font-size: 15px;
  font-weight: 600;
}

.brand-sub {
  color: rgba(255, 255, 255, 0.55);
  font-size: 11px;
}

.menu {
  flex: 1;
  border-right: none;
  background: transparent;
}

.menu :deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.65);
  margin: 2px 8px;
  border-radius: 6px;
  height: 42px;
  line-height: 42px;
}

.menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.06);
}

.menu :deep(.el-menu-item.is-active) {
  background: #1677ff;
  color: #fff;
}

.menu :deep(.el-icon) {
  margin-right: 8px;
}

.header {
  background: #fff;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 1px solid #e8e8e8;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.header-left {
  display: flex;
  align-items: center;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.username {
  font-size: 14px;
  color: #333;
}

.main {
  background: #f0f2f5;
  padding: 24px;
}
</style>
