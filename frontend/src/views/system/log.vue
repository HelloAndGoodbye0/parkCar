<template>
  <el-card shadow="never">
    <template #header>
      <span class="card-title">操作日志</span>
    </template>

    <el-form inline style="margin-bottom: 12px">
      <el-form-item label="操作人">
        <el-input v-model="query.username" placeholder="用户名" clearable style="width: 160px" @keyup.enter="load" />
      </el-form-item>
      <el-form-item label="模块">
        <el-select v-model="query.module" placeholder="全部" clearable style="width: 140px">
          <el-option label="认证" value="认证" />
          <el-option label="系统管理" value="系统管理" />
          <el-option label="车位管理" value="车位管理" />
          <el-option label="出入场" value="出入场" />
          <el-option label="收费管理" value="收费管理" />
          <el-option label="会员月卡" value="会员月卡" />
          <el-option label="黑名单" value="黑名单" />
        </el-select>
      </el-form-item>
      <el-form-item label="开始">
        <el-date-picker v-model="query.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
      </el-form-item>
      <el-form-item label="结束">
        <el-date-picker v-model="query.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="records" v-loading="loading">
      <el-table-column prop="username" label="操作人" width="110" />
      <el-table-column prop="module" label="模块" width="100" />
      <el-table-column prop="action" label="操作" width="110" />
      <el-table-column prop="content" label="内容" />
      <el-table-column prop="ip" label="IP" width="120" />
      <el-table-column prop="createTime" label="时间" width="180" />
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
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getLogs } from '@/api'

const records = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, username: '', module: '', startTime: '', endTime: '' })

const load = async () => {
  loading.value = true
  try {
    const params = { ...query }
    if (!params.module) delete params.module
    if (!params.startTime) delete params.startTime
    if (!params.endTime) delete params.endTime
    const data = await getLogs(params)
    records.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const reset = () => {
  query.page = 1
  query.username = ''
  query.module = ''
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
