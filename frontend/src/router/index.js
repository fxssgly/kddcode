// 文件作用：路由配置层，负责把浏览器地址映射到具体页面组件，并在进入页面前做登录校验。
// 关联文件：main.js 通过 app.use(router) 注册这里；App.vue 通过 RouterView 渲染当前路由对应的页面。
import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import AssociationView from '../views/AssociationView.vue'
import ClusteringView from '../views/ClusteringView.vue'
import ClassificationView from '../views/ClassificationView.vue'
import RegressionView from '../views/RegressionView.vue'
import { isLoggedIn } from '../auth'

// routes 是路由表：path 是浏览器地址，component 是这个地址要显示的 Vue 页面组件。
const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: LoginView },
  { path: '/association', component: AssociationView },
  { path: '/clustering', component: ClusteringView },
  { path: '/classification', component: ClassificationView },
  { path: '/regression', component: RegressionView },
]

// createWebHistory 使用 HTML5 history 模式，地址栏显示 /login 这类正常路径。
// routes 注入后，RouterView 才知道当前路径应该渲染哪个页面。
const router = createRouter({
  history: createWebHistory(),
  routes,
})

// beforeEach 是全局路由守卫：每次页面跳转前都会先执行这里。
// 返回字符串表示重定向到指定路径；返回 true 表示允许继续进入目标页面。
router.beforeEach((to) => {
  const loggedIn = isLoggedIn()
  // 未登录访问实验页时强制回登录页，避免直接输入 /association 绕过登录。
  if (to.path !== '/login' && !loggedIn) return '/login'
  // 已登录再访问 /login 时跳到默认实验页，避免重复登录。
  if (to.path === '/login' && loggedIn) return '/association'
  return true
})

// 导出 router 给 main.js 注册到整个 Vue 应用。
export default router
