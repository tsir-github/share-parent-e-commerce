<template>
  <div class="merchant-login">
    <el-card class="login-card" shadow="always">
      <div class="login-header">
        <h2>商家管理后台</h2>
      </div>
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        label-width="0"
        size="large"
        class="login-form"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入账号"
            prefix-icon="User"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            style="width: 100%"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { merchantLogin } from '@/api/merchant/auth'
import { setToken } from '@/utils/auth'
import useUserStore from '@/store/modules/user'

const router = useRouter()
const loginFormRef = ref()
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function handleLogin() {
  loginFormRef.value.validate((valid) => {
    if (!valid) return
    loading.value = true
    merchantLogin(loginForm.username, loginForm.password)
      .then((res) => {
        const token = res.data?.access_token
        const merchant = res.data?.merchant
        if (token) {
          setToken(token)
          if (merchant) {
            const mi = JSON.parse(localStorage.getItem('merchantInfo') || '{}')
            localStorage.setItem('merchantInfo', JSON.stringify({ ...mi, ...merchant }))
            const userStore = useUserStore()
            userStore.name = merchant.name || merchant.id
            userStore.roles = ['merchant']
            userStore.permissions = ['*:*:*']
          }
          router.push('/merchant/dashboard')
        }
      })
      .catch((err) => {
        ElMessage.error(err.msg || '登录失败')
      })
      .finally(() => {
        loading.value = false
      })
  })
}
</script>

<style scoped>
.merchant-login {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 420px;
  border-radius: 8px;
}
.login-header {
  text-align: center;
  margin-bottom: 24px;
}
.login-header h2 {
  margin: 0;
  color: #303133;
  font-size: 24px;
}
.login-form {
  padding: 0 8px;
}
</style>
