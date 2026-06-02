<template>
  <div class="mcp-manager">
    <!-- 顶部操作栏 -->
    <div class="action-bar">
      <el-button type="primary" @click="showAddDialog">
        <el-icon><Plus /></el-icon>
        添加 MCP 服务器
      </el-button>
      <el-button @click="refreshServers">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <!-- 服务器列表 -->
    <el-table :data="servers" style="width: 100%" v-loading="loading">
      <el-table-column prop="name" label="服务器名称" width="180" />
      <el-table-column prop="description" label="描述" min-width="200" />
      <el-table-column prop="transportType" label="传输类型" width="100">
        <template #default="{ row }">
          <el-tag :type="getTransportTagType(row.transportType)">
            {{ row.transportType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="getStatusTagType(row.status)">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="toolCount" label="工具数量" width="100" />
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <el-button size="small" @click="testConnection(row)">测试连接</el-button>
          <el-button size="small" @click="viewTools(row)">查看工具</el-button>
          <el-button size="small" type="danger" @click="deleteServer(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加服务器对话框 -->
    <el-dialog v-model="addDialogVisible" title="添加 MCP 服务器" width="600px">
      <el-form :model="serverForm" label-width="120px">
        <el-form-item label="服务器名称" required>
          <el-input v-model="serverForm.name" placeholder="例如：filesystem-server" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="serverForm.description" placeholder="服务器功能描述" />
        </el-form-item>
        <el-form-item label="传输类型" required>
          <el-select v-model="serverForm.transportType" style="width: 100%">
            <el-option label="Stdio (标准输入输出)" value="stdio" />
            <el-option label="SSE (Server-Sent Events)" value="sse" disabled />
            <el-option label="HTTP" value="http" disabled />
          </el-select>
        </el-form-item>
        <el-form-item v-if="serverForm.transportType === 'stdio'" label="命令" required>
          <el-input v-model="serverForm.command" placeholder="例如：npx" />
        </el-form-item>
        <el-form-item v-if="serverForm.transportType === 'stdio'" label="参数">
          <el-input v-model="serverForm.argsStr" placeholder="多个参数用空格分隔，例如：@modelcontextprotocol/server-filesystem /tmp" />
        </el-form-item>
        <el-form-item label="自动连接">
          <el-switch v-model="serverForm.autoConnect" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="addServer" :loading="adding">添加</el-button>
      </template>
    </el-dialog>

    <!-- 工具列表对话框 -->
    <el-dialog v-model="toolsDialogVisible" title="MCP 工具列表" width="700px">
      <div v-if="currentTools.length === 0" class="empty-tools">
        <el-empty description="暂无工具" />
      </div>
      <el-collapse v-else>
        <el-collapse-item v-for="tool in currentTools" :key="tool.name" :title="tool.name">
          <div class="tool-detail">
            <p><strong>描述：</strong>{{ tool.description }}</p>
            <p v-if="tool.inputSchema"><strong>参数：</strong></p>
            <pre v-if="tool.inputSchema">{{ JSON.stringify(tool.inputSchema, null, 2) }}</pre>
          </div>
        </el-collapse-item>
      </el-collapse>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import http from '@/api/index'

interface McpServer {
  id: string
  name: string
  description: string
  transportType: string
  status: string
  toolCount: number
  tools: any[]
}

const servers = ref<McpServer[]>([])
const loading = ref(false)
const addDialogVisible = ref(false)
const toolsDialogVisible = ref(false)
const adding = ref(false)
const currentTools = ref<any[]>([])

const serverForm = ref({
  name: '',
  description: '',
  transportType: 'stdio',
  command: '',
  argsStr: '',
  autoConnect: true
})

// 获取服务器列表
async function refreshServers() {
  loading.value = true
  try {
    const res: any = await http.get('/mcp/servers')
    if (res?.data) {
      servers.value = res.data
    }
  } catch (error) {
    console.error('获取服务器列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 显示添加对话框
function showAddDialog() {
  serverForm.value = {
    name: '',
    description: '',
    transportType: 'stdio',
    command: '',
    argsStr: '',
    autoConnect: true
  }
  addDialogVisible.value = true
}

// 添加服务器
async function addServer() {
  if (!serverForm.value.name || !serverForm.value.command) {
    ElMessage.warning('请填写必填项')
    return
  }

  adding.value = true
  try {
    const args = serverForm.value.argsStr ? serverForm.value.argsStr.split(' ').filter(Boolean) : []
    await http.post('/mcp/servers', {
      name: serverForm.value.name,
      description: serverForm.value.description,
      transportType: serverForm.value.transportType,
      command: serverForm.value.command,
      args: args,
      autoConnect: serverForm.value.autoConnect
    })
    ElMessage.success('MCP 服务器添加成功')
    addDialogVisible.value = false
    refreshServers()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '添加失败')
  } finally {
    adding.value = false
  }
}

// 测试连接
async function testConnection(server: McpServer) {
  try {
    const res: any = await http.post(`/mcp/servers/${server.id}/test`)
    if (res?.data?.success) {
      ElMessage.success(`连接成功！发现 ${res.data.toolCount} 个工具`)
    } else {
      ElMessage.error(`连接失败：${res?.data?.error || '未知错误'}`)
    }
  } catch (error: any) {
    ElMessage.error(`测试失败：${error.response?.data?.message || error.message}`)
  }
}

// 查看工具
async function viewTools(server: McpServer) {
  try {
    const res: any = await http.get(`/mcp/servers/${server.id}/tools`)
    if (res?.data) {
      currentTools.value = res.data
      toolsDialogVisible.value = true
    }
  } catch (error: any) {
    ElMessage.error('获取工具列表失败')
  }
}

// 删除服务器
async function deleteServer(server: McpServer) {
  try {
    await ElMessageBox.confirm(`确定要删除服务器 "${server.name}" 吗？`, '确认删除', {
      type: 'warning'
    })
    await http.delete(`/mcp/servers/${server.id}`)
    ElMessage.success('删除成功')
    refreshServers()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 获取传输类型标签类型
function getTransportTagType(type: string) {
  switch (type) {
    case 'stdio': return 'primary'
    case 'sse': return 'success'
    case 'http': return 'warning'
    default: return 'info'
  }
}

// 获取状态标签类型
function getStatusTagType(status: string) {
  switch (status) {
    case 'connected': return 'success'
    case 'disconnected': return 'info'
    case 'error': return 'danger'
    default: return 'info'
  }
}

// 获取状态文本
function getStatusText(status: string) {
  switch (status) {
    case 'connected': return '已连接'
    case 'disconnected': return '未连接'
    case 'error': return '连接错误'
    default: return '未知'
  }
}

onMounted(refreshServers)
</script>

<style scoped>
.mcp-manager {
  padding: 20px;
}

.action-bar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}

.tool-detail {
  padding: 10px;
}

.tool-detail pre {
  background: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
  overflow-x: auto;
}

.empty-tools {
  padding: 40px 0;
}
</style>
