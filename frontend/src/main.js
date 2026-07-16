import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import './styles.css'

function isResizeObserverNoise(message) {
  return typeof message === 'string'
    && (
      message.includes('ResizeObserver loop completed with undelivered notifications')
      || message.includes('ResizeObserver loop limit exceeded')
    )
}

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

createApp(App).use(ElementPlus).use(router).mount('#app')
