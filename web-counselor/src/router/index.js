import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'
import LoginView from '../views/LoginView.vue'
import PendingListView from '../views/PendingListView.vue'
import ProcessedListView from '../views/ProcessedListView.vue'
import ReviewDetailView from '../views/ReviewDetailView.vue'
import PlaceholderView from '../views/PlaceholderView.vue'
import DashboardView from '../views/DashboardView.vue'
import StudentsView from '../views/StudentsView.vue'
import SettingsView from '../views/SettingsView.vue'
import PartyManagementView from '../views/PartyManagementView.vue'
import PartyProgressView from '../views/PartyProgressView.vue'
import PartyReportReviewView from '../views/PartyReportReviewView.vue'
import PartyActivityReviewView from '../views/PartyActivityReviewView.vue'
import { fetchCurrentUser } from '../api/auth'

const WEB_ROLES = ['counselor', 'admin']
const ADMIN_ROLES = ['admin']

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView },
    {
      path: '/',
      component: MainLayout,
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', name: 'dashboard', component: DashboardView, meta: { roles: WEB_ROLES } },

        // 审批管理
        { path: 'review/pending', name: 'pending', component: PendingListView, meta: { roles: WEB_ROLES } },
        { path: 'review/processed', name: 'processed', component: ProcessedListView, meta: { roles: WEB_ROLES } },
        { path: 'review/:id', name: 'detail', component: ReviewDetailView, meta: { roles: WEB_ROLES } },

        // 占位路由（交由组员后续实现）
        { path: 'party/import', redirect: '/party' },
        { path: 'party', name: 'party', component: PartyManagementView, meta: { title: '党团管理' } },
        { path: 'knowledge', name: 'knowledge', component: PlaceholderView, meta: { title: '知识库管理' } },
        { path: 'students', name: 'students', component: StudentsView, meta: { title: '学生管理' } },
        { path: 'notices', name: 'notices', component: PlaceholderView, meta: { title: '通知发布' } },
        { path: 'settings', name: 'settings', component: SettingsView, meta: { title: '系统设置' } },
        { path: 'party/import', name: 'partyImport', component: PartyImportView, meta: { title: '入党进度导入', roles: WEB_ROLES } },
        { path: 'party', name: 'party', component: PartyManagementView, meta: { title: '党团管理', roles: WEB_ROLES } },
        { path: 'knowledge', name: 'knowledge', component: PlaceholderView, meta: { title: '知识库管理', roles: WEB_ROLES } },
        { path: 'students', name: 'students', component: StudentsView, meta: { title: '学生管理', roles: WEB_ROLES } },
        { path: 'notices', name: 'notices', component: PlaceholderView, meta: { title: '通知发布', roles: WEB_ROLES } },
        { path: 'settings', name: 'settings', component: SettingsView, meta: { title: '系统设置', roles: ADMIN_ROLES } },
      ],
    },
  ],
})

function readCurrentUser() {
  try {
    const saved = localStorage.getItem('currentUser')
    return saved ? JSON.parse(saved) : null
  } catch (_) {
    return null
  }
}

router.beforeEach(async (to, _from, next) => {
  if (to.path !== '/login') {
    const token = localStorage.getItem('accessToken')
    if (!token) {
      next('/login')
      return
    }
    let user = readCurrentUser()
    if (!user) {
      try {
        user = await fetchCurrentUser()
        localStorage.setItem('currentUser', JSON.stringify(user))
      } catch (_) {
        localStorage.removeItem('accessToken')
        localStorage.removeItem('currentUser')
        next('/login')
        return
      }
    }
    const roles = user.roles || []
    const requiredRoles = to.meta.roles || WEB_ROLES
    const allowed = requiredRoles.some((role) => roles.includes(role))
    if (!allowed) {
      next(roles.some((role) => WEB_ROLES.includes(role)) ? '/dashboard' : '/login')
      return
    }
  }
  next()
})

export default router
