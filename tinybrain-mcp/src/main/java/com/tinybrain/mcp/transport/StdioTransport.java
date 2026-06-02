package com.tinybrain.mcp.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinybrain.mcp.dto.MCPRequest;
import com.tinybrain.mcp.dto.MCPResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Stdio 传输实现
 * <p>
 * 通过标准输入输出与 MCP 服务器通信
 */
@Slf4j
public class StdioTransport implements Transport {

    private final String command;
    private final String[] args;
    private final Map<String, String> env;
    private final ObjectMapper objectMapper;
    private Process process;
    private BufferedWriter writer;
    private BufferedReader reader;
    private volatile boolean alive = false;

    public StdioTransport(String command, String[] args) {
        this(command, args, null);
    }

    public StdioTransport(String command, String[] args, Map<String, String> env) {
        this.command = command;
        this.args = args != null ? args : new String[0];
        this.env = env;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void initialize() throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder();
        // 构建完整命令
        String[] fullCommand = new String[1 + args.length];
        fullCommand[0] = command;
        System.arraycopy(args, 0, fullCommand, 1, args.length);
        processBuilder.command(fullCommand);
        processBuilder.redirectErrorStream(true);

        // 设置环境变量
        if (env != null && !env.isEmpty()) {
            processBuilder.environment().putAll(env);
        }

        log.info("启动 MCP 服务器: {} {}", command, String.join(" ", args));
        process = processBuilder.start();

        writer = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

        alive = true;
        log.info("MCP 服务器启动成功，PID: {}", process.pid());
    }

    @Override
    public MCPResponse sendRequest(MCPRequest request) throws Exception {
        if (!alive || process == null) {
            throw new IllegalStateException("传输连接未初始化或已关闭");
        }

        // 序列化请求
        String requestJson = objectMapper.writeValueAsString(request);
        log.debug("发送 MCP 请求: {}", requestJson);

        // 发送请求
        writer.write(requestJson);
        writer.newLine();
        writer.flush();

        // 读取响应
        String responseLine = reader.readLine();
        if (responseLine == null) {
            throw new IOException("MCP 服务器关闭了连接");
        }

        log.debug("收到 MCP 响应: {}", responseLine);

        // 反序列化响应
        MCPResponse response = objectMapper.readValue(responseLine, MCPResponse.class);

        if (response.hasError()) {
            log.error("MCP 错误: {} - {}", 
                     response.getError().getCode(), 
                     response.getError().getMessage());
        }

        return response;
    }

    @Override
    public void close() {
        alive = false;

        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                log.warn("关闭 writer 失败", e);
            }
        }

        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                log.warn("关闭 reader 失败", e);
            }
        }

        if (process != null) {
            process.destroy();
            try {
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
            }
            log.info("MCP 服务器已关闭");
        }
    }

    @Override
    public boolean isAlive() {
        return alive && process != null && process.isAlive();
    }
}