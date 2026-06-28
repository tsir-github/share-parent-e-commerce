<template>
  <!--
  登录页面
  整个流程的"入口"——用户看到的第一页。
  -->
  <div class="login">
    <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="login-form">
      <h3 class="title">小区电商管理平台</h3>

      <!-- 账号输入框 -->
      <el-form-item prop="username">
        <el-input
          v-model="loginForm.username"
          type="text"
          size="large"
          auto-complete="off"
          placeholder="账号"
        >
          <template #prefix><svg-icon icon-class="user" class="el-input__icon input-icon" /></template>
        </el-input>
      </el-form-item>

      <!-- 密码输入框（支持回车提交） -->
      <el-form-item prop="password">
        <el-input
          v-model="loginForm.password"
          type="password"
          size="large"
          auto-complete="off"
          placeholder="密码"
          @keyup.enter="handleLogin"
        >
          <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
        </el-input>
      </el-form-item>

      <!-- 验证码输入框（v-if="captchaEnabled"：验证码可关闭） -->
      <el-form-item prop="code" v-if="captchaEnabled">
        <el-input
          v-model="loginForm.code"
          size="large"
          auto-complete="off"
          placeholder="验证码"
          style="width: 63%"
          @keyup.enter="handleLogin"
        >
          <template #prefix><svg-icon icon-class="validCode" class="el-input__icon input-icon" /></template>
        </el-input>
        <!-- 验证码图片，点击可刷新 -->
        <div class="login-code">
          <img :src="codeUrl" @click="getCode" class="login-code-img"/>
        </div>
      </el-form-item>

      <!-- 记住密码（存 Cookie，30 天有效） -->
      <el-checkbox v-model="loginForm.rememberMe" style="margin:0px 0px 25px 0px;">记住密码</el-checkbox>

      <!-- 登录按钮 -->
      <el-form-item style="width:100%;">
        <el-button
          :loading="loading"
          size="large"
          type="primary"
          style="width:100%;"
          @click.prevent="handleLogin"
        >
          <span v-if="!loading">登 录</span>
          <span v-else>登 录 中...</span>
        </el-button>
        <!-- 注册链接（v-if="register" 可配置） -->
        <div style="float: right;" v-if="register">
          <router-link class="link-type" :to="'/register'">立即注册</router-link>
        </div>
      </el-form-item>
    </el-form>
    <!--  底部版权信息  -->
    <div class="el-login-footer">
      <span>Copyright © 2018-2023 ruoyi.vip All Rights Reserved.</span>
    </div>
  </div>
</template>

<script setup>
/**
 * ★ 登录页面的逻辑脚本
 *
 * 这是"登录流程"的最前端触发点。
 * 用户填好账号密码验证码，点"登录"，从这里开始一路走到后端。
 *
 * 完整的调用链：
 *   login.vue → userStore.login() → api/login.js login() → request.js 拦截器 → axios POST /auth/login
 */

import { getCodeImg } from "@/api/login";
import Cookies from "js-cookie";
import { encrypt, decrypt } from "@/utils/jsencrypt";
import useUserStore from '@/store/modules/user'

const userStore = useUserStore()
const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();

// ★ 登录表单数据
// 默认值 admin / admin123（方便开发调试）
const loginForm = ref({
  username: "admin",
  password: "admin123",
  rememberMe: false,  // 记住密码
  code: "",            // 验证码
  uuid: ""             // 验证码在 Redis 中的 key
});

// ★ 表单校验规则
// 三个字段都是必填的
const loginRules = {
  username: [{ required: true, trigger: "blur", message: "请输入您的账号" }],
  password: [{ required: true, trigger: "blur", message: "请输入您的密码" }],
  code: [{ required: true, trigger: "change", message: "请输入验证码" }]
};

const codeUrl = ref("");      // 验证码图片的 base64 URL
const loading = ref(false);   // 登录中状态（按钮显示"登录中..."）
const captchaEnabled = ref(true);  // 验证码开关（后端可关闭）
const register = ref(false);       // 注册开关
const redirect = ref(undefined);   // 登录成功后要跳转的路由

// ★ 监听路由参数中的 redirect
// 比如未登录访问 /system/user，被路由守卫拦到登录页，带上 ?redirect=/system/user
watch(route, (newRoute) => {
    redirect.value = newRoute.query && newRoute.query.redirect;
}, { immediate: true });

/**
 * ★ 点击"登录"按钮
 *
 * 流程：
 *   1. 表单校验
 *   2. 如果勾了"记住密码"→ 加密密码存 Cookie（30 天）
 *   3. 调 userStore.login() → 发 POST /auth/login
 *   4. 登录成功 → 跳转到 redirect 指定的页面（或首页）
 *   5. 登录失败 → 取消加载状态，刷新验证码
 */
function handleLogin() {
  proxy.$refs.loginRef.validate(valid => {
    if (valid) {
      loading.value = true;

      // ★ 记住密码：用户名 + 加密后的密码存入 Cookie
      if (loginForm.value.rememberMe) {
        Cookies.set("username", loginForm.value.username, { expires: 30 });
        Cookies.set("password", encrypt(loginForm.value.password), { expires: 30 });
        Cookies.set("rememberMe", loginForm.value.rememberMe, { expires: 30 });
      } else {
        Cookies.remove("username");
        Cookies.remove("password");
        Cookies.remove("rememberMe");
      }

      // ★ 调用 Pinia store 的 login action
      // store 会调 api/login.js → 发 POST 请求到后端
      userStore.login(loginForm.value).then(() => {
        // 登录成功 → 跳转（保留 redirect 之外的 query 参数）
        const query = route.query;
        const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
          if (cur !== "redirect") {
            acc[cur] = query[cur];
          }
          return acc;
        }, {});
        router.push({ path: redirect.value || "/", query: otherQueryParams });
      }).catch(() => {
        // 登录失败 → 取消加载状态 + 刷新验证码
        loading.value = false;
        if (captchaEnabled.value) {
          getCode();
        }
      });
    }
  });
}

/**
 * 获取验证码图片
 * GET /code → { img: base64, uuid: key }
 */
function getCode() {
  getCodeImg().then(res => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled;
    if (captchaEnabled.value) {
      codeUrl.value = "data:image/gif;base64," + res.img;
      loginForm.value.uuid = res.uuid;
    }
  });
}

/**
 * 从 Cookie 中回填记住的用户名密码
 * 页面加载时执行
 */
function getCookie() {
  const username = Cookies.get("username");
  const password = Cookies.get("password");
  const rememberMe = Cookies.get("rememberMe");
  loginForm.value = {
    username: username === undefined ? loginForm.value.username : username,
    password: password === undefined ? loginForm.value.password : decrypt(password),
    rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
  };
}

// ★ 页面初始化：获取验证码 + 回填记住的密码
getCode();
getCookie();
</script>

<style lang='scss' scoped>
.login {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  background-image: url("../assets/images/login-background.jpg");
  background-size: cover;
}
.title {
  margin: 0px auto 30px auto;
  text-align: center;
  color: #707070;
}

.login-form {
  border-radius: 6px;
  background: #ffffff;
  width: 400px;
  padding: 25px 25px 5px 25px;
  .el-input {
    height: 40px;
    input {
      height: 40px;
    }
  }
  .input-icon {
    height: 39px;
    width: 14px;
    margin-left: 0px;
  }
}
.login-tip {
  font-size: 13px;
  text-align: center;
  color: #bfbfbf;
}
.login-code {
  width: 33%;
  height: 40px;
  float: right;
  img {
    cursor: pointer;
    vertical-align: middle;
  }
}
.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: #fff;
  font-family: Arial;
  font-size: 12px;
  letter-spacing: 1px;
}
.login-code-img {
  height: 40px;
  padding-left: 12px;
}
</style>
