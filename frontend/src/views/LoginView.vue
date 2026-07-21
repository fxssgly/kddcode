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

async function register() {
  await registerUser(username.value, password.value)
  mode.value = 'login'
  password.value = ''
  messageType.value = 'success'
  message.value = '注册成功，请登录'
}

async function login() {
  const response = await loginUser(username.value, password.value)
  setLoggedIn(response.data.user)
  router.push('/association')
}
</script>

<style scoped>
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

.eyebrow {
  margin: 0 0 8px;
  color: #2a9d8f;
  font-weight: 700;
}

h1 {
  margin: 0 0 20px;
  color: #1f2937;
  font-size: 26px;
  line-height: 1.3;
  letter-spacing: 0;
}

.login-switch,
.login-alert {
  margin: 16px 0;
}

.login-button {
  width: 100%;
}
</style>
