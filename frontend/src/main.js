// 文件作用：前端入口层，负责创建 Vue 应用、注册 Element Plus 和路由，并挂载到 public/index.html 的 #app。
// 关联文件：App.vue 提供根组件；router/index.js 决定 URL 对应的页面；Element Plus 提供 el-* 组件和样式。
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'

// isResizeObserverNoise 只识别 Element Plus/图表布局变化时常见的 ResizeObserver 噪声。
// ResizeObserver = 浏览器监听元素尺寸变化的机制；这些提示通常不影响业务，所以统一在入口层屏蔽。
function isResizeObserverNoise(message) {
  return typeof message === 'string'
    && (
      message.includes('ResizeObserver loop completed with undelivered notifications')
      || message.includes('ResizeObserver loop limit exceeded')
    )
}

// 同步错误和 Promise 异常都可能携带 ResizeObserver 提示；这里只拦截这类已知噪声。
window.addEventListener('error', (event) => {
  if (isResizeObserverNoise(event.message)) {
    event.preventDefault()
    event.stopImmediatePropagation()
  }
}, true)

window.addEventListener('unhandledrejection', (event) => {
  if (isResizeObserverNoise(event.reason?.message || event.reason)) {
    event.preventDefault()
    event.stopImmediatePropagation()
  }
}, true)

// 启动流程：创建应用实例 → 注册组件库 → 注册路由 → 挂载到页面 DOM。
createApp(App).use(ElementPlus).use(router).mount('#app')
