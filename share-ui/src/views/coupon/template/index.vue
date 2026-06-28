<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入优惠券名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="模板状态" clearable style="width: 150px">
          <el-option label="未启用" value="0" />
          <el-option label="已启用" value="1" />
          <el-option label="已过期" value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['coupon:template:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['coupon:template:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['coupon:template:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="templateList" @selection-change="handleSelectionChange" border stripe>
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="名称" align="center" prop="name" width="160" :show-overflow-tooltip="true" />
      <el-table-column label="类型" align="center" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.type === '0' ? 'primary' : 'success'">{{ scope.row.type === '0' ? '现金券' : '折扣券' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="满减条件" align="center" prop="conditionAmt" width="100">
        <template #default="scope">¥{{ scope.row.conditionAmt }}</template>
      </el-table-column>
      <el-table-column label="优惠" align="center" width="100">
        <template #default="scope">
          <span v-if="scope.row.type === '0'">¥{{ scope.row.discountAmt }}</span>
          <span v-else>{{ scope.row.discountRate }}折</span>
        </template>
      </el-table-column>
      <el-table-column label="发行总量" align="center" prop="totalCount" width="80" />
      <el-table-column label="剩余数量" align="center" prop="remainCount" width="80" />
      <el-table-column label="限领" align="center" prop="limitPerUser" width="60" />
      <el-table-column label="状态" align="center" width="80">
        <template #default="scope">
          <el-tag :type="statusType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="有效期" align="center" width="180">
        <template #default="scope">{{ parseTime(scope.row.startTime) }} ~ {{ parseTime(scope.row.endTime) }}</template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="170">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['coupon:template:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['coupon:template:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form :model="form" :rules="rules" ref="templateRef" label-width="110px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="优惠券名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入名称" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类型" prop="type">
              <el-select v-model="form.type" placeholder="请选择" style="width: 100%">
                <el-option label="现金券" value="0" />
                <el-option label="折扣券" value="1" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="满减条件" prop="conditionAmt">
              <el-input-number v-model="form.conditionAmt" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.type === '0'" label="优惠金额" prop="discountAmt">
              <el-input-number v-model="form.discountAmt" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
            <el-form-item v-else label="折扣率" prop="discountRate">
              <el-input-number v-model="form.discountRate" :min="0" :max="10" :precision="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="发行总量" prop="totalCount">
              <el-input-number v-model="form.totalCount" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="每人限领" prop="limitPerUser">
              <el-input-number v-model="form.limitPerUser" :min="1" :max="99" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="有效期开始" prop="startTime">
              <el-date-picker v-model="form.startTime" type="datetime" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD HH:mm:ss" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="有效期结束" prop="endTime">
              <el-date-picker v-model="form.endTime" type="datetime" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD HH:mm:ss" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="0">未启用</el-radio>
            <el-radio value="1">已启用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="CouponTemplate">
import { getCurrentInstance } from 'vue'
import { listTemplate, getTemplate, addTemplate, updateTemplate, delTemplate } from '@/api/coupon/template'

const { proxy } = getCurrentInstance()

const templateList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    status: undefined
  },
  rules: {
    name: [{ required: true, message: '名称不能为空', trigger: 'blur' }],
    type: [{ required: true, message: '请选择类型', trigger: 'change' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

function statusType(s) {
  return { '0': 'info', '1': 'success', '2': 'danger' }[s] || 'info'
}

function statusLabel(s) {
  return { '0': '未启用', '1': '已启用', '2': '已过期' }[s] || s
}

function getList() {
  loading.value = true
  listTemplate(queryParams.value).then(res => {
    templateList.value = res.rows || []
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

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function reset() {
  form.value = {
    id: undefined,
    name: undefined,
    type: '0',
    conditionAmt: 0,
    discountAmt: 0,
    discountRate: 0,
    totalCount: 100,
    limitPerUser: 1,
    startTime: undefined,
    endTime: undefined,
    status: '0'
  }
  proxy.resetForm('templateRef')
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '添加优惠券模板'
}

function handleUpdate(row) {
  reset()
  const id = row.id || ids.value
  getTemplate(id).then(res => {
    form.value = res.data
    open.value = true
    title.value = '修改优惠券模板'
  })
}

function handleDelete(row) {
  const idsToDel = row.id || ids.value
  proxy.$modal.confirm('是否确认删除该模板？').then(() => {
    return delTemplate(idsToDel)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function submitForm() {
  proxy.$refs['templateRef'].validate(valid => {
    if (valid) {
      const submit = form.value.id !== undefined ? updateTemplate(form.value) : addTemplate(form.value)
      submit.then(() => {
        proxy.$modal.msgSuccess(form.value.id !== undefined ? '修改成功' : '新增成功')
        open.value = false
        getList()
      })
    }
  })
}

function cancel() {
  open.value = false
  reset()
}

getList()
</script>
