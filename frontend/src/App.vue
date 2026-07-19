<!--
  文件作用：App.vue 是根组件，属于应用外壳层。
  它负责区分登录页和系统主界面：/login 只渲染登录页面，其它路由渲染侧边栏、顶部栏和内容区。
  关联文件：main.js 把它挂载为根组件；router/index.js 提供 $route 和 RouterView；auth.js 提供当前用户和退出登录逻辑。
-->
<template>
  <!-- RouterView = 路由出口；当前地址匹配哪个页面组件，这里就渲染哪个组件。 -->
  <RouterView v-if="$route.path === '/login'" />
  <el-container v-else class="app-shell">
    <el-aside width="248px" class="sidebar">
      <div class="brand">
        <div>
          <strong>数据挖掘综合实验</strong>
        </div>
      </div>

      <!-- router 属性让 el-menu-item 的 index 变成路由地址；点击菜单会直接切换页面。 -->
      <el-menu router :default-active="$route.path" class="side-menu">
        <el-menu-item index="/association">关联规则</el-menu-item>
        <el-menu-item index="/clustering">聚类分析</el-menu-item>
        <el-menu-item index="/classification">分类分析</el-menu-item>
        <el-menu-item index="/regression">回归分析</el-menu-item>
      </el-menu>

    </el-aside>

    <el-container class="main-panel">
      <el-header class="topbar">
        <div class="topbar-title">
          <h1>数据挖掘与分析综合实验平台</h1>
        </div>
        <div class="topbar-actions">
          <!-- v-if 根据当前用户是否存在决定是否显示用户名，避免未登录时渲染空用户信息。 -->
          <div v-if="currentUser.username" class="current-user">
            <el-icon class="current-user-icon"><UserFilled /></el-icon>
            <span>当前用户</span>
            <strong>{{ currentUser.username }}</strong>
          </div>
          <!-- @click 绑定点击事件；这里直接修改 ref 变量，弹窗会因为 v-model 自动打开。 -->
          <el-button type="primary" @click="authorDialogVisible = true">
            联系作者
          </el-button>
          <el-button plain @click="logout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="content">
        <RouterView />
      </el-main>
      <el-footer class="app-footer">
        ©2026 Created by 疯狂星期五
      </el-footer>
    </el-container>

    <!-- v-model 双向绑定弹窗显示状态：按钮把它改成 true，关闭弹窗时 Element Plus 会改回 false。 -->
    <el-dialog v-model="authorDialogVisible" title="小组成员" width="420px">
      <el-table :data="authors" border>
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="studentId" label="学号" />
      </el-table>
    </el-dialog>
  </el-container>
</template>

<script setup>
// script setup 是 Vue3 单文件组件语法，里面声明的变量和函数可以直接给 template 使用。
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { UserFilled } from '@element-plus/icons-vue'
import { getCurrentUser, logout as clearLogin } from './auth'

// router 用来主动跳转页面；route 用来读取当前页面地址。
const router = useRouter()
const route = useRoute()

// currentUser 使用 computed，是为了在路由变化时重新从 localStorage 读取登录用户。
// route.path 这一行虽然不参与返回值，但会让 computed 依赖当前路由，从而在跳转后重新计算。
const currentUser = computed(() => {
  route.path
  return getCurrentUser()
})

// ref 创建响应式布尔值，控制“联系作者”弹窗的打开和关闭。
const authorDialogVisible = ref(false)

// authors 是固定展示数据，不需要响应式；模板中的表格直接读取这个数组。
const authors = [
  { name: '顾林奕', studentId: '038123011' },
  { name: '张秋彤', studentId: '028123236' },
  { name: '张晨琳', studentId: '028123173' },
]

// 退出流程：清除本地登录态 → 跳转回 /login；路由守卫会继续阻止未登录用户进入实验页。
function logout() {
  clearLogin()
  router.push('/login')
}
</script>

<style>
/* 全局盒模型，避免 padding/border 把元素撑出预期宽度。 */
* {
  box-sizing: border-box;
}

/* 全局页面底色和默认字体。 */
body {
  margin: 0;
  background: #f5f7fb;
  color: #1f2937;
  font-family: "Microsoft YaHei", Arial, sans-serif;
}

/* 全局按钮风格：统一尺寸、圆角、字重和交互反馈。 */
.el-button {
  min-height: 34px;
  padding: 8px 16px;
  border-radius: 8px;
  border-color: #d6dee9;
  background: #fff;
  color: #334155;
  font-weight: 600;
  letter-spacing: 0;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.06);
  transition: background-color 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.el-button:hover,
.el-button:focus {
  border-color: #2a9d8f;
  background: #f0fdfa;
  color: #1f7f74;
  box-shadow: 0 4px 10px rgba(42, 157, 143, 0.14);
  transform: translateY(-1px);
}

.el-button:active {
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.1);
  transform: translateY(0);
}

.el-button.is-disabled,
.el-button.is-disabled:hover,
.el-button.is-disabled:focus {
  border-color: #e5e7eb;
  background: #f8fafc;
  color: #94a3b8;
  box-shadow: none;
  transform: none;
}

.el-button--primary {
  border-color: #2a9d8f;
  background: #2a9d8f;
  color: #fff;
}

.el-button--primary:hover,
.el-button--primary:focus {
  border-color: #23877c;
  background: #23877c;
  color: #fff;
}

.el-button--success {
  border-color: #3b82f6;
  background: #3b82f6;
  color: #fff;
}

.el-button--success:hover,
.el-button--success:focus {
  border-color: #2563eb;
  background: #2563eb;
  color: #fff;
}

.el-button.is-plain {
  border-color: #cbd5e1;
  background: #f8fafc;
  color: #475569;
}

.el-button.is-plain:hover,
.el-button.is-plain:focus {
  border-color: #2a9d8f;
  background: #ecfdf5;
  color: #1f7f74;
}

/* 登录页使用全屏网格居中。 */
.login-page {
  display: grid;
  min-height: 100vh;
  place-items: center;
  padding: 24px;
  background: #eef3f8;
}

/* 登录卡片宽度限制，兼容窄屏。 */
.login-card {
  width: min(430px, 100%);
  border-radius: 8px;
}

.login-switch,
.login-alert {
  margin: 16px 0;
}

.login-button {
  width: 100%;
}

.app-shell {
  min-height: 100vh;
}

.main-panel {
  min-height: 100vh;
}

/* 侧边栏使用深色背景，并承担菜单和底部操作按钮。 */
.sidebar {
  position: relative;
  padding: 22px 16px;
  background: #14213d;
  color: #fff;
}

/* 品牌区横向排列 logo 与标题。 */
.brand {
  display: flex;
  align-items: center;
  position: relative;
  padding: 8px 10px 22px 18px;
}

.brand::before {
  content: "";
  position: absolute;
  left: 8px;
  top: 9px;
  width: 4px;
  height: 25px;
  border-radius: 999px;
  background: #2a9d8f;
}

.brand strong {
  display: block;
  color: #f8fafc;
  font-size: 18px;
  font-weight: 700;
  line-height: 1.35;
}

.brand small {
  display: block;
  margin-top: 5px;
  color: #cbd5e1;
  line-height: 1.35;
}

.side-menu {
  border-right: 0;
  background: transparent;
}

/* 覆盖 Element Plus 菜单默认色，使其融入深色侧栏。 */
.side-menu .el-menu-item {
  color: #dbe4ef;
  border-radius: 6px;
}

.side-menu .el-menu-item.is-active {
  background: #2a9d8f;
  color: #fff;
}

/* 顶部栏承载当前系统标题。 */
.topbar {
  display: flex;
  gap: 16px;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  height: auto;
  padding: 22px 28px 14px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.topbar h1 {
  margin: 0;
  font-size: 23px;
}

.topbar-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.current-user {
  display: flex;
  gap: 10px;
  align-items: center;
  min-height: 40px;
  padding: 6px 12px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #f8fafc;
  color: #1f2937;
}

.current-user-icon {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border-radius: 50%;
  background: #2a9d8f;
  color: #fff;
  font-size: 16px;
}

.current-user span {
  color: #64748b;
  font-size: 12px;
}

.current-user strong {
  max-width: 140px;
  overflow: hidden;
  color: #111827;
  font-size: 14px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.topbar-actions .el-button + .el-button {
  margin-left: 0;
}

.topbar p {
  margin: 0;
  color: #64748b;
}

.content {
  flex: 1;
  padding: 22px 28px 28px;
}

.app-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 48px;
  padding: 0 28px;
  color: #64748b;
  font-size: 13px;
}

/* 每个实验页通用标题样式。 */
.page-title {
  display: flex;
  gap: 22px;
  align-items: center;
  min-height: 44px;
}

.page-title strong {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
}

.page-title span {
  padding-left: 22px;
  border-left: 1px solid #dcdfe6;
  font-size: 24px;
  color: #1f2937;
}

.eyebrow {
  margin: 0 0 8px;
  color: #2a9d8f;
  font-weight: 700;
}

.description {
  color: #64748b;
  line-height: 1.7;
}

.toolbar-card,
.work-card {
  border-radius: 8px;
}

/* 工具栏内联表单不保留默认底部间距，减少顶部操作区高度。 */
.toolbar-card .el-form-item {
  margin-bottom: 0;
}

.toolbar-card .el-upload {
  margin-left: 8px;
}

/* 常见的左右两栏布局：表格 + 图表。 */
.two-column {
  display: grid;
  grid-template-columns: minmax(420px, 1fr) minmax(420px, 1fr);
  gap: 16px;
  align-items: stretch;
}

/* 卡片头部左右分布，标题在左，指标/标签在右。 */
.card-header {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
}

/* ECharts 容器必须有明确高度，否则图表无法正常计算尺寸。 */
.chart {
  width: 100%;
  height: 360px;
}

/* 窄屏下把双栏改成单栏，避免内容被挤压。 */
@media (max-width: 1100px) {
  .two-column {
    grid-template-columns: 1fr;
  }
}
</style>
