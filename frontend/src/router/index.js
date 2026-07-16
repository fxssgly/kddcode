// 文件作用：定义前端页面路由和登录拦截规则。
// 项目位置：Vue Router 配置层，负责 URL 和页面组件之间的对应关系。
// 交互关系：读取 auth.js 的登录状态；未登录用户会被导回 LoginView，登录后才能进入实验页面。

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
  { path: '/', redirect: '/login' },
  { path: '/login', component: LoginView },
  { path: '/association', component: AssociationView },
  { path: '/clustering', component: ClusteringView },
  { path: '/classification', component: ClassificationView },
  { path: '/regression', component: RegressionView },
]

const router = createRouter({
  // 使用 HTML5 history 模式，URL 更干净；生产部署时后端需要把未知路径回退到 index.html。
  history: createWebHistory(),
  routes,
})

// 全局前置守卫：在页面切换前检查登录状态。
router.beforeEach((to) => {
  const loggedIn = isLoggedIn()
  // 未登录用户只能访问登录页，访问实验页面时强制回到 /login。
  if (to.path !== '/login' && !loggedIn) return '/login'
  // 已登录用户再次打开登录页时，直接进入默认的关联规则页面。
  if (to.path === '/login' && loggedIn) return '/association'
  return true
})

export default router
