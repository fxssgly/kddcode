<template>
  <RouterView v-if="$route.path === '/login'" />
  <el-container v-else class="app-shell">
    <el-aside width="248px" class="sidebar">
      <div class="brand">
        <span class="brand-mark">KDD</span>
        <div>
          <strong>数据挖掘综合实验</strong>
        </div>
      </div>

      <el-menu router :default-active="$route.path" class="side-menu">
        <el-menu-item index="/association">关联规则</el-menu-item>
        <el-menu-item index="/clustering">聚类分析</el-menu-item>
        <el-menu-item index="/classification">分类分析</el-menu-item>
        <el-menu-item index="/regression">回归分析</el-menu-item>
      </el-menu>

      <el-button class="logout" plain @click="logout">退出登录</el-button>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div>
          <h1>数据挖掘与分析综合实验平台</h1>
        </div>
      </el-header>
      <el-main class="content">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

function logout() {
  localStorage.removeItem('kdd_logged_in')
  router.push('/login')
}
</script>

<style>
* {
  box-sizing: border-box;
}

body {
  margin: 0;
  background: #f5f7fb;
  color: #1f2937;
  font-family: "Microsoft YaHei", Arial, sans-serif;
}

.login-page {
  display: grid;
  min-height: 100vh;
  place-items: center;
  padding: 24px;
  background: #eef3f8;
}

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

.sidebar {
  position: relative;
  padding: 22px 16px;
  background: #14213d;
  color: #fff;
}

.brand {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 0 8px 20px;
}

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

.side-menu .el-menu-item {
  color: #dbe4ef;
  border-radius: 6px;
}

.side-menu .el-menu-item.is-active {
  background: #2a9d8f;
  color: #fff;
}

.logout {
  position: absolute;
  left: 16px;
  right: 16px;
  bottom: 18px;
  width: calc(100% - 32px);
}

.topbar {
  height: auto;
  padding: 22px 28px 14px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.topbar h1 {
  margin: 0 0 8px;
  font-size: 23px;
}

.topbar p {
  margin: 0;
  color: #64748b;
}

.content {
  padding: 22px 28px 28px;
}

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

.toolbar-card .el-form-item {
  margin-bottom: 0;
}

.toolbar-card .el-upload {
  margin-left: 8px;
}

.two-column {
  display: grid;
  grid-template-columns: minmax(420px, 1fr) minmax(420px, 1fr);
  gap: 16px;
  align-items: stretch;
}

.card-header {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
}

.chart {
  width: 100%;
  height: 360px;
}

@media (max-width: 1100px) {
  .two-column {
    grid-template-columns: 1fr;
  }
}
</style>
