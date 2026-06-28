<template>
  <div class="merchant-dashboard">
    <div class="page-header">
      <h2>控制台</h2>
      <p class="greeting">欢迎回来，{{ storeName }}</p>
    </div>
    <el-row :gutter="20" class="stat-cards">
      <el-col :xs="24" :sm="12" :md="6" v-for="stat in stats" :key="stat.title">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-title">{{ stat.title }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getDashboard } from '@/api/merchant/dashboard'

const storeName = ref('')
const stats = ref([
  { title: '今日订单数', value: 0 },
  { title: '今日销售额', value: '¥0.00' },
  { title: '待发货', value: 0 },
  { title: '待处理售后', value: 0 }
])

onMounted(() => {
  const merchant = localStorage.getItem('merchantInfo')
  if (merchant) {
    storeName.value = JSON.parse(merchant).name || ''
  }
  getDashboard().then(res => {
    if (res.data) {
      stats.value[0].value = res.data.todayOrders ?? 0
      stats.value[1].value = '¥' + (res.data.todaySales ?? 0).toFixed(2)
      stats.value[2].value = res.data.pendingDelivery ?? 0
      stats.value[3].value = res.data.pendingAfterSale ?? 0
    }
  })
})
</script>

<style scoped>
.page-header { margin-bottom: 24px; }
.page-header h2 { margin: 0 0 8px; }
.greeting { color: #909399; margin: 0; font-size: 14px; }
.stat-card { text-align: center; margin-bottom: 16px; }
.stat-value { font-size: 32px; font-weight: bold; color: #409eff; margin-bottom: 8px; }
.stat-title { font-size: 14px; color: #909399; }
</style>
