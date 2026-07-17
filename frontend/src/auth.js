// 文件作用：封装浏览器本地登录态的读取、保存和清除。
// 项目位置：前端认证辅助模块，被登录页、路由守卫和退出登录按钮共同使用。
// 交互关系：LoginView 登录成功后写入 localStorage；router/index.js 根据这里的状态决定是否允许进入实验页。
//
// 逐词注释：
// const 表示常量；KEY 表示 localStorage 里的键名；export 表示把函数暴露给其它文件使用。
// localStorage 是浏览器本地存储；getItem 读取；setItem 写入；removeItem 删除。

const LOGGED_IN_KEY = 'kdd_logged_in' // 登录状态标记，值为字符串 'true' 时表示本地已登录。
const CURRENT_USER_KEY = 'kdd_current_user' // 当前用户名，对应后端返回的 username。
const CURRENT_USER_ID_KEY = 'kdd_current_user_id' // 当前用户 id，对应后端返回的 id。

/**
 * 判断前端本地是否存在有效登录态。
 *
 * 当前项目没有引入 token，登录状态由 localStorage 中的标记和用户名共同决定。
 */
export function isLoggedIn() {
  // 同时检查登录标记和用户名，避免只有一个残留字段时被误判为已登录。
  return localStorage.getItem(LOGGED_IN_KEY) === 'true'
    && Boolean(localStorage.getItem(CURRENT_USER_KEY))
}

/**
 * 登录成功后保存最小用户信息。
 *
 * 只保存 id 和 username，避免把密码、密码哈希或其它敏感字段写入浏览器。
 */
export function setLoggedIn(user) {
  // user 是后端登录接口返回的用户对象，这里只取前端需要的最小字段。
  localStorage.setItem(LOGGED_IN_KEY, 'true')
  localStorage.setItem(CURRENT_USER_KEY, user.username)
  // id 可能为 0，所以不能只用 if (user.id)，需要显式排除 undefined 和 null。
  if (user.id !== undefined && user.id !== null) {
    localStorage.setItem(CURRENT_USER_ID_KEY, String(user.id))
  }
}

/**
 * 获取当前登录用户信息，供页面后续展示或扩展权限判断使用。
 */
export function getCurrentUser() {
  // 返回对象形式，调用方可以通过 user.id / user.username 读取。
  return {
    id: localStorage.getItem(CURRENT_USER_ID_KEY),
    username: localStorage.getItem(CURRENT_USER_KEY),
  }
}

/**
 * 退出登录时清除所有本地登录信息。
 */
export function logout() {
  // removeItem 是幂等操作：即使键不存在，调用也不会报错。
  localStorage.removeItem(LOGGED_IN_KEY)
  localStorage.removeItem(CURRENT_USER_KEY)
  localStorage.removeItem(CURRENT_USER_ID_KEY)
}
