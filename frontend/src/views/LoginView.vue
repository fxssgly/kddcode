<template>
  <main class="login-page">
    <el-card class="login-card" shadow="always">
      <p class="eyebrow">KDD System</p>
      <h1>数据挖掘与分析系统</h1>

      <el-segmented v-model="mode" :options="modeOptions" class="login-switch" />

      <el-form label-position="top">
        <el-form-item label="账号">
          <el-input v-model.trim="username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="password"
            type="password"
            placeholder="请输入密码"
            show-password
            @keyup.enter="submit"
          />
        </el-form-item>
      </el-form>

      <el-alert
        v-if="message"
        :title="message"
        :type="messageType"
        show-icon
        :closable="false"
        class="login-alert"
      />

      <el-button type="primary" class="login-button" :loading="loading" @click="submit">
        {{ mode === 'login' ? '登录系统' : '注册账号' }}
      </el-button>
    </el-card>
  </main>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { loginUser, registerUser } from '../api/request'
import { setLoggedIn } from '../auth'

const router = useRouter()
const mode = ref('login')
const modeOptions = [
  { label: '登录', value: 'login' },
  { label: '注册', value: 'register' },
]
const username = ref('')
const password = ref('')
const message = ref('')
const messageType = ref('error')
const loading = ref(false)

// 登录和注册共用同一个提交按钮，根据 mode 决定调用哪个接口。
async function submit() {
  message.value = ''
  messageType.value = 'error'
  if (!username.value || !password.value) {
    message.value = '请输入账号和密码'
    return
  }

  loading.value = true
  try {
    if (mode.value === 'register') {
      await register()
    } else {
      await login()
    }
  } catch (error) {
    message.value = error.response?.data?.message || '操作失败，请确认后端和 MySQL 已启动'
  } finally {
    loading.value = false
  }
}

// 注册成功后不直接进入系统，而是切回登录模式，让用户重新登录。
async function register() {
  await registerUser(username.value, password.value)
  mode.value = 'login'
  password.value = ''
  messageType.value = 'success'
  message.value = '注册成功，请登录'
}

// 登录成功后保存本地登录态，并跳转到默认实验页面。
async function login() {
  const response = await loginUser(username.value, password.value)
  setLoggedIn(response.data.user)
  router.push('/association')
}
</script>
