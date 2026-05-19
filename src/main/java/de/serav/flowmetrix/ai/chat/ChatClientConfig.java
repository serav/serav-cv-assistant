package de.serav.flowmetrix.ai.chat;

import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.core.dialect.JdbcPostgresDialect;

import javax.sql.DataSource;

@Configuration
public class ChatClientConfig {

    @Bean
    JdbcPostgresDialect jdbcPostgresDialect() {
        return JdbcPostgresDialect.INSTANCE;
    }

    @Bean
    PromptChatMemoryAdvisor promptChatMemoryAdvisor(DataSource dataSource) {
        var jdbc = JdbcChatMemoryRepository
                .builder()
                .dataSource(dataSource)
                .build();
        var mwa = MessageWindowChatMemory
                .builder()
                .chatMemoryRepository(jdbc)
                .maxMessages(20)
                .build();
        return PromptChatMemoryAdvisor
                .builder(mwa)
                .build();
    }
}
