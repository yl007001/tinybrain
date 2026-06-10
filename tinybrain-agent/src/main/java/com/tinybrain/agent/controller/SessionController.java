package com.tinybrain.agent.controller;

import com.tinybrain.common.entity.Message;
import com.tinybrain.common.entity.Session;
import com.tinybrain.agent.service.SessionService;
import com.tinybrain.common.response.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 会话管理控制器
 * <p>
 * 提供会话的增删查、消息查询接口。
 */
@Tag(name = "05-会话管理", description = "对话会话的列表查询、删除、消息查看")
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @Operation(summary = "获取当前用户的所有会话")
    @GetMapping("/list")
    public R<List<Session>> listSessions(HttpServletRequest request) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<Session> sessions = sessionService.listSessions(userId);
        return R.ok(sessions);
    }

    @Operation(summary = "获取会话的消息列表")
    @GetMapping("/{sessionId}/messages")
    public R<List<Message>> getMessages(@PathVariable String sessionId) {
        List<Message> messages = sessionService.getMessages(sessionId);
        return R.ok(messages);
    }

    @Operation(summary = "删除单个会话")
    @DeleteMapping("/{sessionId}")
    public R<Void> deleteSession(@PathVariable String sessionId, HttpServletRequest request) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        sessionService.deleteSession(sessionId, userId);
        return R.okMsg("会话已删除");
    }

    @Operation(summary = "批量删除会话")
    @PostMapping("/batch-delete")
    public R<Void> batchDeleteSessions(@RequestBody Map<String, List<String>> body,
                                        HttpServletRequest request) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<String> sessionIds = body.get("sessionIds");
        sessionService.batchDeleteSessions(sessionIds, userId);
        return R.okMsg("批量删除成功");
    }
}
