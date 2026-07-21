<template>
  <RouterView v-if="$route.path === '/login'" />
  <el-container v-else class="app-shell">
    <el-aside width="248px" class="sidebar">
      <div class="brand">
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
    </el-aside>

    <el-container class="main-panel">
      <el-header class="topbar">
        <div class="topbar-title">
          <h1>数据挖掘与分析综合实验平台</h1>
        </div>
        <div class="topbar-actions">
          <div v-if="currentUser.username" class="current-user">
            <el-icon class="current-user-icon"><UserFilled /></el-icon>
            <span>当前用户</span>
            <strong>{{ currentUser.username }}</strong>
          </div>
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

    <el-dialog v-model="authorDialogVisible" title="小组成员" width="420px">
      <el-table :data="authors" border>
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="studentId" label="学号" />
      </el-table>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { UserFilled } from '@element-plus/icons-vue'
import { getCurrentUser, logout as clearLogin } from './auth'

const router = useRouter()
const route = useRoute()

const currentUser = computed(() => {
  route.path
  return getCurrentUser()
})

const authorDialogVisible = ref(false)
const authors = [
  { name: '顾林奕', studentId: '038123011' },
  { name: '张秋彤', studentId: '028123236' },
  { name: '张晨琳', studentId: '028123173' },
]

function logout() {
  clearLogin()
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

.app-shell {
  min-height: 100vh;
}

.main-panel {
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

.side-menu .el-menu-item {
  color: #dbe4ef;
  border-radius: 6px;
}

.side-menu .el-menu-item.is-active {
  background: #2a9d8f;
  color: #fff;
}

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
