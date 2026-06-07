package com.codechronicles.core.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisChatMemoryRepositoryTest {

    private static final String PHONE = "13800138000";
    private static final String REDIS_KEY = "cc:test:chat:memory:" + PHONE;

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ListOperations<String, String> listOperations = mock(ListOperations.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private RedisChatMemoryRepository repository;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        repository = new RedisChatMemoryRepository(redisTemplate, objectMapper, "cc:test");
    }

    @Test
    void shouldKeepOnlyLastFortyMessagesForPhone() throws Exception {
        List<Message> messages = new ArrayList<>();
        for (int index = 0; index < 45; index++) {
            messages.add(new UserMessage("message-" + index));
        }

        repository.saveAll(PHONE, messages);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> valuesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(redisTemplate).delete(REDIS_KEY);
        verify(listOperations).rightPushAll(org.mockito.ArgumentMatchers.eq(REDIS_KEY), valuesCaptor.capture());
        verify(redisTemplate).expire(REDIS_KEY, Duration.ofDays(7));

        List<String> values = List.copyOf(valuesCaptor.getValue());
        assertThat(values).hasSize(40);
        assertThat(objectMapper.readTree(values.getFirst()).get("content").asText()).isEqualTo("message-5");
        assertThat(objectMapper.readTree(values.getLast()).get("content").asText()).isEqualTo("message-44");
    }

    @Test
    void shouldRestoreUserAndAssistantMessagesFromPhoneList() {
        when(listOperations.range(REDIS_KEY, -40, -1)).thenReturn(List.of(
                "{\"role\":\"USER\",\"content\":\"第一个问题\"}",
                "{\"role\":\"ASSISTANT\",\"content\":\"第一个回答\"}"
        ));

        List<Message> messages = repository.findByConversationId(PHONE);

        assertThat(messages).hasSize(2);
        assertThat(messages.get(0)).isInstanceOf(UserMessage.class);
        assertThat(messages.get(0).getText()).isEqualTo("第一个问题");
        assertThat(messages.get(1)).isInstanceOf(AssistantMessage.class);
        assertThat(messages.get(1).getText()).isEqualTo("第一个回答");
    }
}
