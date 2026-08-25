<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="优惠券名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="类型" prop="type">
        <el-select v-model="queryParams.type" placeholder="全部" clearable style="width: 120px">
          <el-option label="满减券" value="0" />
          <el-option label="折扣券" value="1" />
          <el-option label="无门槛" value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
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
      <el-table-column label="名称" align="center" prop="name" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="类型" align="center" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.type === '0' ? 'primary' : scope.row.type === '2' ? 'success' : 'warning'">
            {{ scope.row.type === '0' ? '满减' : scope.row.type === '2' ? '无门槛' : '折扣' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="门槛" align="center" width="90">
        <template #default="scope">¥{{ scope.row.conditionAmt || 0 }}</template>
      </el-table-column>
      <el-table-column label="优惠" align="center" width="90">
        <template #default="scope">
          <span v-if="scope.row.type === '0' || scope.row.type === '2'">¥{{ scope.row.discountAmt }}</span>
          <span v-else>{{ scope.row.discountRate }}折</span>
        </template>
      </el-table-column>
      <el-table-column label="库存" align="center" width="70">
        <template #default="scope">{{ scope.row.remainCount }}/{{ scope.row.totalCount }}</template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="90">
        <template #default="scope">
          <el-switch
            v-model="scope.row.status"
            active-value="1"
            inactive-value="0"
            @change="handleStatusChange(scope.row)"
            v-hasPermi="['coupon:template:edit']"
          />
        </template>
      </el-table-column>
      <el-table-column label="有效期" align="center" width="170">
        <template #default="scope">{{ parseTime(scope.row.startTime, '{y}-{m}-{d}') }} ~ {{ parseTime(scope.row.endTime, '{y}-{m}-{d}') }}</template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" width="160">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="120" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['coupon:template:edit']">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['coupon:template:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="650px" append-to-body @close="cancel">
      <el-form :model="form" :rules="rules" ref="templateRef" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="优惠券名称" prop="name">
              <el-input v-model="form.name" placeholder="如：新人专享券" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="优惠类型" prop="type">
              <el-select v-model="form.type" @change="onTypeChange" style="width: 100%">
                <el-option label="💵 满减券" value="0" />
                <el-option label="🏷 折扣券" value="1" />
                <el-option label="🎁 无门槛券" value="2" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="使用门槛" prop="conditionAmt">
              <el-input v-model="form.conditionAmt" placeholder="满多少可用，0=无门槛">
                <template #append>元</template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.type === '0'" label="优惠金额" prop="discountAmt">
              <el-input v-model="form.discountAmt" placeholder="满减金额">
                <template #append>元</template>
              </el-input>
            </el-form-item>
            <el-form-item v-else-if="form.type === '1'" label="折扣力度" prop="discountRate">
              <el-select v-model="form.discountRate" style="width: 100%">
                <el-option v-for="i in 9" :key="i" :label="(10-i) + '折'" :value="(10-i)" />
              </el-select>
            </el-form-item>
            <el-form-item v-else label="减免金额" prop="discountAmt">
              <el-input v-model="form.discountAmt" placeholder="无门槛直接减">
                <template #append>元</template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="发放总量" prop="totalCount">
              <el-input v-model="form.totalCount" placeholder="计划发行数量" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="每人限领" prop="limitPerUser">
              <el-select v-model="form.limitPerUser" style="width: 100%">
                <el-option v-for="n in [1,2,3,5,10]" :key="n" :label="'限领 ' + n + ' 张'" :value="n" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker v-model="form.startTime" type="datetime" placeholder="选择开始时间" style="width: 100%" value-format="YYYY-MM-DD HH:mm:ss" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker v-model="form.endTime" type="datetime" placeholder="选择结束时间" style="width: 100%" value-format="YYYY-MM-DD HH:mm:ss" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="启用状态">
          <el-radio-group v-model="form.status">
            <el-radio label="1">✅ 创建后立即启用</el-radio>
            <el-radio label="0">⏸ 暂不启用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancel">取 消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">确 定</el-button>
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
const submitting = ref(false)

const data = reactive({
  form: {},
  queryParams: { pageNum: 1, pageSize: 10, name: undefined, type: undefined, status: undefined },
  rules: {
    name: [{ required: true, message: '名称不能为空', trigger: 'blur' }],
    type: [{ required: true, message: '请选择类型', trigger: 'change' }],
    startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
    endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
    totalCount: [{ required: true, message: '请输入发行总量', trigger: 'blur' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listTemplate(queryParams.value).then(res => {
    templateList.value = res.rows || []
    total.value = res.total || 0
    loading.value = false
  })
}

function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }

function handleSelectionChange(selection) {
  ids.value = selection.map(i => i.id)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function onTypeChange() {
  if (form.value.type === '2') { form.value.conditionAmt = 0; form.value.discountRate = undefined }
  else if (form.value.type === '1') { form.value.discountAmt = undefined }
  else { form.value.discountRate = undefined }
}

function reset() {
  form.value = { id: undefined, name: undefined, type: '0', conditionAmt: 0, discountAmt: 0, discountRate: undefined, totalCount: 100, limitPerUser: 1, startTime: undefined, endTime: undefined, status: '1' }
}

function handleAdd() { reset(); open.value = true; title.value = '新增优惠券' }
function handleUpdate(row) {
  reset()
  const id = row.id || ids.value
  getTemplate(id).then(res => {
    form.value = res.data
    open.value = true
    title.value = '编辑优惠券'
  })
}

function handleStatusChange(row) {
  const text = row.status === '1' ? '启用' : '禁用'
  proxy.$modal.confirm('确认' + text + '该优惠券？').then(() => {
    return updateTemplate({ id: row.id, status: row.status })
  }).then(() => {
    proxy.$modal.msgSuccess(text + '成功')
    getList()
  }).catch(() => { getList() })
}

function handleDelete(row) {
  const delIds = row.id || ids.value
  proxy.$modal.confirm('确认删除？已领取的用户不受影响。').then(() => delTemplate(delIds)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {})
}

function submitForm() {
  proxy.$refs['templateRef'].validate(valid => {
    if (!valid) return
    submitting.value = true
    const d = form.value
    const payload = {
      id: d.id,
      name: d.name,
      type: d.type,
      conditionAmt: d.type !== '2' ? (parseInt(d.conditionAmt) || 0) : 0,
      discountAmt: d.type !== '1' ? (parseFloat(d.discountAmt) || 0) : undefined,
      discountRate: d.type === '1' ? (parseFloat(d.discountRate) || 9) : undefined,
      totalCount: parseInt(d.totalCount) || 0,
      limitPerUser: parseInt(d.limitPerUser) || 1,
      startTime: d.startTime,
      endTime: d.endTime,
      status: d.status
    }
    const api = d.id ? updateTemplate(payload) : addTemplate(payload)
    api.then(() => {
      proxy.$modal.msgSuccess(d.id ? '修改成功' : '新增成功')
      open.value = false
      getList()
    }).catch(() => {}).finally(() => submitting.value = false)
  })
}

function cancel() { open.value = false; reset() }

getList()
</script>
