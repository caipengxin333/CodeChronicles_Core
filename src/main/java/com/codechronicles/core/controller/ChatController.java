package com.codechronicles.core.controller;

import com.codechronicles.core.common.ApiResponse;
import com.codechronicles.core.common.CurrentUserContext;
import com.codechronicles.core.config.AiChatClientConfig;
import com.codechronicles.core.dto.ChatHistoryMessageResponse;
import com.codechronicles.core.dto.ChatStreamRequest;
import com.codechronicles.core.util.ThreadLocalUtil;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatController.class);
    private static final int MAX_MESSAGE_LENGTH = 4000;
    private static final MediaType SSE_MEDIA_TYPE =
            new MediaType("text", "event-stream", StandardCharsets.UTF_8);

    private final ChatClient generalChatClient;
    private final ChatClient articleWriterChatClient;
    private final ChatClient tagExtractorChatClient;
    private final ChatMemory chatMemory;

    public ChatController(
            @Qualifier(AiChatClientConfig.GENERAL_CHAT_CLIENT) ChatClient generalChatClient,
            @Qualifier(AiChatClientConfig.ARTICLE_WRITER_CHAT_CLIENT) ChatClient articleWriterChatClient,
            @Qualifier(AiChatClientConfig.TAG_EXTRACTOR_CHAT_CLIENT) ChatClient tagExtractorChatClient,
            ChatMemory chatMemory
    ) {
        this.generalChatClient = generalChatClient;
        this.articleWriterChatClient = articleWriterChatClient;
        this.tagExtractorChatClient = tagExtractorChatClient;
        this.chatMemory = chatMemory;
    }

    /**
     * 通用技术问答，使用 generalChatClient 中预设的系统 Prompt。
     * POST /api/chat/stream
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<ServerSentEvent<String>>> chat(@RequestBody ChatStreamRequest request) {
        CurrentUserContext currentUser = requireCurrentUser();
        String message = requireMessage(request);

        Flux<ServerSentEvent<String>> response = generalChatClient.prompt()
                .user(message)
                .advisors(advisor -> advisor.param(
                        ChatMemory.CONVERSATION_ID,
                        currentUser.phone()
                ))
                .stream()
                .content()
                .map(content -> ServerSentEvent.builder(content)
                        .event("message")
                        .build())
                .concatWithValues(ServerSentEvent.builder("[DONE]")
                        .event("done")
                        .build())
                .onErrorResume(exception -> {
                    LOGGER.error("AI 流式问答失败，phone={}", currentUser.phone(), exception);
                    return Flux.just(ServerSentEvent.builder("AI服务暂时不可用，请稍后重试")
                            .event("error")
                            .build());
                });

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .header("X-Accel-Buffering", "no")
                .contentType(SSE_MEDIA_TYPE)
                .body(response);
    }

    /**
     * 获取当前登录用户的聊天历史，手机号从登录上下文中获取。
     * GET /api/chat/history
     */
    @GetMapping("/history")
    public ApiResponse<List<ChatHistoryMessageResponse>> getChatHistory() {
        CurrentUserContext currentUser = requireCurrentUser();
        List<ChatHistoryMessageResponse> history = chatMemory.get(currentUser.phone()).stream()
                .map(message -> new ChatHistoryMessageResponse(
                        message.getMessageType().name(),
                        message.getText()
                ))
                .toList();
        return ApiResponse.success(history);
    }

    /**
     * 技术文章生成，使用文章编辑 Prompt。
     * GET /api/chat/article?message=写一篇Spring Boot事务文章
     */
    @GetMapping("/article")
    public String writeArticle(@RequestParam String message) {
        return articleWriterChatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    /**
     * 技术标签提取，返回英文逗号分隔的标签名称。
     * GET /api/chat/tags?message=Spring Boot整合Redis实现接口限流
     */
    @GetMapping("/tags")
    public String extractTags(@RequestParam String message) {
        return tagExtractorChatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    private CurrentUserContext requireCurrentUser() {
        CurrentUserContext currentUser = ThreadLocalUtil.get();
        if (currentUser == null || currentUser.phone() == null || currentUser.phone().isBlank()) {
            throw new IllegalArgumentException("无法获取当前登录用户手机号");
        }
        return currentUser;
    }

    private String requireMessage(ChatStreamRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            throw new IllegalArgumentException("问题内容不能为空");
        }
        String message = request.message().trim();
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("问题内容不能超过 4000 个字符");
        }
        return message;
    }
}
