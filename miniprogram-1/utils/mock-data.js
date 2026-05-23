const homeData = {
  banner: {
    eyebrow: '学生服务中心',
    title: '学生服务与党团事务平台',
    subtitle: '统一查询知识、通知和服务入口，帮助学生更快找到所需内容。',
  },
  quickEntries: [
    {
      title: '知识库',
      desc: '查询文章、模板和政策资料。',
      icon: '知',
      path: '/pages/knowledge/knowledge',
    },
    {
      title: '通知',
      desc: '查看通知消息与未读提醒。',
      icon: '通',
      path: '/pages/notice/notice',
    },
    {
      title: '服务',
      desc: '请假、党团事务、模板下载等入口。',
      icon: '服',
      path: '/pages/service/service',
    },
  ],
  todoStats: [
    { label: '未读', value: '0', hint: '消息' },
    { label: '提醒', value: '0', hint: '党团流程' },
    { label: '汇报', value: '0', hint: '待处理' },
  ],
  latestNotices: [],
  downloads: [],
}

const knowledgeBaseData = {
  categories: ['全部', '党团流程', '日常服务', '奖助学金', '学业发展', '就业创业', '心理健康', '宿舍管理', '证件证明', '通知公告', '制度政策', '办事指南'],
  articles: [],
  templates: [],
}

const partyProgressData = {
  profile: {
    name: '学生用户',
    major: '软件工程',
    className: '2023级',
  },
  currentStage: {
    stage: '加载中',
    description: '正在使用本地兜底数据。',
    updatedAt: '',
  },
  stages: [],
  reminders: [],
}

module.exports = {
  homeData,
  knowledgeBaseData,
  partyProgressData,
}
