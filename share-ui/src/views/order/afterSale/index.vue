<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="审核状态" prop="auditStatus">
        <el-select v-model="queryParams.auditStatus" placeholder="审核状态" clearable style="width: 200px">
          <el-option label="待审核" value="0" />
          <el-option label="商家同意" value="1" />
          <el-option label="商家拒绝" value="2" />
          <el-option label="客服介入" value="3" />
          <el-option label="客服同意退款" value="4" />
          <el-option label="客服拒绝" value="5" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column label="ID" align="center" prop="id" width="60" />
      <el-table-column label="订单号" align="center" prop="orderNo" width="220" />
      <el-table-column label="用户ID" align="center" prop="userId" width="70" />
      <el-table-column label="商家ID" align="center" prop="merchantId" width="70" />
      <el-table-column label="退款金额" align="center" prop="refundAmount" width="100">
        <template #default="scope">¥{{ scope.row.refundAmount }}</template>
      </el-table-column>
      <el-table-column label="退款原因" align="center" prop="refundReason" :show-overflow-tooltip="true" min-width="120" />
      <el-table-column label="审核状态" align="center" width="110">
        <template #default="scope">
          <el-tag :type="statusType(scope.row.auditStatus)">{{ statusLabel(scope.row.auditStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="审核备注" align="center" prop="auditRemark" :show-overflow-tooltip="true" min-width="120" />
      <el-table-column label="审核人" align="center" prop="auditBy" width="100" />
      <el-table-column label="审核时间" align="center" prop="auditTime" width="170">
        <template #default="scope">{{ parseTime(scope.row.auditTime) }}</template>
      </el-table-column>
      <el-table-column label="申请时间" align="center" prop="createTime" width="170">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Check" @click="handleAudit(scope.row)" v-hasPermi="['order:after-sale:audit']">审核</el-button>
          <el-button link type="warning" icon="Service" @click="handleAdminAudit(scope.row)" v-hasPermi="['order:after-sale:admin-audit']">客服处理</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 商家审核弹窗 -->
    <el-dialog title="商家审核" v-model="auditOpen" width="500px" append-to-body>
      <el-form :model="auditForm" label-width="80px">
        <el-form-item label="订单号">
          <el-input :value="auditForm.orderNo" disabled />
        </el-form-item>
        <el-form-item label="审核结果">
          <el-radio-group v-model="auditForm.auditStatus">
            <el-radio value="1">同意退款</el-radio>
            <el-radio value="2">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input v-model="auditForm.auditRemark" type="textarea" placeholder="请输入审核备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditOpen = false">取 消</el-button>
        <el-button type="primary" @click="submitAudit">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 客服处理弹窗 -->
    <el-dialog title="客服处理" v-model="adminAuditOpen" width="500px" append-to-body>
      <el-form :model="adminAuditForm" label-width="80px">
        <el-form-item label="订单号">
          <el-input :value="adminAuditForm.orderNo" disabled />
        </el-form-item>
        <el-form-item label="当前状态">
          <el-tag :type="statusType(adminAuditForm.currentStatus)">{{ statusLabel(adminAuditForm.currentStatus) }}</el-tag>
        </el-form-item>
        <el-form-item label="处理结果">
          <el-radio-group v-model="adminAuditForm.auditStatus">
            <el-radio value="4">同意退款</el-radio>
            <el-radio value="5">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处理备注">
          <el-input v-model="adminAuditForm.auditRemark" type="textarea" placeholder="请输入处理备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adminAuditOpen = false">取 消</el-button>
        <el-button type="primary" @click="submitAdminAudit">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="AfterSale">
import { getCurrentInstance } from 'vue'
import { listAfterSale, auditAfterSale, adminAuditAfterSale } from '@/api/order/afterSale'

const { proxy } = getCurrentInstance()

const list = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const auditOpen = ref(false)
const adminAuditOpen = ref(false)

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    auditStatus: undefined
  },
  auditForm: {
    id: undefined,
    orderNo: '',
    auditStatus: '1',
    auditRemark: ''
  },
  adminAuditForm: {
    id: undefined,
    orderNo: '',
    currentStatus: '',
    auditStatus: '4',
    auditRemark: ''
  }
})

const { queryParams, auditForm, adminAuditForm } = toRefs(data)

function statusType(s) {
  return { '0': 'warning', '1': 'success', '2': 'danger', '3': 'info', '4': 'success', '5': 'danger' }[s] || 'info'
}

function statusLabel(s) {
  return { '0': '待审核', '1': '商家同意', '2': '商家拒绝', '3': '客服介入', '4': '客服同意退款', '5': '客服拒绝' }[s] || s
}

function getList() {
  loading.value = true
  listAfterSale(queryParams.value).then(res => {
    list.value = res.rows || []
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

function handleAudit(row) {
  auditForm.value.id = row.id
  auditForm.value.orderNo = row.orderNo
  auditForm.value.auditStatus = '1'
  auditForm.value.auditRemark = ''
  auditOpen.value = true
}

function submitAudit() {
  auditAfterSale({ id: auditForm.value.id, auditStatus: auditForm.value.auditStatus, auditRemark: auditForm.value.auditRemark }).then(() => {
    proxy.$modal.msgSuccess('审核成功')
    auditOpen.value = false
    getList()
  })
}

function handleAdminAudit(row) {
  adminAuditForm.value.id = row.id
  adminAuditForm.value.orderNo = row.orderNo
  adminAuditForm.value.currentStatus = row.auditStatus
  adminAuditForm.value.auditStatus = '4'
  adminAuditForm.value.auditRemark = ''
  adminAuditOpen.value = true
}

function submitAdminAudit() {
  adminAuditAfterSale({ id: adminAuditForm.value.id, auditStatus: adminAuditForm.value.auditStatus, auditRemark: adminAuditForm.value.auditRemark }).then(() => {
    proxy.$modal.msgSuccess('处理成功')
    adminAuditOpen.value = false
    getList()
  })
}

getList()
</script>
