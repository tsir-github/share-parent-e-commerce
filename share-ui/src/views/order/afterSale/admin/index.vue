<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="审核状态" prop="auditStatus">
        <el-select v-model="queryParams.auditStatus" placeholder="全部" clearable style="width:140px">
          <el-option label="待审核" value="0" />
          <el-option label="商家通过" value="1" />
          <el-option label="商家拒绝" value="2" />
          <el-option label="客服通过" value="3" />
          <el-option label="客服拒绝" value="4" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list" border stripe max-height="600">
      <el-table-column label="订单号" prop="orderNo" width="170" />
      <el-table-column label="退款金额" prop="refundAmount" width="100" align="center">
        <template #default="s">¥{{ s.row.refundAmount }}</template>
      </el-table-column>
      <el-table-column label="退款原因" prop="refundReason" width="200" :show-overflow-tooltip="true" />
      <el-table-column label="审核状态" align="center" width="100">
        <template #default="s">
          <el-tag :type="statusType(s.row.auditStatus)">{{ statusLabel(s.row.auditStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="审核备注" prop="auditRemark" width="160" :show-overflow-tooltip="true" />
      <el-table-column label="申请时间" prop="createTime" width="160" />
      <el-table-column label="操作" align="center" width="200" fixed="right">
        <template #default="s">
          <el-button link type="success" icon="Check" @click="handleAudit(s.row, '1')" v-if="s.row.auditStatus === '0'" v-hasPermi="['order:after-sale:audit']">通过</el-button>
          <el-button link type="danger" icon="Close" @click="handleAudit(s.row, '2')" v-if="s.row.auditStatus === '0'" v-hasPermi="['order:after-sale:audit']">拒绝</el-button>
          <el-button link type="success" icon="Check" @click="handleAdminAudit(s.row, '3')" v-hasPermi="['order:after-sale:admin-audit']">客服通过</el-button>
          <el-button link type="danger" icon="Close" @click="handleAdminAudit(s.row, '4')" v-hasPermi="['order:after-sale:admin-audit']">客服拒绝</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="auditTitle" v-model="auditOpen" width="450px">
      <el-form ref="auditRef" :model="auditForm" :rules="auditRules" label-width="100px">
        <el-form-item label="操作">
          <el-tag :type="auditForm.status === '1' || auditForm.status === '3' ? 'success' : 'danger'">
            {{ auditForm.status === '1' || auditForm.status === '3' ? '通过' : '拒绝' }}
          </el-tag>
        </el-form-item>
        <el-form-item label="审核备注" prop="auditRemark">
          <el-input v-model="auditForm.auditRemark" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitAudit">确 定</el-button>
        <el-button @click="auditOpen = false">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="AfterSaleAdmin">
import { getCurrentInstance } from 'vue'
import { listAfterSale, auditAfterSale, adminAuditAfterSale } from '@/api/order/adminAfterSale'

const { proxy } = getCurrentInstance()

const list = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const auditOpen = ref(false)
const auditTitle = ref('')
const auditForm = ref({ id: undefined, status: '', auditRemark: undefined })
const auditRules = {}

const queryParams = ref({ pageNum: 1, pageSize: 10, auditStatus: undefined })

function statusType(s) {
  return { '0': 'warning', '1': 'success', '2': 'danger', '3': 'success', '4': 'danger' }[s] || 'info'
}
function statusLabel(s) {
  return { '0': '待审核', '1': '商家通过', '2': '商家拒绝', '3': '客服通过', '4': '客服拒绝' }[s] || s
}

function getList() {
  loading.value = true
  listAfterSale(queryParams.value).then(res => {
    list.value = res.rows || []
    total.value = res.total || 0
    loading.value = false
  })
}

function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }

function handleAudit(row, status) {
  auditForm.value = { id: row.id, status, auditRemark: undefined }
  auditTitle.value = status === '1' ? '商家审核通过' : '商家审核拒绝'
  auditOpen.value = true
}

function handleAdminAudit(row, status) {
  auditForm.value = { id: row.id, status, auditRemark: undefined }
  auditTitle.value = status === '3' ? '客服审核通过' : '客服审核拒绝'
  auditOpen.value = true
}

function submitAudit() {
  const f = auditForm.value
  const isAdmin = f.status === '3' || f.status === '4'
  const api = isAdmin ? adminAuditAfterSale : auditAfterSale
  api({ id: f.id, auditStatus: f.status, auditRemark: f.auditRemark || '' }).then(() => {
    proxy.$modal.msgSuccess('审核完成')
    auditOpen.value = false
    getList()
  })
}

getList()
</script>
