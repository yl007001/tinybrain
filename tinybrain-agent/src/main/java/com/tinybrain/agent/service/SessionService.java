package com.tinybrain.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tinybrain.common.entity.Message;
import com.tinybrain.common.entity.Session;
import com.tinybrain.common.mapper.MessageMapper;
import com.tinybrain.common.mapper.SessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 会话管理服务
 * <p>
 * 负责会话和消息的 CRUD 操作，按 updateTime 降序排列。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionMapper sessionMapper;
    private final MessageMapper messageMapper;

    /**
     * 创建新会话
     * <p>
     * title 默认取用户第一条消息的前20个字符
     *
     * @param userId       用户ID
     * @param firstMessage 用户第一条消息（可为空，后续保存消息时再更新 title）
     * @return 新建的 Session 对象
     */
    public Session createSession(Long userId, String firstMessage) {
        Session session = new Session();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setType("agent");

        // title 默认取用户第一条消息前20个字符
        if (firstMessage != null && !firstMessage.isBlank()) {
            String title = firstMessage.length() <= 20 ? firstMessage : firstMessage.substring(0, 20);
            session.setTitle(title);
        } else {
            session.setTitle("新对话");
        }

        sessionMapper.insert(session);
        log.info("创建会话: sessionId={}, userId={}", session.getSessionId(), userId);
        return session;
    }

    /**
     * 查询当前用户的所有会话，按 updateTime 降序排列
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    public List<Session> listSessions(Long userId) {
        return sessionMapper.selectList(
                new LambdaQueryWrapper<Session>()
                        .eq(Session::getUserId, userId)
                        .orderByDesc(Session::getUpdateTime)
        );
    }

    /**
     * 删除单个会话（级联删除关联消息）
     *
     * @param sessionId 会话唯一标识
     * @param userId    用户ID（权限校验）
     */
    public void deleteSession(String sessionId, Long userId) {
        // 删除会话下的所有消息
        messageMapper.delete(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getSessionId, sessionId)
        );
        // 删除会话（@TableLogic 逻辑删除）
        sessionMapper.delete(
                new LambdaQueryWrapper<Session>()
                        .eq(Session::getSessionId, sessionId)
                        .eq(Session::getUserId, userId)
        );
        log.info("删除会话: sessionId={}, userId={}", sessionId, userId);
    }

    /**
     * 批量删除会话（级联删除关联消息）
     *
     * @param sessionIds 会话ID列表
     * @param userId     用户ID（权限校验）
     */
    public void batchDeleteSessions(List<String> sessionIds, Long userId) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return;
        }
        // 批量删除消息
        messageMapper.delete(
                new LambdaQueryWrapper<Message>()
                        .in(Message::getSessionId, sessionIds)
        );
        // 批量删除会话
        sessionMapper.delete(
                new LambdaQueryWrapper<Session>()
                        .in(Session::getSessionId, sessionIds)
                        .eq(Session::getUserId, userId)
        );
        log.info("批量删除会话: sessionIds={}, userId={}", sessionIds, userId);
    }

    /**
     * 保存消息到数据库
     *
     * @param sessionId  会话ID
     * @param userId     用户ID
     * @param role       角色（user/assistant/system/tool）
     * @param content    消息内容
     * @param toolCalls  工具调用记录（JSON，可为空）
     */
    public void saveMessage(String sessionId, Long userId, String role, String content, String toolCalls) {
        Message message = new Message();
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setRole(role);
        message.setContent(content);
        message.setToolCalls(toolCalls);
        messageMapper.insert(message);
    }

    /**
     * 获取会话的消息列表，按创建时间升序排列
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    public List<Message> getMessages(String sessionId) {
        return messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getSessionId, sessionId)
                        .orderByAsc(Message::getCreateTime)
        );
    }

    /**
     * 获取会话的最近 N 条消息，按创建时间升序排列
     *
     * @param sessionId 会话ID
     * @param limit     最大条数
     * @return 消息列表
     */
    public List<Message> getRecentMessages(String sessionId, int limit) {
        // 先按 createTime 降序取 limit 条，再反转为升序
        List<Message> messages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getSessionId, sessionId)
                        .orderByDesc(Message::getCreateTime)
                        .last("LIMIT " + limit)
        );
        java.util.Collections.reverse(messages);
        return messages;
    }

    /**
     * 根据 sessionId 查询会话
     *
     * @param sessionId 会话ID
     * @return Session 或 null
     */
    public Session getSession(String sessionId) {
        return sessionMapper.selectOne(
                new LambdaQueryWrapper<Session>()
                        .eq(Session::getSessionId, sessionId)
        );
    }
}
