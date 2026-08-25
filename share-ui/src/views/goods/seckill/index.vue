<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="活动名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="未开始" value="0" />
          <el-option label="进行中" value="1" />
          <el-option label="已结束" value="2" />
          <el-option label="已禁用" value="3" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['goods:seckill:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['goods:seckill:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['goods:seckill:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange" border stripe>
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="活动名称" align="center" prop="name" width="160" :show-overflow-tooltip="true" />
      <el-table-column label="商品名称" align="center" prop="productName" width="140" :show-overflow-tooltip="true" />
      <el-table-column label="秒杀价" align="center" prop="seckillPrice" width="90">
        <template #default="scope">¥{{ scope.row.seckillPrice }}</template>
      </el-table-column>
      <el-table-column label="库存" align="center" prop="stock" width="60" />
      <el-table-column label="限购" align="center" prop="limitPerUser" width="60" />
      <el-table-column label="状态" align="center" width="80">
        <template #default="scope">
          <el-tag :type="statusType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="活动时间" align="center" width="160">
        <template #default="scope">{{ parseTime(scope.row.startTime) }} ~ {{ parseTime(scope.row.endTime) }}</template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['goods:seckill:edit']">修改</el-button>
          <el-button link type="primary" icon="Refresh" @click="handleEnable(scope.row)" v-if="scope.row.status === '0'" v-hasPermi="['goods:seckill:edit']">启用</el-button>
          <el-button link type="danger" icon="CircleClose" @click="handleDisable(scope.row)" v-if="scope.row.status === '0' || scope.row.status === '1'" v-hasPermi="['goods:seckill:edit']">禁用</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['goods:seckill:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="650px" append-to-body>
      <el-form :model="form" :rules="rules" ref="seckillRef" label-width="110px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="活动名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="秒杀价" prop="seckillPrice">
              <el-input-number v-model="form.seckillPrice" :min="0.01" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="商品" prop="productId">
              <el-select v-model="form.productId" placeholder="请选择" filterable style="width: 100%" @change="onProductChange">
                <el-option v-for="p in productList" :key="p.id" :label="p.name" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="SKU" prop="skuId">
              <el-select v-model="form.skuId" placeholder="请选择" filterable style="width: 100%">
                <el-option v-for="s in skuList" :key="s.id" :label="s.specs || '默认'" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="库存" prop="stock">
              <el-input-number v-model="form.stock" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="每人限购" prop="limitPerUser">
              <el-input-number v-model="form.limitPerUser" :min="1" :max="99" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="排序" prop="sort">
              <el-input-number v-model="form.sort" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商家" prop="merchantId">
              <el-select v-model="form.merchantId" placeholder="请选择" filterable style="width: 100%">
                <el-option v-for="m in merchantList" :key="m.id" :label="m.name" :value="m.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker v-model="form.startTime" type="datetime" placeholder="选择" style="width: 100%" value-format="YYYY-MM-DD HH:mm:ss" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker v-model="form.endTime" type="datetime" placeholder="选择" style="width: 100%" value-format="YYYY-MM-DD HH:mm:ss" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选" />
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

<script setup name="Seckill">
import { getCurrentInstance, onMounted } from 'vue'
import { listSeckill, getSeckill, addSeckill, updateSeckill, updateSeckillStatus, delSeckill } from '@/api/goods/seckill'
import { listProduct } from '@/api/goods/product'

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
const productList = ref([])
const skuList = ref([])
const merchantList = ref([])

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    status: undefined
  },
  rules: {
    name: [{ required: true, message: '活动名称不能为空', trigger: 'blur' }],
    productId: [{ required: true, message: '请选择商品', trigger: 'change' }],
    skuId: [{ required: true, message: '请选择SKU', trigger: 'change' }],
    seckillPrice: [{ required: true, message: '请输入秒杀价', trigger: 'blur' }],
    stock: [{ required: true, message: '请输入库存', trigger: 'blur' }],
    startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
    endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
    merchantId: [{ required: true, message: '请选择商家', trigger: 'change' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

function statusType(s) {
  return { '0': 'info', '1': 'success', '2': 'warning', '3': 'danger' }[s] || 'info'
}

function statusLabel(s) {
  return { '0': '未开始', '1': '进行中', '2': '已结束', '3': '已禁用' }[s] || s
}

function getList() {
  loading.value = true
  listSeckill(queryParams.value).then(res => {
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

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function onProductChange(productId) {
  form.value.skuId = undefined
  if (productId) {
    import('@/api/goods/product').then(m => m.listSkuByProduct(productId)).then(res => {
      skuList.value = res.data || []
    })
  } else {
    skuList.value = []
  }
}

function reset() {
  form.value = {
    id: undefined,
    name: undefined,
    productId: undefined,
    skuId: undefined,
    seckillPrice: undefined,
    stock: 0,
    limitPerUser: 1,
    sort: 0,
    merchantId: undefined,
    startTime: undefined,
    endTime: undefined,
    status: '0',
    remark: undefined
  }
  proxy.resetForm('seckillRef')
  skuList.value = []
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '新增秒杀活动'
}

function handleUpdate(row) {
  reset()
  const id = row.id || ids.value
  getSeckill(id).then(res => {
    form.value = res.data
    open.value = true
    title.value = '修改秒杀活动'
    // 加载该商品的 SKU 列表
    if (form.value.productId) {
      import('@/api/goods/product').then(m => m.listSkuByProduct(form.value.productId)).then(res => {
        skuList.value = res.data || []
      })
    }
  })
}

function handleEnable(row) {
  proxy.$modal.confirm('确认启用该秒杀活动？').then(() => {
    return updateSeckillStatus({ id: row.id, status: '1' })
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('启用成功')
  }).catch(() => {})
}

function handleDisable(row) {
  proxy.$modal.confirm('禁用后活动将不可参与，确认？').then(() => {
    return updateSeckillStatus({ id: row.id, status: '3' })
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('已禁用')
  }).catch(() => {})
}

function handleDelete(row) {
  const idsToDel = row.id || ids.value
  proxy.$modal.confirm('确认删除该活动？').then(() => {
    return delSeckill(idsToDel)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function submitForm() {
  proxy.$refs['seckillRef'].validate(valid => {
    if (valid) {
      const submit = form.value.id !== undefined ? updateSeckill(form.value) : addSeckill(form.value)
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

onMounted(() => {
  listProduct().then(res => { productList.value = res.rows || [] })
  import('@/api/merchant/info').then(m => m.listMerchantInfo({ pageNum: 1, pageSize: 999 })).then(res => {
    merchantList.value = res.rows || []
  })
})

getList()
</script>
