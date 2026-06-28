<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="店铺名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入店铺名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="商家状态" clearable style="width: 200px">
          <el-option label="待审核" value="0" />
          <el-option label="已启用" value="1" />
          <el-option label="已关闭" value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['merchant:merchant:add']">新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="merchantList" border stripe>
      <el-table-column label="ID" align="center" prop="id" width="60" />
      <el-table-column label="店铺名称" align="center" prop="name" width="160" :show-overflow-tooltip="true" />
      <el-table-column label="联系人" align="center" prop="contactName" width="100" />
      <el-table-column label="联系电话" align="center" prop="contactPhone" width="130" />
      <el-table-column label="店铺地址" align="center" prop="address" :show-overflow-tooltip="true" />
      <el-table-column label="状态" align="center" width="100">
        <template #default="scope">
          <el-tag :type="statusType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="审核备注" align="center" prop="auditRemark" width="140" :show-overflow-tooltip="true" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="170">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="260" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['merchant:merchant:edit']">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['merchant:merchant:remove']">删除</el-button>
          <el-button v-if="scope.row.status === '0'" link type="primary" icon="CircleCheck" @click="handleAudit(scope.row)" v-hasPermi="['merchant:merchant:audit']">审核</el-button>
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="title" v-model="open" width="550px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="店铺名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入店铺名称" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="form.contactName" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="店铺地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入店铺地址" />
        </el-form-item>
        <el-form-item label="店铺描述" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入店铺描述" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态">
            <el-option label="待审核" value="0" />
            <el-option label="已启用" value="1" />
            <el-option label="已关闭" value="2" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 审核弹窗 -->
    <el-dialog :title="auditTitle" v-model="auditOpen" width="450px" append-to-body>
      <el-form :model="auditForm" ref="auditRef" label-width="90px">
        <el-form-item label="店铺名称">
          <span>{{ auditForm.name }}</span>
        </el-form-item>
        <el-form-item label="当前状态">
          <el-tag :type="statusType(auditForm.status)">{{ statusLabel(auditForm.status) }}</el-tag>
        </el-form-item>
        <el-form-item label="审核备注" prop="auditRemark">
          <el-input v-model="auditForm.auditRemark" type="textarea" :rows="3" placeholder="请输入审核备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="success" :loading="auditing" @click="submitAudit('1')">审核通过</el-button>
          <el-button type="danger" :loading="auditing" @click="submitAudit('2')">审核拒绝</el-button>
          <el-button @click="auditOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="商家详情" v-model="detailOpen" width="600px" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="店铺名称">{{ detailForm.name }}</el-form-item>
        <el-form-item label="联系人">{{ detailForm.contactName }}</el-form-item>
        <el-form-item label="联系电话">{{ detailForm.contactPhone }}</el-form-item>
        <el-form-item label="店铺地址">{{ detailForm.address }}</el-form-item>
        <el-form-item label="店铺描述">{{ detailForm.description || '-' }}</el-form-item>
        <el-form-item label="状态">
          <el-tag :type="statusType(detailForm.status)">{{ statusLabel(detailForm.status) }}</el-tag>
        </el-form-item>
        <el-form-item label="审核备注">{{ detailForm.auditRemark || '-' }}</el-form-item>
        <el-form-item label="创建时间">{{ parseTime(detailForm.createTime) }}</el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup name="MerchantInfo">
import { getCurrentInstance } from 'vue'
import { listMerchantInfo, getMerchantInfo, auditMerchantInfo, addMerchantInfo, updateMerchantInfo, delMerchantInfo } from '@/api/merchant/info'

const { proxy } = getCurrentInstance()

const merchantList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const auditOpen = ref(false)
const auditTitle = ref('')
const auditing = ref(false)
const auditForm = ref({ id: undefined, name: '', status: '', auditRemark: '' })
const detailOpen = ref(false)
const detailForm = ref({})
const open = ref(false)
const title = ref('')

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    status: undefined
  },
  form: {}
})

const { queryParams, form } = toRefs(data)

const rules = {
  name: [{ required: true, message: '店铺名称不能为空', trigger: 'blur' }],
  contactName: [{ required: true, message: '联系人不能为空', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '联系电话不能为空', trigger: 'blur' }],
  address: [{ required: true, message: '店铺地址不能为空', trigger: 'blur' }]
}

function statusType(s) {
  return { '0': 'warning', '1': 'success', '2': 'danger' }[s] || 'info'
}

function statusLabel(s) {
  return { '0': '待审核', '1': '已启用', '2': '已关闭' }[s] || s
}

function getList() {
  loading.value = true
  listMerchantInfo(queryParams.value).then(res => {
    merchantList.value = res.rows
    total.value = res.total
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
  auditForm.value = { id: row.id, name: row.name, status: row.status, auditRemark: '' }
  auditOpen.value = true
  auditTitle.value = '审核商家 - ' + row.name
}

function submitAudit(status) {
  if (status === '2' && !auditForm.value.auditRemark) {
    proxy.$modal.msgWarning('审核拒绝请填写备注')
    return
  }
  auditing.value = true
  auditMerchantInfo({ id: auditForm.value.id, status, auditRemark: auditForm.value.auditRemark })
    .then(() => {
      proxy.$modal.msgSuccess('审核完成')
      auditOpen.value = false
      getList()
    })
    .finally(() => { auditing.value = false })
}

function handleDetail(row) {
  getMerchantInfo(row.id).then(res => {
    detailForm.value = res.data
    detailOpen.value = true
  })
}

// ========== 新增/修改 ==========

function resetForm() {
  form.value = { name: '', contactName: '', contactPhone: '', address: '', description: '', status: '0' }
}

function handleAdd() {
  resetForm()
  title.value = '新增商家'
  open.value = true
}

function handleUpdate(row) {
  form.value = { ...row }
  title.value = '修改商家'
  open.value = true
}

function submitForm() {
  proxy.$refs.formRef.validate(valid => {
    if (!valid) return
    if (form.value.id) {
      updateMerchantInfo(form.value).then(() => {
        proxy.$modal.msgSuccess('修改成功')
        open.value = false
        getList()
      })
    } else {
      addMerchantInfo(form.value).then(() => {
        proxy.$modal.msgSuccess('新增成功')
        open.value = false
        getList()
      })
    }
  })
}

function handleDelete(row) {
  proxy.$modal.confirm('确认删除商家"' + row.name + '"吗？').then(() => {
    return delMerchantInfo(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  })
}

getList()
</script>
