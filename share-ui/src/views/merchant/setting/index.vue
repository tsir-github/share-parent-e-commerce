<template>
  <div class="merchant-setting">
    <div class="page-header"><h2>店铺设置</h2></div>
    <el-card shadow="never" style="max-width:600px">
      <el-form :model="form" label-width="100px" v-loading="loading">
        <el-form-item label="店铺名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contactName" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.contactPhone" />
        </el-form-item>
        <el-form-item label="店铺地址">
          <el-input v-model="form.address" />
        </el-form-item>
        <el-form-item label="店铺描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getProfile, updateProfile } from '@/api/merchant/dashboard'

const form = ref({ name: '', contactName: '', contactPhone: '', address: '', description: '' })
const loading = ref(true)
const saving = ref(false)

function handleSave() {
  saving.value = true
  updateProfile(form.value).then(() => {
    ElMessage.success('已保存')
  }).finally(() => { saving.value = false })
}

onMounted(() => {
  getProfile().then(res => {
    if (res.data) {
      const { id, userId, status, auditRemark, auditTime, createTime, updateTime, delFlag, ...rest } = res.data
      form.value = { ...form.value, ...rest }
    }
  }).finally(() => { loading.value = false })
})
</script>
