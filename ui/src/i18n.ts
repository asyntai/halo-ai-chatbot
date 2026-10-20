// The tab follows the language of the Halo console: Chinese or English.

export function isChinese(): boolean {
  // Halo keeps the console language in the "language" cookie (zh-CN, en, ...).
  const cookie = document.cookie.match(/(?:^|;\s*)language=([^;]+)/)
  let locale = cookie ? decodeURIComponent(cookie[1]) : ''
  if (!locale) {
    try {
      locale = window.localStorage.getItem('locale') || ''
    } catch {
      locale = ''
    }
  }
  if (!locale) {
    locale = document.documentElement.lang || navigator.language || ''
  }
  return /^zh/i.test(locale)
}

const zh = {
  title: 'Asyntai AI 智能客服',
  intro: '在网站的每个页面上添加 AI 智能客服。它根据你的网站内容回答访客的问题，支持 80 多种语言。',
  loading: '正在加载…',
  statusLabel: '状态：',
  connected: '已连接',
  notConnected: '未连接',
  as: '账号：',
  widgetId: '小部件 ID：',
  connectedHelp: '聊天窗口已显示在网站的每个页面上。在 Asyntai 控制台中可以修改它的外观、知识和回答。',
  openDashboard: '打开 Asyntai 控制台',
  docs: '使用文档',
  disconnect: '断开连接',
  disabledNote: '聊天窗口已在“基本设置”中关闭，网站上不会显示。',
  connectHelp: '登录 Asyntai 或免费创建账号。会打开一个新窗口，完成后本页会自动保存连接。',
  connect: '连接 Asyntai',
  fallback: '如果窗口没有打开，请',
  fallbackLink: '打开登录页面',
  popupBlocked: '浏览器阻止了弹出窗口。请使用下面的登录链接。',
  finishInWindow: '请在新窗口中完成登录。',
  finishInTab: '请在新标签页中完成登录。',
  saving: '已连接，正在保存…',
  saved: '已连接。聊天窗口现在显示在你的网站上。',
  timedOut: '5 分钟内没有完成登录，已停止检查。请再次点击“连接 Asyntai”，或在下面填写小部件 ID。',
  unreachable: '暂时无法连接 Asyntai 服务器，已停止检查。请稍后再次点击“连接 Asyntai”，或在下面填写小部件 ID。',
  windowClosed: '登录窗口已关闭，连接未完成。请再次点击“连接 Asyntai”。',
  saveFailed: '无法保存设置：',
  disconnected: '已断开连接。聊天窗口不再显示。',
  manualTitle: '或填写小部件 ID',
  manualHelp: '在 Asyntai 控制台的“安装”页面可以找到，格式如 asyntai_1a2b3c4d5e6f。',
  save: '保存',
  invalidId: '小部件 ID 的格式为 asyntai_ 加字母和数字。',
}

const en: typeof zh = {
  title: 'Asyntai AI Chatbot',
  intro: 'An AI chatbot on every page of your website. It answers visitor questions from your own website content, in more than 80 languages.',
  loading: 'Loading…',
  statusLabel: 'Status: ',
  connected: 'Connected',
  notConnected: 'Not connected',
  as: 'Account: ',
  widgetId: 'Widget ID: ',
  connectedHelp: 'The chatbot shows on every page of your website. Change its look, its knowledge and its answers in your Asyntai dashboard.',
  openDashboard: 'Open the Asyntai dashboard',
  docs: 'Documentation',
  disconnect: 'Disconnect',
  disabledNote: 'The chat widget is turned off in the Basic settings, so your website does not show it.',
  connectHelp: 'Sign in to Asyntai or create a free account. A new window opens. When you finish there, this page saves the connection.',
  connect: 'Connect Asyntai',
  fallback: 'If the window does not open, ',
  fallbackLink: 'open the sign-in page',
  popupBlocked: 'Your browser blocked the window. Use the sign-in link below.',
  finishInWindow: 'Finish the sign-in in the new window.',
  finishInTab: 'Finish the sign-in in the new tab.',
  saving: 'Connected. Saving…',
  saved: 'Connected. The chatbot now shows on your website.',
  timedOut: 'The sign-in did not finish within 5 minutes, so the check stopped. Click "Connect Asyntai" again, or enter your widget ID below.',
  unreachable: 'The Asyntai server cannot be reached now, so the check stopped. Click "Connect Asyntai" again later, or enter your widget ID below.',
  windowClosed: 'The sign-in window was closed before the connection finished. Click "Connect Asyntai" again.',
  saveFailed: 'The settings could not be saved: ',
  disconnected: 'Disconnected. The chatbot no longer shows.',
  manualTitle: 'Or enter your widget ID',
  manualHelp: 'Find it on the Install page of your Asyntai dashboard. It looks like asyntai_1a2b3c4d5e6f.',
  save: 'Save',
  invalidId: 'The widget ID looks like asyntai_ followed by letters and numbers.',
}

export function messages(): typeof zh {
  return isChinese() ? zh : en
}
