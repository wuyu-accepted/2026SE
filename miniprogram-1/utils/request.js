const { BASE_URL, TOKEN_KEY } = require('./config')

function doRequest(options) {
  const {
    url,
    method = 'GET',
    data,
    header = {},
    withAuth = true,
    timeout,
  } = options

  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync(TOKEN_KEY)
    const requestHeader = { ...header }

    if (withAuth && token) {
      requestHeader.Authorization = token
    }

    wx.request({
      url: `${BASE_URL}${url}`,
      method,
      data,
      header: requestHeader,
      timeout,
      success: (res) => {
        const response = res.data || {}

        if (res.statusCode >= 200 && res.statusCode < 300 && response.code === 0) {
          resolve(response.data)
          return
        }

        reject({
          statusCode: res.statusCode,
          message: response.message || '请求失败',
          ...response,
        })
      },
      fail: (error) => {
        reject({
          message: (error && error.errMsg) || '网络请求失败',
          ...error,
        })
      },
    })
  })
}

async function request(options) {
  try {
    return await doRequest(options)
  } catch (error) {
    const authExpired = error && (error.statusCode === 401 || error.code === 40100 || error.code === 40101)

    if (options.withAuth !== false && authExpired) {
      const { clearLoginState, redirectToLogin } = require('./auth')
      clearLoginState()
      redirectToLogin()
    }

    throw error
  }
}

module.exports = {
  request,
}
