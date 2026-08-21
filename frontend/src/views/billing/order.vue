<template>
  <el-card shadow="never">
    <template #header>
      <span class="card-title">收费订单</span>
    </template>

    <el-form inline style="margin-bottom: 12px">
      <el-form-item label="车牌">
        <el-input v-model="query.plateNo" placeholder="车牌号" clearable style="width: 160px" @keyup.enter="load" />
      </el-form-item>
      <el-form-item label="支付方式">
        <el-select v-model="query.payType" placeholder="全部" clearable style="width: 120px">
          <el-option label="现金" :value="1" />
          <el-option label="微信" :value="2" />
          <el-option label="支付宝" :value="3" />
          <el-option label="月卡抵扣" :value="4" />
        </el-select>
      </el-form-item>
      <el-form-item label="开始">
        <el-date-picker v-model="query.startTime" type="datetime" placeholder="开始时间" value-format="YYYY-MM-DD HH:mm:ss" />
      </el-form-item>
      <el-form-item label="结束">
        <el-date-picker v-model="query.endTime" type="datetime" placeholder="结束时间" value-format="YYYY-MM-DD HH:mm:ss" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="records" v-loading="loading">
      <el-table-column prop="orderNo" label="订单号" width="200" />
      <el-table-column prop="plateNo" label="车牌" width="110" />
      <el-table-column label="应收" width="90">
        <template #default="{ row }">¥{{ Number(row.amount).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="减免" width="90">
        <template #default="{ row }">¥{{ Number(row.discount).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="实收" width="90">
        <template #default="{ row }">
          <span style="color: #409eff; font-weight: bold">¥{{ Number(row.paidAmount).toFixed(2) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="支付方式" width="100">
        <template #default="{ row }">{{ payTypeText(row.payType) }}</template>
      </el-table-column>
      <el-table-column prop="payTime" label="支付时间" />
      <el-table-column prop="remark" label="备注" />
      <el-table-column label="操作" width="80">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.page"
      :page-size="query.size"
      :total="total"
      layout="total, prev, pager, next, sizes"
      :page-sizes="[10, 20, 50]"
      style="margin-top: 12px; justify-content: flex-end"
      @current-change="load"
      @size-change="load"
    />

    <el-dialog v-model="detailVisible" title="订单详情" width="560px">
      <el-descriptions v-if="detail" :column="1" border size="small">
        <el-descriptions-item label="订单号">{{ detail.order.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="车牌">{{ detail.order.plateNo }}</el-descriptions-item>
        <el-descriptions-item label="应收金额">¥{{ Number(detail.order.amount).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="减免金额">¥{{ Number(detail.order.discount).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="实收金额">¥{{ Number(detail.order.paidAmount).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ payTypeText(detail.order.payType) }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ detail.order.payTime }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.order.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </el-card>
</template>

<script setup>
defineOptions({ name: 'BillingOrder' })
import { onMounted, reactive, ref } from 'vue'
import { getOrders, getOrderDetail } from '@/api'

const records = ref([])
const total = ref(0)
const loading = ref(false)
const detailVisible = ref(false)
const detail = ref(null)
const query = reactive({ page: 1, size: 10, plateNo: '', payType: null, startTime: '', endTime: '' })

const payTypeText = (t) => ({ 1: '现金', 2: '微信', 3: '支付宝', 4: '月卡抵扣' }[t] || '-')

const load = async () => {
  loading.value = true
  try {
    const params = { ...query }
    if (!params.payType) delete params.payType
    if (!params.startTime) delete params.startTime
    if (!params.endTime) delete params.endTime
    const data = await getOrders(params)
    records.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const reset = () => {
  query.page = 1
  query.plateNo = ''
  query.payType = null
  query.startTime = ''
  query.endTime = ''
  load()
}

const openDetail = async (row) => {
  detail.value = await getOrderDetail(row.orderNo)
  detailVisible.value = true
}

onMounted(load)
</script>

<style scoped>
.card-title {
  font-weight: 600;
}
</style>
