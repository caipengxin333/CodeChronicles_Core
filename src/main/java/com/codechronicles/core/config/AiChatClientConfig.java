package com.codechronicles.core.config;

import com.codechronicles.core.memory.RedisChatMemoryRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 集中配置不同业务场景使用的 AI 客户端。
 * 每个 ChatClient 固化自己的系统 Prompt，业务接口只需要传入用户内容。
 */
@Configuration
public class AiChatClientConfig {

    public static final String GENERAL_CHAT_CLIENT = "generalChatClient";
    public static final String ARTICLE_WRITER_CHAT_CLIENT = "articleWriterChatClient";
    public static final String TAG_EXTRACTOR_CHAT_CLIENT = "tagExtractorChatClient";

    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(RedisChatMemoryRepository.MAX_MESSAGES)
                .build();
    }

    @Bean(GENERAL_CHAT_CLIENT)
    public ChatClient generalChatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder.clone()
                .defaultSystem("""
                        你是 CodeChronicles 技术博客的通用 AI 助手。
                        请使用中文回答，内容准确、简洁；不确定的信息需要明确说明。
                        """)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    @Bean(ARTICLE_WRITER_CHAT_CLIENT)
    public ChatClient articleWriterChatClient(ChatClient.Builder builder) {
        return builder.clone()
                .defaultSystem("""
                        你是专业的中文技术博客编辑。
                        请根据用户提供的主题或草稿生成结构清晰、术语准确的技术文章。
                        文章应包含标题、摘要、正文小节和总结，代码示例使用 Markdown 代码块。
                        不要虚构无法确认的技术事实。
                        """)
                .build();
    }

    @Bean(TAG_EXTRACTOR_CHAT_CLIENT)
    public ChatClient tagExtractorChatClient(ChatClient.Builder builder) {
        return builder.clone()
                .defaultSystem("""
                        你是技术文章标签提取器。
                        请从用户提供的标题、摘要或正文中提取 1 到 5 个核心技术标签。
                        只返回标签名称，使用英文逗号分隔，不要解释，不要编号，不要添加其他内容。
                        """)
                .build();
    }
}
