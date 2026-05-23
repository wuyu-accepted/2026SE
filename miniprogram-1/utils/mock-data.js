const homeData = {
  banners: [
    {
      id: 0,
      title: '欢迎使用学院服务平台',
      subtitle: '便捷获取政策信息与党团服务',
      targetType: 'none',
    },
  ],
  quickEntries: [],
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
