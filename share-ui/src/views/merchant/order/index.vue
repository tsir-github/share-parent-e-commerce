<template>
  <div class="merchant-orders">
    <div class="page-header"><h2>订单管理</h2></div>
    <div class="search-bar">
      <el-form :inline="true" size="small">
        <el-form-item label="状态">
          <el-select v-model="statusFilter" placeholder="全部" clearable style="width:130px" @change="fetchData">
            <el-option label="待付款" value="0" />
            <el-option label="待发货" value="1" />
            <el-option label="待收货" value="2" />
            <el-option label="已完成" value="3" />
            <el-option label="已取消" value="4" />
          </el-select>
        </el-form-item>
      </el-form>
    </div>
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="orderNo" label="订单号" width="220" />
      <el-table-column prop="totalAmount" label="金额" width="120" align="right">
        <template #default="{ row }">¥{{ row.totalAmount }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="下单时间" width="170" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="showDetail(row)">详情</el-button>
          <el-button v-if="row.status === '1'" link type="primary" size="small" @click="showDeliver(row)">发货</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="deliverVisible" title="发货" width="400px">
      <el-form :model="deliverForm" label-width="90px">
        <el-form-item label="配送员姓名">
          <el-input v-model="deliverForm.deliveryName" />
        </el-form-item>
        <el-form-item label="配送员电话">
          <el-input v-model="deliverForm.deliveryPhone" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deliverVisible = false">取消</el-button>
        <el-button type="primary" :loading="delivering" @click="handleDeliver">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listOrder, deliverOrder } from '@/api/merchant/order'

const router = useRouter()
const list = ref([])
const loading = ref(false)
const statusFilter = ref('')
const deliverVisible = ref(false)
const delivering = ref(false)
const deliverForm = ref({ orderNo: '', deliveryName: '', deliveryPhone: '' })

function statusType(s) {
  return { '0': 'danger', '1': 'warning', '2': 'primary', '3': 'success', '4': 'info' }[s] || 'info'
}
function statusLabel(s) {
  return { '0': '待付款', '1': '待发货', '2': '待收货', '3': '已完成', '4': '已取消' }[s] || s
}

function fetchData() {
  loading.value = true
  listOrder({ status: statusFilter.value || undefined })
    .then(res => { list.value = res.data || [] })
    .finally(() => { loading.value = false })
}

function showDetail(row) { router.push('/merchant/order/detail/' + row.id) }

function showDeliver(row) {
  deliverForm.value = { orderNo: row.orderNo, deliveryName: '', deliveryPhone: '' }
  deliverVisible.value = true
}

function handleDeliver() {
  if (!deliverForm.value.deliveryName || !deliverForm.value.deliveryPhone) {
    ElMessage.warning('请填写配送员信息')
    return
  }
  delivering.value = true
  deliverOrder(deliverForm.value).then(() => {
    ElMessage.success('发货成功')
    deliverVisible.value = false
    fetchData()
  }).finally(() => { delivering.value = false })
}

onMounted(fetchData)
</script>
