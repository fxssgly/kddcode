import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'

// 判断当前错误是否只是 ResizeObserver 的浏览器提示噪声。
// Element Plus 在表格、弹窗或图表尺寸变化时，浏览器偶尔会抛出 ResizeObserver 提示。
// 这类提示不影响业务功能，但会污染开发控制台，所以这里统一识别并拦截。
function isResizeObserverNoise(message) {
  return typeof message === 'string'
    && (
      message.includes('ResizeObserver loop completed with undelivered notifications')
      || message.includes('ResizeObserver loop limit exceeded')
    )
}

// 拦截同步错误事件中的 ResizeObserver 噪音，避免 Vue 开发环境把它当成真正异常展示。
window.addEventListener('error', (event) => {
  if (isResizeObserverNoise(event.message)) {
    event.preventDefault()
    event.stopImmediatePropagation()
  }
}, true)

// 某些浏览器会把同类问题包装成 Promise rejection，这里也做一次兜底处理。
window.addEventListener('unhandledrejection', (event) => {
  if (isResizeObserverNoise(event.reason?.message || event.reason)) {
    event.preventDefault()
    event.stopImmediatePropagation()
  }
}, true)

// 创建 Vue 应用，依次挂载 Element Plus 组件库和路由，最后渲染到 public/index.html 的 #app。
createApp(App).use(ElementPlus).use(router).mount('#app')
