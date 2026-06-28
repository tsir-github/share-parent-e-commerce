<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="商品名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入商品名称" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="商品状态" clearable style="width: 200px">
          <el-option label="上架" value="0" />
          <el-option label="下架" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['goods:product:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['goods:product:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['goods:product:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="productList" @selection-change="handleSelectionChange" border stripe>
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="商品名称" align="center" prop="name" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="分类ID" align="center" prop="categoryId" width="70" />
      <el-table-column label="最低售价" align="center" prop="minPrice" width="100">
        <template #default="scope">¥{{ scope.row.minPrice }}</template>
      </el-table-column>
      <el-table-column label="最高售价" align="center" prop="maxPrice" width="100">
        <template #default="scope">¥{{ scope.row.maxPrice }}</template>
      </el-table-column>
      <el-table-column label="总库存" align="center" prop="totalStock" width="80" />
      <el-table-column label="销量" align="center" prop="sales" width="70" />
      <el-table-column label="状态" align="center" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'">{{ scope.row.status === '0' ? '上架' : '下架' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="170">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['goods:product:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['goods:product:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form :model="form" :rules="rules" ref="productRef" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="商品名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入商品名称" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类ID" prop="categoryId">
              <el-input-number v-model="form.categoryId" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="单位" prop="unit">
              <el-input v-model="form.unit" placeholder="如：件、箱" maxlength="10" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio value="0">上架</el-radio>
                <el-radio value="1">下架</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="最低售价" prop="minPrice">
              <el-input-number v-model="form.minPrice" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最高售价" prop="maxPrice">
              <el-input-number v-model="form.maxPrice" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="总库存" prop="totalStock">
              <el-input-number v-model="form.totalStock" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="副标题">
              <el-input v-model="form.subtitle" placeholder="卖点/副标题" maxlength="200" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="是否新品">
              <el-radio-group v-model="form.isNew">
                <el-radio value="1">是</el-radio>
                <el-radio value="0">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否热销">
              <el-radio-group v-model="form.isHot">
                <el-radio value="1">是</el-radio>
                <el-radio value="0">否</el-radio>
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

<script setup name="Product">
import { getCurrentInstance } from 'vue'
import { listProduct, getProduct, addProduct, updateProduct, delProduct } from '@/api/goods/product'

const { proxy } = getCurrentInstance()

const productList = ref([])
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
    name: [{ required: true, message: '商品名称不能为空', trigger: 'blur' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listProduct(queryParams.value).then(res => {
    productList.value = res.rows
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

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function reset() {
  form.value = {
    id: undefined,
    name: undefined,
    categoryId: undefined,
    unit: undefined,
    minPrice: undefined,
    maxPrice: undefined,
    totalStock: 0,
    status: '0',
    subtitle: undefined,
    isNew: '0',
    isHot: '0'
  }
  proxy.resetForm('productRef')
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '添加商品'
}

function handleUpdate(row) {
  reset()
  const id = row.id || ids.value
  getProduct(id).then(res => {
    form.value = res.data
    open.value = true
    title.value = '修改商品'
  })
}

function handleDelete(row) {
  const idsToDel = row.id || ids.value
  proxy.$modal.confirm('是否确认删除该商品？').then(() => {
    return delProduct(idsToDel)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function submitForm() {
  proxy.$refs['productRef'].validate(valid => {
    if (valid) {
      const submit = form.value.id !== undefined ? updateProduct(form.value) : addProduct(form.value)
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
