<template>
  <el-card shadow="never">
    <template #header>
      <span class="card-title">出入场历史</span>
    </template>

    <el-form inline style="margin-bottom: 12px">
      <el-form-item label="车牌">
        <el-input v-model="query.plateNo" placeholder="车牌号" clearable style="width: 180px" @keyup.enter="load" />
      </el-form-item>
      <el-form-item label="开始时间">
        <el-date-picker
          v-model="query.startTime"
          type="datetime"
          placeholder="开始时间"
          value-format="YYYY-MM-DD HH:mm:ss"
        />
      </el-form-item>
      <el-form-item label="结束时间">
        <el-date-picker
          v-model="query.endTime"
          type="datetime"
          placeholder="结束时间"
          value-format="YYYY-MM-DD HH:mm:ss"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="records" v-loading="loading">
      <el-table-column prop="plateNo" label="车牌" width="120" />
      <el-table-column prop="spaceNo" label="车位" width="90" />
      <el-table-column prop="areaName" label="区域" width="100" />
      <el-table-column prop="inTime" label="入场时间" />
      <el-table-column prop="outTime" label="出场时间" />
      <el-table-column label="时长" width="110">
        <template #default="{ row }">{{ formatDuration(row.durationMinutes) }}</template>
      </el-table-column>
      <el-table-column label="类型" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.isMember === 1" type="success" size="small">月卡</el-tag>
          <span v-else>临时</span>
        </template>
      </el-table-column>
      <el-table-column label="应收" width="90">
        <template #default="{ row }">¥{{ Number(row.chargeAmount || 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="减免" width="90">
        <template #default="{ row }">¥{{ Number(row.discountAmount || 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="实收" width="90">
        <template #default="{ row }">
          <span style="color: #409eff; font-weight: bold">¥{{ Number(row.paidAmount || 0).toFixed(2) }}</span>
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
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getHistoryRecords } from '@/api'

const records = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, plateNo: '', startTime: '', endTime: '' })

const formatDuration = (minutes) => {
  if (minutes == null) return '-'
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return h > 0 ? `${h}小时${m}分` : `${m}分钟`
}

const load = async () => {
  loading.value = true
  try {
    const params = { ...query }
    if (!params.startTime) delete params.startTime
    if (!params.endTime) delete params.endTime
    const data = await getHistoryRecords(params)
    records.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const reset = () => {
  query.page = 1
  query.plateNo = ''
  query.startTime = ''
  query.endTime = ''
  load()
}

onMounted(load)
</script>

<style scoped>
.card-title {
  font-weight: 600;
}
</style>
