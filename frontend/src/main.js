// 文件作用：创建并挂载 Vue 前端应用。
// 项目位置：前端入口文件，浏览器加载 index.html 后会从这里启动整个单页应用。
// 交互关系：挂载 Element Plus 组件库和 Vue Router；路由再决定显示登录页或各个实验页面。
//
// 逐词注释：
// import 表示“引入”；from 表示“来自哪个模块”；createApp 是 Vue 提供的“创建应用”函数。
// ElementPlus 是整套 UI 组件库；router 是页面路由器；App 是根组件；mount 是“挂载到页面节点”。

import { createApp } from 'vue' // 从 vue 包中取出 createApp，用它创建前端应用实例。
import ElementPlus from 'element-plus' // 引入 Element Plus 组件库，页面里的 el-button、el-table 等都来自这里。
import 'element-plus/dist/index.css' // 引入 Element Plus 默认样式，否则组件只有结构没有完整外观。
import App from './App.vue' // 引入根组件，整个页面外壳从 App.vue 开始渲染。
import router from './router' // 引入路由配置，负责 URL 与页面组件之间的切换。

// 判断当前错误是否只是 ResizeObserver 的浏览器提示噪声。
// Element Plus 在表格、弹窗或图表尺寸变化时，浏览器偶尔会抛出 ResizeObserver 提示。
// 这类提示不影响业务功能，但会污染开发控制台，所以这里统一识别并拦截。
function isResizeObserverNoise(message) {
  // typeof 用来确认 message 是字符串，includes 用来判断字符串里是否包含指定提示。
  return typeof message === 'string'
    && (
      message.includes('ResizeObserver loop completed with undelivered notifications')
      || message.includes('ResizeObserver loop limit exceeded')
    )
}

// 拦截同步错误事件中的 ResizeObserver 噪音，避免 Vue 开发环境把它当成真正异常展示。
window.addEventListener('error', (event) => {
  // event.message 是浏览器同步错误的文本内容。
  if (isResizeObserverNoise(event.message)) {
    // preventDefault 阻止默认报错展示；stopImmediatePropagation 阻止后续监听器继续处理这个噪声。
    event.preventDefault()
    event.stopImmediatePropagation()
  }
}, true)

// 某些浏览器会把同类问题包装成 Promise rejection，这里也做一次兜底处理。
window.addEventListener('unhandledrejection', (event) => {
  // event.reason 可能是 Error 对象，也可能只是普通字符串，所以这里用可选链兜底读取。
  if (isResizeObserverNoise(event.reason?.message || event.reason)) {
    event.preventDefault()
    event.stopImmediatePropagation()
  }
}, true)

// 创建 Vue 应用，依次挂载 Element Plus 组件库和路由，最后渲染到 public/index.html 的 #app。
createApp(App).use(ElementPlus).use(router).mount('#app')
