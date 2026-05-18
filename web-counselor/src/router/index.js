import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'
import LoginView from '../views/LoginView.vue'
import PendingListView from '../views/PendingListView.vue'
import ProcessedListView from '../views/ProcessedListView.vue'
import ReviewDetailView from '../views/ReviewDetailView.vue'
import PlaceholderView from '../views/PlaceholderView.vue'
import DashboardView from '../views/DashboardView.vue'
import PartyImportView from '../views/PartyImportView.vue'
import StudentsView from '../views/StudentsView.vue'
import SettingsView from '../views/SettingsView.vue'
import PartyManagementView from '../views/PartyManagementView.vue'
import PartyProgressView from '../views/PartyProgressView.vue'
import PartyReportReviewView from '../views/PartyReportReviewView.vue'
import PartyActivityReviewView from '../views/PartyActivityReviewView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView },
    {
      path: '/',
      component: MainLayout,
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', name: 'dashboard', component: DashboardView },

        // 审批管理
        { path: 'review/pending', name: 'pending', component: PendingListView },
        { path: 'review/processed', name: 'processed', component: ProcessedListView },
        { path: 'review/:id', name: 'detail', component: ReviewDetailView },

        // 占位路由（交由组员后续实现）
        { path: 'party/import', name: 'partyImport', component: PartyImportView, meta: { title: '入党进度导入' } },
        { path: 'party', name: 'party', component: PartyManagementView, meta: { title: '党团管理' } },
        { path: 'knowledge', name: 'knowledge', component: PlaceholderView, meta: { title: '知识库管理' } },
        { path: 'students', name: 'students', component: StudentsView, meta: { title: '学生管理' } },
        { path: 'notices', name: 'notices', component: PlaceholderView, meta: { title: '通知发布' } },
        { path: 'settings', name: 'settings', component: SettingsView, meta: { title: '系统设置' } },
      ],
    },
  ],
})

router.beforeEach((to, _from, next) => {
  if (to.path !== '/login') {
    const token = localStorage.getItem('accessToken')
    if (!token) {
      next('/login')
      return
    }
  }
  next()
})

export default router
