import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import './styles.css'

function isResizeObserverNoise(message) {
  return typeof message === 'string'
    && message.includes('ResizeObserver loop completed with undelivered notifications')
}

window.addEventListener('error', (event) => {
  if (isResizeObserverNoise(event.message)) {
    event.stopImmediatePropagation()
  }
})

createApp(App).use(ElementPlus).use(router).mount('#app')
