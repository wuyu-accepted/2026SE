const { request } = require('./request')
const { TOKEN_KEY, USER_KEY, LOGIN_PAGE } = require('./config')

function getSession() {
  const token = wx.getStorageSync(TOKEN_KEY)
  const user = wx.getStorageSync(USER_KEY)
  return {
    token,
    user,
  }
}

function setSession(loginData) {
  wx.setStorageSync(TOKEN_KEY, loginData.token)
  wx.setStorageSync(USER_KEY, loginData.user || null)

  const app = getApp()
  if (app && app.globalData) {
    app.globalData.token = loginData.token || ''
    app.globalData.userInfo = loginData.user || null
  }
}

function setCurrentUser(user) {
  wx.setStorageSync(USER_KEY, user || null)

  const app = getApp()
  if (app && app.globalData) {
    app.globalData.userInfo = user || null
  }
}

function clearLoginState() {
  wx.removeStorageSync(TOKEN_KEY)
  wx.removeStorageSync(USER_KEY)

  const app = getApp()
  if (app && app.globalData) {
    app.globalData.token = ''
    app.globalData.userInfo = null
  }
}

function redirectToLogin() {
  const pages = getCurrentPages()
  const currentRoute = pages.length ? `/${pages[pages.length - 1].route}` : ''
  if (currentRoute === LOGIN_PAGE) {
    return
  }

  wx.reLaunch({
    url: LOGIN_PAGE,
  })
}

async function ensureLogin(force = false) {
  if (force) {
    clearLoginState()
  }

  const { token, user } = getSession()

  if (token && user) {
    return { token, user }
  }

  if (token) {
    const currentUser = await request({ url: '/api/auth/me' })
    setCurrentUser(currentUser)
    return {
      token,
      user: currentUser,
    }
  }

  redirectToLogin()
  const error = new Error('NOT_LOGGED_IN')
  error.code = 'NOT_LOGGED_IN'
  throw error
}

async function login(payload) {
  const loginData = await request({
    url: '/api/auth/login',
    method: 'POST',
    data: {
      ...payload,
      clientType: 'miniprogram',
    },
    header: {
      'Content-Type': 'application/json',
    },
    withAuth: false,
  })

  setSession(loginData)
  return loginData
}

async function register(payload) {
  const loginData = await request({
    url: '/api/auth/register',
    method: 'POST',
    data: {
      ...payload,
      clientType: 'miniprogram',
    },
    header: {
      'Content-Type': 'application/json',
    },
    withAuth: false,
  })

  setSession(loginData)
  return loginData
}

async function logout() {
  try {
    const { token } = getSession()
    if (token) {
      await request({
        url: '/api/auth/logout',
        method: 'POST',
      })
    }
  } finally {
    clearLoginState()
    redirectToLogin()
  }
}

module.exports = {
  clearLoginState,
  ensureLogin,
  getSession,
  login,
  logout,
  redirectToLogin,
  register,
  setCurrentUser,
}
