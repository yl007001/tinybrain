package com.tinybrain.agent.controller;

import com.tinybrain.agent.dto.McpServerConfig;
import com.tinybrain.agent.dto.McpServerInfo;
import com.tinybrain.agent.service.McpService;
import com.tinybrain.common.response.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * MCP 服务器管理控制器
 * <p>
 * 提供 MCP 服务器的配置、连接、工具发现等功能。
 */
@Tag(name = "05-MCP 管理", description = "MCP 服务器配置、连接管理、工具发现")
@RestController
@RequestMapping("/api/mcp")
@RequiredArgsConstructor
public class McpController {

    private final McpService mcpService;

    @Operation(summary = "获取所有 MCP 服务器", description = "获取已配置的所有 MCP 服务器列表及其状态")
    @GetMapping("/servers")
    public R<List<McpServerInfo>> listServers() {
        return R.ok(mcpService.listServers());
    }

    @Operation(summary = "添加 MCP 服务器", description = "添加一个新的 MCP 服务器配置")
    @PostMapping("/servers")
    public R<McpServerInfo> addServer(@Valid @RequestBody McpServerConfig config) {
        return R.ok(mcpService.addServer(config));
    }

    @Operation(summary = "更新 MCP 服务器", description = "更新指定 MCP 服务器的配置")
    @PutMapping("/servers/{id}")
    public R<McpServerInfo> updateServer(@PathVariable String id, @Valid @RequestBody McpServerConfig config) {
        return R.ok(mcpService.updateServer(id, config));
    }

    @Operation(summary = "删除 MCP 服务器", description = "删除指定的 MCP 服务器配置")
    @DeleteMapping("/servers/{id}")
    public R<Void> deleteServer(@PathVariable String id) {
        mcpService.deleteServer(id);
        return R.okMsg("MCP 服务器已删除");
    }

    @Operation(summary = "测试 MCP 服务器连接", description = "测试与指定 MCP 服务器的连接")
    @PostMapping("/servers/{id}/test")
    public R<Map<String, Object>> testConnection(@PathVariable String id) {
        return R.ok(mcpService.testConnection(id));
    }

    @Operation(summary = "获取 MCP 服务器工具列表", description = "获取指定 MCP 服务器提供的所有工具")
    @GetMapping("/servers/{id}/tools")
    public R<List<Map<String, Object>>> getServerTools(@PathVariable String id) {
        return R.ok(mcpService.getServerTools(id));
    }

    @Operation(summary = "调用 MCP 工具", description = "调用指定 MCP 服务器上的工具")
    @PostMapping("/servers/{id}/tools/{toolName}/call")
    public R<String> callTool(
            @PathVariable String id,
            @PathVariable String toolName,
            @RequestBody Map<String, Object> arguments) {
        return R.ok(mcpService.callTool(id, toolName, arguments));
    }
}
