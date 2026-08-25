<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['goods:banner:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['goods:banner:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange" border stripe>
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="标题" align="center" prop="title" width="160" :show-overflow-tooltip="true" />
      <el-table-column label="图片" align="center" width="120">
        <template #default="scope">
          <el-image :src="scope.row.imageUrl" style="width:80px;height:40px" fit="cover" />
        </template>
      </el-table-column>
      <el-table-column label="跳转链接" align="center" prop="linkUrl" width="200" :show-overflow-tooltip="true" />
      <el-table-column label="排序" align="center" prop="sort" width="60" />
      <el-table-column label="状态" align="center" width="80">
        <template #default="scope">
          <el-switch :model-value="scope.row.status === '1'" @change="val => handleStatusChange(scope.row, val)" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['goods:banner:edit']">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['goods:banner:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="550px" append-to-body>
      <el-form :model="form" :rules="rules" ref="bannerRef" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入" maxlength="50" />
        </el-form-item>
        <el-form-item label="图片URL" prop="imageUrl">
          <el-input v-model="form.imageUrl" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="跳转链接" prop="linkUrl">
          <el-input v-model="form.linkUrl" placeholder="可选" />
        </el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="排序" prop="sort">
              <el-input-number v-model="form.sort" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio value="0">禁用</el-radio>
                <el-radio value="1">启用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
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

<script setup name="Banner">
import { getCurrentInstance } from 'vue'
import { listBanner, getBanner, addBanner, updateBanner, updateBannerStatus, delBanner } from '@/api/goods/banner'

const { proxy } = getCurrentInstance()

const list = ref([])
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
  queryParams: { pageNum: 1, pageSize: 10, title: undefined },
  rules: {
    title: [{ required: true, message: '标题不能为空', trigger: 'blur' }],
    imageUrl: [{ required: true, message: '图片URL不能为空', trigger: 'blur' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listBanner(queryParams.value).then(res => {
    list.value = res.rows || []
    total.value = res.total || 0
    loading.value = false
  })
}

function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function reset() {
  form.value = { id: undefined, title: undefined, imageUrl: undefined, linkUrl: undefined, sort: 0, status: '1' }
  proxy.resetForm('bannerRef')
}

function handleAdd() { reset(); open.value = true; title.value = '新增Banner' }

function handleUpdate(row) {
  reset()
  getBanner(row.id || ids.value).then(res => {
    form.value = res.data
    open.value = true
    title.value = '修改Banner'
  })
}

function handleStatusChange(row, val) {
  updateBannerStatus({ id: row.id, status: val ? '1' : '0' }).then(() => {
    row.status = val ? '1' : '0'
    proxy.$modal.msgSuccess(val ? '已启用' : '已禁用')
  })
}

function handleDelete(row) {
  const idsToDel = row.id || ids.value
  proxy.$modal.confirm('确认删除？').then(() => delBanner(idsToDel)).then(() => {
    getList(); proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function submitForm() {
  proxy.$refs['bannerRef'].validate(valid => {
    if (valid) {
      const submit = form.value.id ? updateBanner(form.value) : addBanner(form.value)
      submit.then(() => {
        proxy.$modal.msgSuccess(form.value.id ? '修改成功' : '新增成功')
        open.value = false; getList()
      })
    }
  })
}

function cancel() { open.value = false; reset() }

getList()
</script>
