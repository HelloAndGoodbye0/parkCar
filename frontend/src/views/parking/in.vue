<template>
  <el-row :gutter="16">
    <el-col :span="10">
      <el-card shadow="never">
        <template #header>
          <span class="card-title">车辆入场</span>
        </template>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
          <el-form-item label="车牌号" prop="plateNo">
            <el-input
              v-model="form.plateNo"
              placeholder="如：京A12345"
              size="large"
              maxlength="20"
              @keyup.enter="onSubmit"
            />
          </el-form-item>
          <el-form-item label="区域">
            <el-select v-model="form.areaId" placeholder="不选则全场查找" clearable style="width: 100%">
              <el-option v-for="a in areas" :key="a.id" :label="a.name" :value="a.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="指定车位">
            <el-select
              v-model="form.spaceId"
              placeholder="不选则自动分配空闲车位"
              clearable
              filterable
              style="width: 100%"
            >
              <el-option
                v-for="s in freeSpaces"
                :key="s.id"
                :label="`${s.spaceNo}（${areaName(s.areaId)}）`"
                :value="s.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="form.remark" placeholder="选填" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="onSubmit">入场登记</el-button>
            <el-button @click="reset">重置</el-button>
          </el-form-item>
        </el-form>

        <el-alert
          v-if="result"
          :type="result.inBlacklist ? 'warning' : 'success'"
          :closable="false"
          style="margin-top: 12px"
        >
          <template #title>
            车牌 <b>{{ result.plateNo }}</b> 已入场，车位 <b>{{ result.spaceNo }}</b>
            <span v-if="result.isMember">（月卡车辆）</span>
            <span v-if="result.inBlacklist">，⚠️ 该车在黑名单中！</span>
          </template>
        </el-alert>
      </el-card>
    </el-col>

    <el-col :span="14">
      <el-card shadow="never">
        <template #header>
          <span class="card-title">当前在场车辆</span>
        </template>
        <div style="margin-bottom: 12px; display: flex; gap: 8px">
          <el-input
            v-model="query.plateNo"
            placeholder="按车牌查询"
            clearable
            style="width: 220px"
            @keyup.enter="load"
          />
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="load">刷新</el-button>
        </div>
        <el-table :data="records" v-loading="tableLoading" size="small">
          <el-table-column prop="plateNo" label="车牌" width="110" />
          <el-table-column prop="spaceNo" label="车位" width="80" />
          <el-table-column prop="areaName" label="区域" width="90" />
          <el-table-column prop="inTime" label="入场时间" />
          <el-table-column label="时长" width="100">
            <template #default="{ row }">{{ formatDuration(row.durationMinutes) }}</template>
          </el-table-column>
          <el-table-column label="月卡" width="70">
            <template #default="{ row }">
              <el-tag v-if="row.isMember === 1" type="success" size="small">月卡</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button link type="primary" @click="goOut(row.plateNo)">出场</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.size"
          :total="total"
          layout="total, prev, pager, next"
          style="margin-top: 12px; justify-content: flex-end"
          @current-change="load"
        />
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { vehicleIn, getAreas, getSpaces, getCurrentRecords } from '@/api'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const tableLoading = ref(false)
const areas = ref([])
const spaces = ref([])
const records = ref([])
const total = ref(0)
const result = ref(null)

const form = reactive({ plateNo: '', areaId: null, spaceId: null, remark: '' })
const query = reactive({ page: 1, size: 10, plateNo: '' })

const rules = {
  plateNo: [{ required: true, message: '请输入车牌号', trigger: 'blur' }]
}

const freeSpaces = computed(() => spaces.value.filter((s) => s.status === 0))

const areaName = (id) => areas.value.find((a) => a.id === id)?.name || ''

const formatDuration = (minutes) => {
  if (minutes == null) return '-'
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return h > 0 ? `${h}小时${m}分` : `${m}分钟`
}

const load = async () => {
  tableLoading.value = true
  try {
    const data = await getCurrentRecords(query)
    records.value = data.records
    total.value = data.total
  } finally {
    tableLoading.value = false
  }
}

const loadAreas = async () => {
  areas.value = await getAreas()
}

const loadSpaces = async () => {
  const data = await getSpaces({ page: 1, size: 200 })
  spaces.value = data.records
}

const onSubmit = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    result.value = await vehicleIn({
      plateNo: form.plateNo,
      spaceId: form.spaceId,
      areaId: form.areaId,
      remark: form.remark
    })
    ElMessage.success('入场成功')
    if (result.value.inBlacklist) {
      ElMessage.warning('该车辆在黑名单中，请留意！')
    }
    await Promise.all([load(), loadSpaces()])
    form.plateNo = ''
    form.spaceId = null
    form.remark = ''
  } catch (e) {
    /* 提示由拦截器处理 */
  } finally {
    loading.value = false
  }
}

const reset = () => {
  form.plateNo = ''
  form.areaId = null
  form.spaceId = null
  form.remark = ''
  result.value = null
}

const goOut = (plateNo) => {
  router.push({ path: '/parking/out', query: { plateNo } })
}

onMounted(async () => {
  await Promise.all([loadAreas(), loadSpaces()])
  await load()
})
</script>

<style scoped>
.card-title {
  font-weight: 600;
}
</style>
