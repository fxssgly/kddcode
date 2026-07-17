// 文件作用：定义前端页面路由和登录拦截规则。
// 项目位置：Vue Router 配置层，负责 URL 和页面组件之间的对应关系。
// 交互关系：读取 auth.js 的登录状态；未登录用户会被导回 LoginView，登录后才能进入实验页面。
//
// 逐词注释：
// createRouter 创建路由器；createWebHistory 使用浏览器 history 模式；component 表示路由要渲染的页面。
// path 是地址路径；redirect 是重定向；beforeEach 是每次跳转前执行的守卫；return 字符串表示改跳到新地址。

import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import AssociationView from '../views/AssociationView.vue'
import ClusteringView from '../views/ClusteringView.vue'
import ClassificationView from '../views/ClassificationView.vue'
import RegressionView from '../views/RegressionView.vue'
import { isLoggedIn } from '../auth'

// 路由表定义了左侧菜单和页面组件之间的对应关系。
// 新增实验页面时，通常需要同时在这里和 App.vue 的侧边菜单中添加入口。
const routes = [
  { path: '/', redirect: '/login' }, // 访问根地址时自动去登录页。
  { path: '/login', component: LoginView }, // 登录/注册页面。
  { path: '/association', component: AssociationView }, // 关联规则实验页面。
  { path: '/clustering', component: ClusteringView }, // 聚类实验页面。
  { path: '/classification', component: ClassificationView }, // 分类实验页面。
  { path: '/regression', component: RegressionView }, // 回归实验页面。
]

const router = createRouter({
  // 使用 HTML5 history 模式，URL 更干净；生产部署时后端需要把未知路径回退到 index.html。
  history: createWebHistory(),
  routes,
})

// 全局前置守卫：在页面切换前检查登录状态。
router.beforeEach((to) => {
  // to 表示“将要去的路由对象”，to.path 是目标地址。
  const loggedIn = isLoggedIn()
  // 未登录用户只能访问登录页，访问实验页面时强制回到 /login。
  if (to.path !== '/login' && !loggedIn) return '/login'
  // 已登录用户再次打开登录页时，直接进入默认的关联规则页面。
  if (to.path === '/login' && loggedIn) return '/association'
  return true
})

export default router
