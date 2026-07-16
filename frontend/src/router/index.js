import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import AssociationView from '../views/AssociationView.vue'
import ClusteringView from '../views/ClusteringView.vue'
import ClassificationView from '../views/ClassificationView.vue'
import RegressionView from '../views/RegressionView.vue'
import { isLoggedIn } from '../auth'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: LoginView },
  { path: '/association', component: AssociationView },
  { path: '/clustering', component: ClusteringView },
  { path: '/classification', component: ClassificationView },
  { path: '/regression', component: RegressionView },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const loggedIn = isLoggedIn()
  if (to.path !== '/login' && !loggedIn) return '/login'
  if (to.path === '/login' && loggedIn) return '/association'
  return true
})

export default router
