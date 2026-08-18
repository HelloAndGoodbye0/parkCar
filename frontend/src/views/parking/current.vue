<template>
  <el-card shadow="never">
    <template #header>
      <span class="card-title">在场车辆</span>
    </template>

    <div style="margin-bottom: 12px; display: flex; gap: 8px; flex-wrap: wrap">
      <el-input
        v-model="query.plateNo"
        placeholder="按车牌查询"
        clearable
        style="width: 220px"
        @keyup.enter="load"
      />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <el-table :data="records" v-loading="loading">
      <el-table-column prop="plateNo" label="车牌" width="120" />
      <el-table-column prop="spaceNo" label="车位" width="90" />
      <el-table-column prop="areaName" label="区域" width="100" />
      <el-table-column prop="inTime" label="入场时间" />
      <el-table-column label="时长" width="110">
        <template #default="{ row }">{{ formatDuration(row.durationMinutes) }}</template>
      </el-table-column>
      <el-table-column label="月卡" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.isMember === 1" type="success" size="small">月卡</el-tag>
          <span v-else>临时车</span>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="toOut(row.plateNo)">出场</el-button>
          <el-button link type="danger" @click="onManualOut(row)">手工出场</el-button>
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
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCurrentRecords, manualOut } from '@/api'

const router = useRouter()
const records = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, plateNo: '' })

const formatDuration = (minutes) => {
  if (minutes == null) return '-'
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return h > 0 ? `${h}小时${m}分` : `${m}分钟`
}

const load = async () => {
  loading.value = true
  try {
    const data = await getCurrentRecords(query)
    records.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const reset = () => {
  query.page = 1
  query.plateNo = ''
  load()
}

const toOut = (plateNo) => {
  router.push({ path: '/parking/out', query: { plateNo } })
}

const onManualOut = (row) => {
  ElMessageBox.confirm(
    `确定手工完结车辆 ${row.plateNo} 的在场记录吗？此操作不计费并直接释放车位。`,
    '手工出场',
    { type: 'warning' }
  )
    .then(async () => {
      await manualOut(row.id, { remark: '手工出场' })
      ElMessage.success('已手工完结')
      load()
    })
    .catch(() => {})
}

onMounted(load)
</script>

<style scoped>
.card-title {
  font-weight: 600;
}
</style>
