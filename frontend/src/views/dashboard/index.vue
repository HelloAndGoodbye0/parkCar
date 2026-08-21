<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">总车位</div>
          <div class="stat-value">{{ overview.total || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">空闲车位</div>
          <div class="stat-value" style="color: #67c23a">{{ overview.free || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">占用车位</div>
          <div class="stat-value" style="color: #e6a23c">{{ overview.occupied || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">在场车辆</div>
          <div class="stat-value" style="color: #409eff">{{ currentRecords.total || 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>各区域车位占用</span>
            </div>
          </template>
          <el-table :data="overview.byArea || []" size="default">
            <el-table-column prop="areaName" label="区域" />
            <el-table-column prop="total" label="车位数" width="90" />
            <el-table-column prop="free" label="空闲" width="90" />
            <el-table-column prop="occupied" label="占用" width="90" />
            <el-table-column label="占用率" width="140">
              <template #default="{ row }">
                <el-progress
                  :percentage="row.total ? Math.round((row.occupied / row.total) * 100) : 0"
                  :color="row.total && row.occupied / row.total > 0.8 ? '#f56c6c' : '#409eff'"
                />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>快捷操作</span>
            </div>
          </template>
          <div class="quick-actions">
            <el-button type="primary" size="large" @click="$router.push('/parking/in')">
              车辆入场
            </el-button>
            <el-button type="warning" size="large" @click="$router.push('/parking/out')">
              车辆出场
            </el-button>
            <el-button size="large" @click="$router.push('/parking/current')">
              在场车辆
            </el-button>
            <el-button size="large" @click="$router.push('/report/index')">
              统计报表
            </el-button>
          </div>
        </el-card>

        <el-card shadow="never" style="margin-top: 16px">
          <template #header>
            <div class="card-header">
              <span>最近入场车辆</span>
            </div>
          </template>
          <el-table :data="(currentRecords.records || []).slice(0, 6)" size="small">
            <el-table-column prop="plateNo" label="车牌" width="110" />
            <el-table-column prop="spaceNo" label="车位" width="80" />
            <el-table-column prop="inTime" label="入场时间" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
defineOptions({ name: 'Dashboard' })
import { onMounted, ref } from 'vue'
import { getSpaceOverview, getCurrentRecords } from '@/api'

const overview = ref({})
const currentRecords = ref({})

onMounted(async () => {
  overview.value = await getSpaceOverview()
  currentRecords.value = await getCurrentRecords({ page: 1, size: 10 })
})
</script>

<style scoped>
.stat-card {
  text-align: center;
}

.stat-title {
  color: #909399;
  font-size: 14px;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  margin-top: 8px;
  color: #303133;
}

.card-header {
  font-weight: 600;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
</style>
