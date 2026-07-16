const LOGGED_IN_KEY = 'kdd_logged_in'
const CURRENT_USER_KEY = 'kdd_current_user'
const CURRENT_USER_ID_KEY = 'kdd_current_user_id'

/**
 * 判断前端本地是否存在有效登录态。
 *
 * 当前项目没有引入 token，登录状态由 localStorage 中的标记和用户名共同决定。
 */
export function isLoggedIn() {
  return localStorage.getItem(LOGGED_IN_KEY) === 'true'
    && Boolean(localStorage.getItem(CURRENT_USER_KEY))
}

/**
 * 登录成功后保存最小用户信息。
 *
 * 只保存 id 和 username，避免把密码、密码哈希或其它敏感字段写入浏览器。
 */
export function setLoggedIn(user) {
  localStorage.setItem(LOGGED_IN_KEY, 'true')
  localStorage.setItem(CURRENT_USER_KEY, user.username)
  if (user.id !== undefined && user.id !== null) {
    localStorage.setItem(CURRENT_USER_ID_KEY, String(user.id))
  }
}

/**
 * 获取当前登录用户信息，供页面后续展示或扩展权限判断使用。
 */
export function getCurrentUser() {
  return {
    id: localStorage.getItem(CURRENT_USER_ID_KEY),
    username: localStorage.getItem(CURRENT_USER_KEY),
  }
}

/**
 * 退出登录时清除所有本地登录信息。
 */
export function logout() {
  localStorage.removeItem(LOGGED_IN_KEY)
  localStorage.removeItem(CURRENT_USER_KEY)
  localStorage.removeItem(CURRENT_USER_ID_KEY)
}
