<!--
  文件作用：定义前端应用的整体外壳，包括侧边栏、顶部栏、主内容区和作者弹窗。
  项目位置：Vue 根组件，所有实验页面都会在它的 RouterView 中显示。
  交互关系：依赖 vue-router 切换页面，依赖 auth.js 完成退出登录，子页面通过路由被嵌入主内容区。
-->
<template>
  <!-- 登录页单独渲染，不使用后台主框架，这样登录界面可以居中显示。 -->
  <RouterView v-if="$route.path === '/login'" />
  <el-container v-else class="app-shell">
    <el-aside width="248px" class="sidebar">
      <div class="brand">
        <span class="brand-mark">KDD</span>
        <div>
          <!-- 系统品牌区：固定展示课程/实验平台名称。 -->
          <strong>数据挖掘综合实验</strong>
        </div>
      </div>

      <!-- router 属性让菜单项点击后直接跳转到对应路由，default-active 用当前地址高亮菜单。 -->
      <el-menu router :default-active="$route.path" class="side-menu">
        <el-menu-item index="/association">关联规则</el-menu-item>
        <el-menu-item index="/clustering">聚类分析</el-menu-item>
        <el-menu-item index="/classification">分类分析</el-menu-item>
        <el-menu-item index="/regression">回归分析</el-menu-item>
      </el-menu>

    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div class="topbar-title">
          <h1>数据挖掘与分析综合实验平台</h1>
        </div>
        <div class="topbar-actions">
          <!-- 弹窗开关由 authorDialogVisible 控制。 -->
          <el-button type="primary" @click="authorDialogVisible = true">
            联系作者
          </el-button>
          <el-button plain @click="logout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="content">
        <!-- 主内容区域：根据当前路由渲染关联、聚类、分类或回归页面。 -->
        <RouterView />
      </el-main>
    </el-container>

    <!-- 小组成员弹窗，数据来自 script 中的 authors 数组。 -->
    <el-dialog v-model="authorDialogVisible" title="小组成员" width="420px">
      <el-table :data="authors" border>
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="studentId" label="学号" />
      </el-table>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { logout as clearLogin } from './auth'

const router = useRouter()

// 控制“联系作者”弹窗是否显示。
const authorDialogVisible = ref(false)

// 小组成员信息集中放在数组中，方便表格直接渲染。
const authors = [
  { name: '顾林奕', studentId: '038123011' },
  { name: '张秋彤', studentId: '028123236' },
  { name: '张晨琳', studentId: '028123173' },
]

function logout() {
  // 先清除本地登录态，再跳转回登录页。
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
  gap: 12px;
  align-items: center;
  padding: 0 8px 20px;
}

/* KDD 标识使用固定尺寸，保证侧栏视觉稳定。 */
.brand-mark {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  border-radius: 8px;
  background: #2a9d8f;
  font-weight: 700;
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

.topbar-actions .el-button + .el-button {
  margin-left: 0;
}

.topbar p {
  margin: 0;
  color: #64748b;
}

.content {
  padding: 22px 28px 28px;
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
