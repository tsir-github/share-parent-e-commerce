<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="订单号" prop="orderNo">
        <el-input v-model="queryParams.orderNo" placeholder="请输入订单号" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="订单状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="订单状态" clearable style="width: 200px">
          <el-option label="待支付" value="0" />
          <el-option label="待发货" value="1" />
          <el-option label="配送中" value="2" />
          <el-option label="已完成" value="3" />
          <el-option label="已取消" value="4" />
          <el-option label="售后中" value="5" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="orderList" border stripe>
      <el-table-column label="订单号" align="center" prop="orderNo" width="220" />
      <el-table-column label="用户ID" align="center" prop="userId" width="70" />
      <el-table-column label="实付金额" align="center" prop="payAmount" width="100">
        <template #default="scope">¥{{ scope.row.payAmount }}</template>
      </el-table-column>
      <el-table-column label="订单状态" align="center" width="100">
        <template #default="scope">
          <el-tag :type="statusType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="支付状态" align="center" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.payStatus === '1' ? 'success' : 'danger'">{{ scope.row.payStatus === '1' ? '已支付' : '未支付' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="收货人" align="center" prop="receiverName" width="100" />
      <el-table-column label="下单时间" align="center" prop="createTime" width="170">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="120" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="订单详情" v-model="detailOpen" width="700px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单号" :span="2">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ detail.userId }}</el-descriptions-item>
        <el-descriptions-item label="订单类型">{{ orderTypeLabel(detail.orderType) }}</el-descriptions-item>
        <el-descriptions-item label="商品总金额">¥{{ detail.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="优惠金额">¥{{ detail.discountAmount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="运费">¥{{ detail.freightAmount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="实付金额">¥{{ detail.payAmount }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <el-tag :type="statusType(detail.status)">{{ statusLabel(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="支付状态">
          <el-tag :type="detail.payStatus === '1' ? 'success' : 'danger'">{{ detail.payStatus === '1' ? '已支付' : '未支付' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ detail.payTime ? parseTime(detail.payTime) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="收货人">{{ detail.receiverName }}</el-descriptions-item>
        <el-descriptions-item label="收货电话">{{ detail.receiverPhone }}</el-descriptions-item>
        <el-descriptions-item label="收货地址" :span="2">{{ detail.receiverAddress }}</el-descriptions-item>
        <el-descriptions-item label="配送员">{{ detail.deliveryName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="配送电话">{{ detail.deliveryPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="配送时间">{{ detail.deliveryTime ? parseTime(detail.deliveryTime) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="签收时间">{{ detail.receiveTime ? parseTime(detail.receiveTime) : '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.closeReason" label="关闭原因" :span="2">{{ detail.closeReason }}</el-descriptions-item>
        <el-descriptions-item label="下单时间" :span="2">{{ parseTime(detail.createTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup name="OrderInfo">
import { getCurrentInstance } from 'vue'
import { listOrderInfo, getOrderInfo } from '@/api/order/orderInfo'

const { proxy } = getCurrentInstance()

const orderList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const detailOpen = ref(false)
const detail = ref({})

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    orderNo: undefined,
    status: undefined
  }
})

const { queryParams } = toRefs(data)

function statusType(s) {
  return { '0': 'danger', '1': 'warning', '2': 'primary', '3': 'success', '4': 'info', '5': 'warning' }[s] || 'info'
}

function statusLabel(s) {
  return { '0': '待支付', '1': '待发货', '2': '配送中', '3': '已完成', '4': '已取消', '5': '售后中' }[s] || s
}

function orderTypeLabel(t) {
  return { '0': '普通', '1': '秒杀', '2': '拼团' }[t] || t || '普通'
}

function getList() {
  loading.value = true
  listOrderInfo(queryParams.value).then(res => {
    orderList.value = res.rows || []
    total.value = res.total || 0
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleDetail(row) {
  getOrderInfo(row.id).then(res => {
    detail.value = res.data || res
    detailOpen.value = true
  })
}

getList()
</script>
