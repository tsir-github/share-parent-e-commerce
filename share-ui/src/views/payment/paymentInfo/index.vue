<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="订单号" prop="orderNo">
        <el-input v-model="queryParams.orderNo" placeholder="请输入订单号" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="支付状态" prop="paymentStatus">
        <el-select v-model="queryParams.paymentStatus" placeholder="支付状态" clearable style="width: 150px">
          <el-option label="未支付" value="0" />
          <el-option label="已支付" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="paymentList" border stripe>
      <el-table-column label="订单号" align="center" prop="orderNo" width="220" />
      <el-table-column label="支付金额" align="center" prop="amount" width="100">
        <template #default="scope">¥{{ scope.row.amount }}</template>
      </el-table-column>
      <el-table-column label="已退款" align="center" prop="refundAmount" width="100">
        <template #default="scope">¥{{ scope.row.refundAmount || 0 }}</template>
      </el-table-column>
      <el-table-column label="支付方式" align="center" width="80">
        <template #default="scope">{{ scope.row.payWay === 1 ? '微信' : '-' }}</template>
      </el-table-column>
      <el-table-column label="支付状态" align="center" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.paymentStatus === 1 ? 'success' : 'danger'">{{ scope.row.paymentStatus === 1 ? '已支付' : '未支付' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="交易内容" align="center" prop="content" :show-overflow-tooltip="true" />
      <el-table-column label="微信交易号" align="center" prop="transactionId" width="200" :show-overflow-tooltip="true" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="170">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)">详情</el-button>
          <el-button link type="warning" icon="Money" @click="handleRefund(scope.row)" v-hasPermi="['payment:payment:refund']" :disabled="scope.row.paymentStatus !== 1 || (scope.row.refundAmount > 0)">退款</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="支付详情" v-model="detailOpen" width="600px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单号" :span="2">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ detail.userId }}</el-descriptions-item>
        <el-descriptions-item label="支付金额">¥{{ detail.amount }}</el-descriptions-item>
        <el-descriptions-item label="已退款金额">¥{{ detail.refundAmount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ detail.payWay === 1 ? '微信支付' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="支付状态">
          <el-tag :type="detail.paymentStatus === 1 ? 'success' : 'danger'">{{ detail.paymentStatus === 1 ? '已支付' : '未支付' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="微信交易号" :span="2">{{ detail.transactionId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="交易内容" :span="2">{{ detail.content || '-' }}</el-descriptions-item>
        <el-descriptions-item label="回调时间">{{ detail.callbackTime ? parseTime(detail.callbackTime) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近退款时间">{{ detail.refundTime ? parseTime(detail.refundTime) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ parseTime(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 退款弹窗 -->
    <el-dialog title="发起退款" v-model="refundOpen" width="500px" append-to-body>
      <el-form :model="refundForm" label-width="90px">
        <el-form-item label="订单号">
          <el-input :value="refundForm.orderNo" disabled />
        </el-form-item>
        <el-form-item label="退款金额">
          <el-input v-model="refundForm.amount" placeholder="请输入退款金额">
            <template #prefix>¥</template>
          </el-input>
        </el-form-item>
        <el-form-item label="退款原因">
          <el-input v-model="refundForm.reason" type="textarea" placeholder="请输入退款原因（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundOpen = false">取 消</el-button>
        <el-button type="primary" @click="submitRefund">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="PaymentInfo">
import { getCurrentInstance } from 'vue'
import { listPaymentInfo, refundPayment } from '@/api/payment/paymentInfo'

const { proxy } = getCurrentInstance()

const paymentList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const detailOpen = ref(false)
const detail = ref({})
const refundOpen = ref(false)

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    orderNo: undefined,
    paymentStatus: undefined
  },
  refundForm: {
    orderNo: '',
    amount: undefined,
    reason: ''
  }
})

const { queryParams, refundForm } = toRefs(data)

function getList() {
  loading.value = true
  listPaymentInfo(queryParams.value).then(res => {
    paymentList.value = res.rows || []
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
  detail.value = row
  detailOpen.value = true
}

function handleRefund(row) {
  refundForm.orderNo = row.orderNo
  refundForm.amount = row.amount - (row.refundAmount || 0)
  refundForm.reason = ''
  refundOpen.value = true
}

function submitRefund() {
  if (!refundForm.amount || refundForm.amount <= 0) {
    proxy.$modal.msgError('请输入有效的退款金额')
    return
  }
  refundPayment({ orderNo: refundForm.orderNo, amount: refundForm.amount, reason: refundForm.reason }).then(() => {
    proxy.$modal.msgSuccess('退款成功')
    refundOpen.value = false
    getList()
  })
}

getList()
</script>
