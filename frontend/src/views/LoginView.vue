<template>
  <main class="login-page">
    <el-card class="login-card" shadow="always">
      <p class="eyebrow">KDD System</p>
      <h1>数据挖掘与分析系统</h1>

      <el-segmented v-model="mode" :options="modeOptions" class="login-switch" />

      <el-form label-position="top">
        <el-form-item label="账号">
          <el-input v-model="username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" type="password" placeholder="请输入密码" show-password @keyup.enter="submit" />
        </el-form-item>
      </el-form>

      <el-alert v-if="message" :title="message" type="error" show-icon :closable="false" class="login-alert" />

      <el-button type="primary" class="login-button" @click="submit">
        {{ mode === 'login' ? '登录系统' : '注册账号' }}
      </el-button>
    </el-card>
  </main>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { getUsers, saveUsers, setLoggedIn } from '../auth'

const router = useRouter()
const mode = ref('login')
const modeOptions = [
  { label: '登录', value: 'login' },
  { label: '注册', value: 'register' },
]
const username = ref('')
const password = ref('')
const message = ref('')

function submit() {
  message.value = ''
  if (!username.value || !password.value) {
    message.value = '请输入账号和密码'
    return
  }
  if (mode.value === 'register') {
    register()
  } else {
    login()
  }
}

function register() {
  const users = getUsers()
  if (users.some((user) => user.username === username.value)) {
    message.value = '账号已存在'
    return
  }
  users.push({ username: username.value, password: password.value })
  saveUsers(users)
  mode.value = 'login'
  message.value = ''
}

function login() {
  const matched = getUsers().some((user) => (
    user.username === username.value && user.password === password.value
  ))
  if (!matched) {
    message.value = '账号或密码错误'
    return
  }
  setLoggedIn(username.value)
  router.push('/association')
}
</script>
