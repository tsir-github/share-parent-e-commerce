<template>
  <div class="app-container">
    <el-card>
      <template #header><span>商家个人中心</span></template>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="店铺信息" name="store">
          <el-form :model="storeForm" ref="storeRef" :rules="storeRules" label-width="100px" style="max-width:600px">
            <el-form-item label="店铺名称" prop="name">
              <el-input v-model="storeForm.name" maxlength="100" />
            </el-form-item>
            <el-form-item label="联系人" prop="contactName">
              <el-input v-model="storeForm.contactName" maxlength="50" />
            </el-form-item>
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="storeForm.contactPhone" maxlength="20" />
            </el-form-item>
            <el-form-item label="店铺地址" prop="address">
              <el-input v-model="storeForm.address" maxlength="200" />
            </el-form-item>
            <el-form-item label="店铺描述" prop="description">
              <el-input v-model="storeForm.description" type="textarea" :rows="3" maxlength="500" />
            </el-form-item>
            <el-form-item label="店铺Logo">
              <el-upload :action="logoUploadUrl" :headers="uploadHeaders" :before-upload="beforeLogoUpload" :on-success="onLogoSuccess" :show-file-list="false" accept="image/*" name="file">
                <img v-if="storeForm.logo" :src="storeForm.logo" class="logo-preview" />
                <el-icon v-else class="logo-uploader-icon"><Plus /></el-icon>
              </el-upload>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="storeSaving" @click="saveStore">保存店铺信息</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="个人资料" name="user">
          <el-form :model="userForm" ref="userRef" label-width="100px" style="max-width:450px">
            <el-form-item label="登录账号">
              <el-input :model-value="merchantUsername" disabled />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="userForm.phone" maxlength="20" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="userForm.email" maxlength="64" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="userSaving" @click="saveUser">保存个人资料</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="修改密码" name="pwd">
          <el-form ref="pwdRef" :model="pwdForm" :rules="pwdRules" label-width="100px" style="max-width:450px">
            <el-form-item label="旧密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入旧密码" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请确认新密码" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="pwdSaving" @click="savePwd">保存密码</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup name="MerchantProfilePage">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getProfile, updateProfile, updatePassword, updateUserInfo, getUserInfo, saveLogo } from '@/api/merchant/dashboard'
import { getToken } from '@/utils/auth'
import useUserStore from '@/store/modules/user'

const activeTab = ref('store')
const storeSaving = ref(false)
const userSaving = ref(false)
const pwdSaving = ref(false)
const merchantUsername = ref('')

const storeForm = ref({ name: '', contactName: '', contactPhone: '', address: '', description: '', logo: '' })
const userForm = ref({ phone: '', email: '' })
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })

const storeRules = {
  name: [{ required: true, message: '店铺名称不能为空', trigger: 'blur' }],
  contactName: [{ required: true, message: '联系人不能为空', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '联系电话不能为空', trigger: 'blur' }]
}
const equalToPwd = (rule, value, callback) => {
  if (pwdForm.value.newPassword !== value) callback(new Error('两次输入的密码不一致'))
  else callback()
}
const pwdRules = {
  oldPassword: [{ required: true, message: '旧密码不能为空', trigger: 'blur' }],
  newPassword: [{ required: true, message: '新密码不能为空', trigger: 'blur' }, { min: 6, max: 20, message: '长度6-20个字符', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '确认密码不能为空', trigger: 'blur' }, { validator: equalToPwd, trigger: 'blur' }]
}

const logoUploadUrl = computed(() => '/file-upload/upload')
const uploadHeaders = computed(() => ({ Authorization: 'Bearer ' + getToken() }))

function beforeLogoUpload(file) {
  if (!file.type.startsWith('image/')) { ElMessage.error('只能上传图片文件'); return false }
  if (file.size > 10 * 1024 * 1024) { ElMessage.error('图片大小不能超过10MB'); return false }
  return true
}
function onLogoSuccess(res) {
  if (res.data?.url) {
    storeForm.value.logo = res.data.url
    useUserStore().avatar = res.data.url
    const mi = JSON.parse(localStorage.getItem('merchantInfo') || '{}')
    mi.logo = res.data.url
    localStorage.setItem('merchantInfo', JSON.stringify(mi))
    saveLogo(res.data.url).then(() => ElMessage.success('Logo已保存'))
  }
}

function saveStore() {
  storeSaving.value = true
  const { logo, ...data } = storeForm.value
  updateProfile(data).then(() => ElMessage.success('店铺信息已保存')).finally(() => { storeSaving.value = false })
}
function saveUser() {
  userSaving.value = true
  updateUserInfo({ phone: userForm.value.phone, email: userForm.value.email })
    .then(() => ElMessage.success('个人资料已保存')).finally(() => { userSaving.value = false })
}
function savePwd() {
  pwdSaving.value = true
  updatePassword({ oldPassword: pwdForm.value.oldPassword, newPassword: pwdForm.value.newPassword })
    .then(() => { ElMessage.success('密码已修改'); pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' } })
    .finally(() => { pwdSaving.value = false })
}

onMounted(() => {
  getProfile().then(res => {
    if (res.data) {
      const d = res.data
      storeForm.value = { name: d.name || '', contactName: d.contactName || '', contactPhone: d.contactPhone || '', address: d.address || '', description: d.description || '', logo: d.logo || '' }
      if (d.logo) {
        useUserStore().avatar = d.logo
        const mi = JSON.parse(localStorage.getItem('merchantInfo') || '{}')
        mi.logo = d.logo
        localStorage.setItem('merchantInfo', JSON.stringify(mi))
      }
    }
  })
  getUserInfo().then(res => {
    if (res.data) userForm.value = { phone: res.data.phone || '', email: res.data.email || '' }
  })
  const mi = localStorage.getItem('merchantInfo')
  if (mi) merchantUsername.value = JSON.parse(mi).name || ''
})
</script>

<style scoped>
.logo-preview { width: 80px; height: 80px; border-radius: 6px; object-fit: cover; border: 1px solid #dcdfe6; }
.logo-uploader-icon { font-size: 28px; color: #c0c4cc; width: 80px; height: 80px; line-height: 80px; text-align: center; border: 1px dashed #dcdfe6; border-radius: 6px; cursor: pointer; }
.logo-uploader-icon:hover { border-color: #409eff; color: #409eff; }
</style>
