// 文件作用：登录状态工具层，统一读写浏览器 localStorage 中的登录信息。
// 关联文件：LoginView.vue 登录成功后调用 setLoggedIn；router/index.js 用 isLoggedIn 做路由守卫；App.vue 用 getCurrentUser 显示用户名。
const LOGGED_IN_KEY = 'kdd_logged_in'
const CURRENT_USER_KEY = 'kdd_current_user'
const CURRENT_USER_ID_KEY = 'kdd_current_user_id'

// 判断是否已登录：既要有登录标记，也要有用户名，避免 localStorage 只有半截数据时误判。
export function isLoggedIn() {
  return localStorage.getItem(LOGGED_IN_KEY) === 'true'
    && Boolean(localStorage.getItem(CURRENT_USER_KEY))
}

// 登录成功后把用户信息保存到 localStorage；刷新页面后路由守卫仍能识别登录状态。
export function setLoggedIn(user) {
  localStorage.setItem(LOGGED_IN_KEY, 'true')
  localStorage.setItem(CURRENT_USER_KEY, user.username)
  // id 可能不存在，所以只有后端返回了有效 id 时才保存。
  if (user.id !== undefined && user.id !== null) {
    localStorage.setItem(CURRENT_USER_ID_KEY, String(user.id))
  }
}

// 给页面读取当前用户；返回对象方便模板中写 currentUser.username。
export function getCurrentUser() {
  return {
    id: localStorage.getItem(CURRENT_USER_ID_KEY),
    username: localStorage.getItem(CURRENT_USER_KEY),
  }
}

// 退出登录时清空所有本地登录字段；之后路由守卫会把用户挡回 /login。
export function logout() {
  localStorage.removeItem(LOGGED_IN_KEY)
  localStorage.removeItem(CURRENT_USER_KEY)
  localStorage.removeItem(CURRENT_USER_ID_KEY)
}
