<template>
  <div class="app-container">
    <el-row :gutter="12" class="mb8">
      <el-col :span="24"><el-page-header @back="goBack" title="返回"><template #content><span style="font-size:18px;font-weight:600">订单详情</span></template></el-page-header></el-col>
    </el-row>

    <div v-if="order" v-loading="loading">
      <!-- 状态横幅 -->
      <el-card shadow="hover" class="mb8" :style="{borderLeft:'4px solid '+statusColor(order.status)}">
        <div style="display:flex;align-items:center;justify-content:space-between">
          <div>
            <span style="font-size:13px;color:#909399">{{ order.createTime?.substring(0,16) }}</span>
            <span style="margin:0 12px;color:#dcdfe6">|</span>
            <span style="font-size:13px;color:#909399">{{ order.orderType==='0'?'普通':order.orderType==='1'?'秒杀':'拼团' }}订单</span>
          </div>
          <el-tag :type="statusType(order.status)" size="large" effect="dark" round>{{ statusLabel(order.status) }}</el-tag>
        </div>
      </el-card>

      <el-row :gutter="12">
        <!-- 左列 -->
        <el-col :span="14">
          <!-- 收货信息 -->
          <el-card shadow="hover" class="mb8">
            <template #header><div style="display:flex;align-items:center"><el-icon style="margin-right:6px;color:#409eff"><LocationFilled /></el-icon><span style="font-weight:600">收货信息</span></div></template>
            <div style="padding-left:24px">
              <div style="font-size:15px;font-weight:600;margin-bottom:4px">{{ order.receiverName }} <span style="font-size:13px;color:#606266;font-weight:400">{{ order.receiverPhone }}</span></div>
              <div style="color:#909399;font-size:13px">{{ (order.receiverProvince||'') }}{{ (order.receiverCity||'') }}{{ (order.receiverDistrict||'') }}{{ order.receiverAddress }}</div>
            </div>
          </el-card>
          <!-- 配送信息 -->
          <el-card shadow="hover" class="mb8">
            <template #header><div style="display:flex;align-items:center"><el-icon style="margin-right:6px;color:#67c23a"><Van /></el-icon><span style="font-weight:600">配送信息</span></div></template>
            <el-descriptions :column="2" size="small">
              <el-descriptions-item label="状态"><el-tag :type="deliveryType(order.deliveryStatus)" size="small" effect="plain">{{ deliveryLabel(order.deliveryStatus) }}</el-tag></el-descriptions-item>
              <el-descriptions-item label="配送员">{{ order.deliveryName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="电话">{{ order.deliveryPhone || '-' }}</el-descriptions-item>
              <el-descriptions-item label="发货时间">{{ order.deliveryTime || '-' }}</el-descriptions-item>
              <el-descriptions-item label="签收时间" :span="2">{{ order.receiveTime || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
        <!-- 右列 -->
        <el-col :span="10">
          <!-- 金额明细 -->
          <el-card shadow="hover" class="mb8">
            <template #header><div style="display:flex;align-items:center"><el-icon style="margin-right:6px;color:#e6a23c"><Money /></el-icon><span style="font-weight:600">金额明细</span></div></template>
            <div style="padding:0 8px">
              <div class="amount-row"><span>商品总价</span><span>¥{{ order.totalAmount }}</span></div>
              <div class="amount-row" v-if="order.discountAmount"><span>优惠</span><span style="color:#f56c6c">-¥{{ order.discountAmount }}</span></div>
              <div class="amount-row"><span>运费</span><span>¥{{ order.freightAmount || 0 }}</span></div>
              <el-divider style="margin:8px 0" />
              <div class="amount-row" style="font-size:18px;font-weight:700"><span>实付</span><span style="color:#f56c6c">¥{{ order.payAmount }}</span></div>
              <div class="amount-row" v-if="order.refundAmount"><span>已退款</span><span style="color:#909399">¥{{ order.refundAmount }}</span></div>
            </div>
          </el-card>
          <!-- 支付信息 -->
          <el-card shadow="hover" class="mb8">
            <template #header><div style="display:flex;align-items:center"><el-icon style="margin-right:6px;color:#909399"><CreditCard /></el-icon><span style="font-weight:600">支付信息</span></div></template>
            <el-descriptions :column="2" size="small">
              <el-descriptions-item label="方式">微信支付</el-descriptions-item>
              <el-descriptions-item label="状态"><el-tag :type="order.payStatus==='1'?'success':'info'" size="small" effect="plain">{{ order.payStatus==='1'?'已支付':'未支付' }}</el-tag></el-descriptions-item>
              <el-descriptions-item label="流水号" :span="2">{{ order.transactionId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="支付时间" :span="2">{{ order.payTime || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
      </el-row>

      <!-- 订单信息 -->
      <el-card shadow="hover" class="mb8">
        <template #header><span style="font-weight:600">订单号 #{{ order.orderNo }}</span></template>
        <el-descriptions :column="3" size="small">
          <el-descriptions-item label="用户ID">{{ order.userId }}</el-descriptions-item>
          <el-descriptions-item label="订单类型">{{ order.orderType==='0'?'普通':order.orderType==='1'?'秒杀':'拼团' }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ order.createTime }}</el-descriptions-item>
          <template v-if="order.status==='4'">
            <el-descriptions-item label="关闭原因">{{ order.closeReason || '-' }}</el-descriptions-item>
            <el-descriptions-item label="关闭类型">{{ order.closeType==='1'?'用户取消':order.closeType==='2'?'超时取消':order.closeType==='3'?'商家取消':order.closeType==='4'?'客服取消':'-' }}</el-descriptions-item>
            <el-descriptions-item label="关闭时间">{{ order.closeTime || '-' }}</el-descriptions-item>
          </template>
          <el-descriptions-item label="使用优惠券" :span="3">{{ order.couponIds || '无' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 操作按钮 -->
      <el-row v-if="order.status==='1'" style="text-align:center;margin-top:16px">
        <el-button type="primary" size="large" @click="showDeliver"><el-icon style="margin-right:6px"><Van /></el-icon>确认发货</el-button>
      </el-row>
    </div>

    <el-dialog v-model="deliverVisible" title="发货" width="400px" append-to-body>
      <el-form :model="deliverForm" label-width="90px">
        <el-form-item label="配送员姓名"><el-input v-model="deliverForm.deliveryName" /></el-form-item>
        <el-form-item label="配送员电话"><el-input v-model="deliverForm.deliveryPhone" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deliverVisible=false">取消</el-button>
        <el-button type="primary" :loading="delivering" @click="handleDeliver">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="MerchantOrderDetail">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getOrder, deliverOrder } from '@/api/merchant/order'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const loading = ref(true)
const deliverVisible = ref(false)
const delivering = ref(false)
const deliverForm = ref({ orderNo: '', deliveryName: '', deliveryPhone: '' })

function goBack() { router.back() }
function statusType(s) { return { '0':'danger','1':'warning','2':'primary','3':'success','4':'info','5':'danger' }[s]||'info' }
function statusLabel(s) { return { '0':'待支付','1':'待发货','2':'配送中','3':'已完成','4':'已取消','5':'售后中' }[s]||s }
function statusColor(s) { return { '0':'#f56c6c','1':'#e6a23c','2':'#409eff','3':'#67c23a','4':'#909399','5':'#f56c6c' }[s]||'#909399' }
function deliveryType(s) { return { '0':'info','1':'warning','2':'success','3':'success' }[s]||'info' }
function deliveryLabel(s) { return { '0':'未配送','1':'配送中','2':'已送达','3':'已确认' }[s]||'未配送' }

function showDeliver() { deliverForm.value = { orderNo: order.value.orderNo, deliveryName: '', deliveryPhone: '' }; deliverVisible.value = true }
function handleDeliver() {
  if (!deliverForm.value.deliveryName||!deliverForm.value.deliveryPhone) { ElMessage.warning('请填写配送员信息'); return }
  delivering.value = true
  deliverOrder(deliverForm.value).then(() => { ElMessage.success('发货成功'); deliverVisible.value = false; getOrder(order.value.id).then(res => { order.value = res.data }) }).finally(() => { delivering.value = false })
}

onMounted(() => {
  const id = route.params.id; if (id) { getOrder(id).then(res => { order.value = res.data }).finally(() => { loading.value = false }) } else loading.value = false
})
</script>

<style scoped>
.amount-row { display:flex; justify-content:space-between; padding:6px 0; font-size:14px; color:#606266; }
:deep(.el-page-header__left) { cursor: pointer; }
:deep(.el-descriptions__label) { width: 80px; }
</style>
