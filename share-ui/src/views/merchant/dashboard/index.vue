<template>
  <div class="app-container">
    <div class="page-header">
      <h2>控制台</h2>
      <p class="greeting">欢迎回来，{{ storeName }}</p>
    </div>

    <el-row :gutter="10" class="mb8"><right-toolbar :search="false" @queryTable="fetchData" /></el-row>

    <el-row :gutter="16" class="stat-cards">
      <el-col :xs="24" :sm="12" :md="8" v-for="stat in stats" :key="stat.title">
        <el-card shadow="hover" class="stat-card" :style="{ borderTop: '3px solid ' + stat.color }">
          <div class="stat-icon"><el-icon :size="22"><component :is="stat.icon" /></el-icon></div>
          <div class="stat-value" :style="{ color: stat.color }">{{ stat.value }}</div>
          <div class="stat-title">{{ stat.title }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="14">
        <el-card shadow="never" class="mb8">
          <template #header><div style="display:flex;justify-content:space-between"><span style="font-weight:600">近期订单</span><el-button text size="small" @click="$router.push('/merchant/order')">查看全部 →</el-button></div></template>
          <el-table :data="recentOrders" size="small" v-loading="loadingOrders">
            <el-table-column prop="orderNo" label="订单号" width="180" />
            <el-table-column label="金额" width="80" align="right"><template #default="{row}">¥{{ row.totalAmount }}</template></el-table-column>
            <el-table-column label="状态" width="80" align="center"><template #default="{row}"><el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag></template></el-table-column>
            <el-table-column prop="receiverName" label="收货人" width="80" />
            <el-table-column label="时间" min-width="130"><template #default="{row}">{{ row.createTime?.substring(0,16) }}</template></el-table-column>
          </el-table>
          <div v-if="!recentOrders.length" style="text-align:center;color:#909399;padding:20px">暂无订单</div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never" class="mb8">
          <template #header><span style="font-weight:600">快捷操作</span></template>
          <el-row :gutter="8">
            <el-col :span="12" style="margin-bottom:8px"><el-button icon="Plus" @click="$router.push('/merchant/product/add')" style="width:100%">新增商品</el-button></el-col>
            <el-col :span="12" style="margin-bottom:8px"><el-button icon="List" @click="$router.push('/merchant/order')" style="width:100%">订单管理</el-button></el-col>
            <el-col :span="12"><el-button icon="Discount" @click="$router.push('/merchant/coupon')" style="width:100%">优惠券</el-button></el-col>
            <el-col :span="12"><el-button icon="Setting" @click="$router.push('/merchant/profile')" style="width:100%">店铺设置</el-button></el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="MerchantDashboard">
import { ref, onMounted } from 'vue'
import { getDashboard } from '@/api/merchant/dashboard'
import { listOrder } from '@/api/merchant/order'
import RightToolbar from '@/components/RightToolbar'

const storeName = ref('')
const recentOrders = ref([])
const loadingOrders = ref(false)
const stats = ref([
  { title: '今日订单数', value: 0, icon: 'Document', color: '#409eff' },
  { title: '今日销售额', value: '¥0.00', icon: 'Money', color: '#67c23a' },
  { title: '待发货', value: 0, icon: 'Van', color: '#e6a23c' },
  { title: '待处理售后', value: 0, icon: 'WarningFilled', color: '#f56c6c' },
  { title: '在售商品', value: 0, icon: 'Goods', color: '#409eff' },
  { title: '优惠券', value: 0, icon: 'Discount', color: '#e6a23c' }
])
function statusType(s) { return { '0':'danger','1':'warning','2':'primary','3':'success','4':'info','5':'danger' }[s]||'info' }
function statusLabel(s) { return { '0':'待支付','1':'待发货','2':'配送中','3':'已完成','4':'已取消','5':'售后中' }[s]||s }

function fetchData() {
  getDashboard().then(res => {
    if (res.data) {
      stats.value[0].value = res.data.todayOrders ?? 0
      stats.value[1].value = '¥' + (res.data.todaySales ?? 0).toFixed(2)
      stats.value[2].value = res.data.pendingDelivery ?? 0
      stats.value[3].value = res.data.pendingAfterSale ?? 0
    }
  })
  loadingOrders.value = true
  listOrder().then(res => { recentOrders.value = (res.data?.rows||res.data||[]).slice(0,5) }).finally(()=>loadingOrders.value=false)
}

onMounted(() => {
  const mi = localStorage.getItem('merchantInfo')
  if (mi) storeName.value = JSON.parse(mi).name || ''
  fetchData()
})
</script>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0 0 4px; }
.greeting { color: #909399; margin: 0; font-size: 14px; }
.stat-card { text-align: center; margin-bottom: 16px; cursor: default; }
.stat-icon { margin-bottom: 6px; }
.stat-value { font-size: 24px; font-weight: bold; margin-bottom: 2px; }
.stat-title { font-size: 12px; color: #909399; }
</style>
