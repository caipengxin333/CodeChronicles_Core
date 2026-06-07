package com.codechronicles.core.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 使用手机号作为唯一会话 ID，将单个用户的聊天消息保存为 Redis List。
 */
@Repository
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    public static final int MAX_MESSAGES = 40;
    private static final Duration CHAT_MEMORY_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String keyPrefix;

    public RedisChatMemoryRepository(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${blog.redis.key-prefix}") String keyPrefix
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.keyPrefix = keyPrefix + ":chat:memory:";
    }

    /**
     * MessageWindowChatMemory 会传入完整窗口；这里覆盖 Redis List，并再次限制为最后 40 条。
     */
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        String key = chatMemoryKey(conversationId);
        int fromIndex = Math.max(0, messages.size() - MAX_MESSAGES);
        List<String> serializedMessages = messages.subList(fromIndex, messages.size()).stream()
                .map(this::serialize)
                .toList();

        redisTemplate.delete(key);
        if (serializedMessages.isEmpty()) {
            return;
        }
        redisTemplate.opsForList().rightPushAll(key, serializedMessages);
        redisTemplate.expire(key, CHAT_MEMORY_TTL);
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        List<String> rawMessages = redisTemplate.opsForList().range(
                chatMemoryKey(conversationId),
                -MAX_MESSAGES,
                -1
        );
        if (rawMessages == null || rawMessages.isEmpty()) {
            return List.of();
        }

        List<Message> messages = new ArrayList<>();
        for (String raw : rawMessages) {
            try {
                MessageRecord record = objectMapper.readValue(raw, MessageRecord.class);
                Message message = toMessage(record);
                if (message != null) {
                    messages.add(message);
                }
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("聊天记录反序列化失败", exception);
            }
        }
        return List.copyOf(messages);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        redisTemplate.delete(chatMemoryKey(conversationId));
    }

    @Override
    public List<String> findConversationIds() {
        Set<String> keys = redisTemplate.keys(keyPrefix + "*");
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        return keys.stream()
                .map(key -> key.substring(keyPrefix.length()))
                .sorted()
                .toList();
    }

    private String serialize(Message message) {
        try {
            return objectMapper.writeValueAsString(new MessageRecord(
                    message.getMessageType().name(),
                    message.getText()
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("聊天记录序列化失败", exception);
        }
    }

    private Message toMessage(MessageRecord record) {
        return switch (record.role()) {
            case "USER" -> new UserMessage(record.content());
            case "ASSISTANT" -> new AssistantMessage(record.content());
            case "SYSTEM" -> new SystemMessage(record.content());
            default -> null;
        };
    }

    private String chatMemoryKey(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("聊天会话手机号不能为空");
        }
        return keyPrefix + phone.trim();
    }

    record MessageRecord(String role, String content) {
    }
}
