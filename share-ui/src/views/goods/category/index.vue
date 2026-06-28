<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['goods:category:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['goods:category:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['goods:category:remove']">删除</el-button>
      </el-col>
      <right-toolbar @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="categoryList" row-key="id" default-expand-all :tree-props="{ children: 'children', hasChildren: 'hasChildren' }" border stripe>
      <el-table-column label="分类名称" align="center" prop="name" width="200" />
      <el-table-column label="排序" align="center" prop="sort" width="70" />
      <el-table-column label="状态" align="center" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'">{{ scope.row.status === '0' ? '正常' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="170">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Plus" @click="handleAdd(scope.row)" v-hasPermi="['goods:category:add']">添加子分类</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['goods:category:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['goods:category:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form :model="form" :rules="rules" ref="categoryRef" label-width="90px">
        <el-form-item label="上级分类" prop="parentId">
          <el-tree-select v-model="form.parentId" :data="treeOptions" :props="{ value: 'id', label: 'name', children: 'children' }" placeholder="请选择上级分类" clearable check-strictly />
        </el-form-item>
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="0">正常</el-radio>
            <el-radio value="1">禁用</el-radio>
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

<script setup name="Category">
import { getCurrentInstance } from 'vue'
import { listCategory, getCategory, addCategory, updateCategory, delCategory, treeselect } from '@/api/goods/category'

const { proxy } = getCurrentInstance()

const categoryList = ref([])
const open = ref(false)
const loading = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const title = ref('')
const treeOptions = ref([])

const data = reactive({
  form: {},
  rules: {
    name: [{ required: true, message: '分类名称不能为空', trigger: 'blur' }]
  }
})

const { form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listCategory().then(res => {
    categoryList.value = res.data || res.rows || []
    loading.value = false
  })
}

function getTree() {
  treeselect().then(res => {
    treeOptions.value = res.data || []
  })
}

function handleAdd(row) {
  reset()
  if (row && row.id) {
    form.value.parentId = row.id
  }
  open.value = true
  title.value = '添加分类'
}

function handleUpdate(row) {
  reset()
  getCategory(row.id).then(res => {
    form.value = res.data
    open.value = true
    title.value = '修改分类'
  })
}

function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除该分类？').then(() => {
    return delCategory(row.id || ids.value)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function reset() {
  form.value = {
    id: undefined,
    parentId: undefined,
    name: undefined,
    sort: 0,
    status: '0'
  }
  proxy.resetForm('categoryRef')
}

function submitForm() {
  proxy.$refs['categoryRef'].validate(valid => {
    if (valid) {
      const submit = form.value.id !== undefined ? updateCategory(form.value) : addCategory(form.value)
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

getTree()
getList()
</script>
