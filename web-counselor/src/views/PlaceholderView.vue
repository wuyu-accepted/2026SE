<script setup>
import { useRoute } from 'vue-router'
import { computed } from 'vue'

const route = useRoute()
const title = computed(() => route.meta?.title || '功能模块')

const placeholderModules = {
  '党团管理': {
    icon: '🏛️',
    features: ['入党积极分子管理', '党员发展流程追踪', '思想汇报审核', '党团活动记录'],
  },
  '知识库管理': {
    icon: '📚',
    features: ['知识文章管理', '分类维护', '模板文件管理', '搜索与检索配置'],
  },
  '学生管理': {
    icon: '👥',
    features: ['学生信息管理', '批量导入/导出', '账号管理', '角色分配'],
  },
  '通知发布': {
    icon: '📢',
    features: ['通知编辑与发布', '发布范围选择', '已读回执统计', '历史通知管理'],
  },
  '系统设置': {
    icon: '⚙️',
    features: ['角色权限配置', '系统参数设置', '操作日志审计', '数据备份'],
  },
}

const module = computed(() => placeholderModules[title.value] || { icon: '📋', features: [] })
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="header">
        <span class="module-icon">{{ module.icon }}</span>
        <span class="module-title">{{ title }}</span>
        <el-tag size="small" type="info">开发中</el-tag>
      </div>
    </template>
    <div class="body">
      <el-alert
        title="该模块尚未实现"
        type="info"
        :closable="false"
        show-icon
        description="此功能板块将由组员后续实现，目前已预留菜单入口和路由。"
      />
      <div class="feature-list">
        <div class="feature-title">规划功能：</div>
        <el-tag
          v-for="feature in module.features"
          :key="feature"
          class="feature-tag"
          hit
        >
          {{ feature }}
        </el-tag>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.module-icon {
  font-size: 22px;
}

.module-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.body {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.feature-list {
  padding: 12px 16px;
  background: #fafafa;
  border-radius: 8px;
}

.feature-title {
  font-size: 13px;
  color: #666;
  margin-bottom: 10px;
}

.feature-tag {
  margin: 0 8px 8px 0;
}
</style>
