const homeData = {
  banner: {
    eyebrow: '学生服务中心',
    title: '学生服务与党团事务平台',
    subtitle: '当后端请求失败时，将使用本地演示数据兜底。',
    badge: '演示',
    meta: [
      { label: '模块数', value: '3' },
      { label: '状态', value: '已连接' },
      { label: '版本', value: 'V1' },
    ],
  },
  quickEntries: [
    {
      title: '知识库',
      desc: '查询文章与模板信息。',
      icon: '知',
      status: '可用',
      path: '/pages/knowledge/knowledge',
    },
    {
      title: '通知',
      desc: '查看通知消息与未读提醒。',
      icon: '通',
      status: '可用',
      path: '/pages/notice/notice',
    },
    {
      title: '服务',
      desc: '请假、党团事务、模板下载等入口。',
      icon: '服',
      status: '可用',
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
  serviceHighlights: [
    {
      title: '学生服务',
      desc: '围绕查询、申请、跟踪三类流程构建。',
    },
    {
      title: '党团事务',
      desc: '当前演示版聚焦学生端流程查看。',
    },
    {
      title: '下一步',
      desc: '后续可扩展审批、荣誉展示与分析能力。',
    },
  ],
}

const knowledgeBaseData = {
  categories: ['全部'],
  articles: [],
  templates: [],
}

const partyProgressData = {
  profile: {
    name: '演示用户',
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
