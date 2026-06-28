<template>
  <div class="merchant-order-detail">
    <div class="page-header">
      <h2>订单详情</h2>
      <el-button @click="goBack">返回</el-button>
    </div>
    <div v-loading="loading">
      <el-card v-if="order" shadow="never">
        <template #header>基础信息</template>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(order.status)">{{ statusLabel(order.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="总金额">¥{{ order.totalAmount }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ order.createTime }}</el-descriptions-item>
          <el-descriptions-item v-if="order.payTime" label="支付时间">{{ order.payTime }}</el-descriptions-item>
          <el-descriptions-item v-if="order.deliveryTime" label="发货时间">{{ order.deliveryTime }}</el-descriptions-item>
          <el-descriptions-item v-if="order.deliveryName" label="配送员">{{ order.deliveryName }}</el-descriptions-item>
          <el-descriptions-item v-if="order.deliveryPhone" label="电话">{{ order.deliveryPhone }}</el-descriptions-item>
        </el-descriptions>
      </el-card>
      <el-card v-if="order" shadow="never" style="margin-top:16px">
        <template #header>收货信息</template>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="收货人">{{ order.receiverName }}</el-descriptions-item>
          <el-descriptions-item label="电话">{{ order.receiverPhone }}</el-descriptions-item>
          <el-descriptions-item label="地址">{{ order.receiverProvince }}{{ order.receiverCity }}{{ order.receiverDistrict }}{{ order.receiverAddress }}</el-descriptions-item>
        </el-descriptions>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrder } from '@/api/merchant/order'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const loading = ref(true)

function goBack() { router.back() }
function statusType(s) { return { '0': 'danger', '1': 'warning', '2': 'primary', '3': 'success', '4': 'info' }[s] || 'info' }
function statusLabel(s) { return { '0': '待付款', '1': '待发货', '2': '待收货', '3': '已完成', '4': '已取消' }[s] || s }

onMounted(() => {
  const id = route.params.id
  if (id) {
    getOrder(id).then(res => { order.value = res.data }).finally(() => { loading.value = false })
  } else {
    loading.value = false
  }
})
</script>
