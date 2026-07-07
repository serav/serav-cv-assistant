package de.serav.cv.assistant.console;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class ConsoleRepository {

    private final JdbcTemplate jdbc;

    public ConsoleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<String> getLabels() {
        return jdbc.queryForList("""
                SELECT DISTINCT at.label
                FROM access_token at
                JOIN conversation_session cs ON cs.token_id = at.id
                ORDER BY at.label
                """, String.class);
    }

    public List<ConversationSummary> getConversations(String labelFilter, boolean includeEmpty) {
        var having = includeEmpty ? "" : "HAVING COUNT(m.type) FILTER (WHERE m.type = 'USER') > 0";
        var sql = """
                SELECT cs.conversation_id, at.label, cs.started_at,
                       COUNT(m.type) FILTER (WHERE m.type = 'USER') AS user_messages,
                       COUNT(m.type) AS total_messages
                FROM conversation_session cs
                JOIN access_token at ON at.id = cs.token_id
                LEFT JOIN spring_ai_chat_memory m ON m.conversation_id = cs.conversation_id::text
                %%s
                GROUP BY cs.conversation_id, at.label, cs.started_at
                %s
                ORDER BY cs.started_at DESC
                """.formatted(having);

        var mapper = (org.springframework.jdbc.core.RowMapper<ConversationSummary>) (rs, row) ->
                new ConversationSummary(
                        (UUID) rs.getObject("conversation_id"),
                        rs.getString("label"),
                        rs.getObject("started_at", OffsetDateTime.class),
                        rs.getLong("user_messages"),
                        rs.getLong("total_messages")
                );

        if (labelFilter == null) {
            return jdbc.query(sql.formatted(""), mapper);
        }
        return jdbc.query(sql.formatted("WHERE at.label = ?"), mapper, labelFilter);
    }

    public List<LabelStats> getLabelStats() {
        return jdbc.query("""
                SELECT label,
                       SUM(CASE WHEN user_count > 0 THEN 1 ELSE 0 END) AS with_messages,
                       SUM(CASE WHEN user_count = 0 THEN 1 ELSE 0 END) AS without_messages
                FROM (
                    SELECT at.label,
                           COUNT(m.type) FILTER (WHERE m.type = 'USER') AS user_count
                    FROM conversation_session cs
                    JOIN access_token at ON at.id = cs.token_id
                    LEFT JOIN spring_ai_chat_memory m ON m.conversation_id = cs.conversation_id::text
                    GROUP BY at.label, cs.conversation_id
                ) sub
                GROUP BY label
                ORDER BY label
                """,
                (rs, row) -> new LabelStats(
                        rs.getString("label"),
                        rs.getLong("with_messages"),
                        rs.getLong("without_messages")
                ));
    }

    public void deleteConversations(List<UUID> ids) {
        if (ids.isEmpty()) return;
        var idStrings = ids.stream().map(UUID::toString).toArray(String[]::new);
        jdbc.update(conn -> {
            var ps = conn.prepareStatement(
                    "DELETE FROM spring_ai_chat_memory WHERE conversation_id = ANY(?)");
            ps.setArray(1, conn.createArrayOf("text", idStrings));
            return ps;
        });
        jdbc.update(conn -> {
            var ps = conn.prepareStatement(
                    "DELETE FROM conversation_session WHERE conversation_id = ANY(?)");
            ps.setArray(1, conn.createArrayOf("uuid", ids.toArray(UUID[]::new)));
            return ps;
        });
    }

    public List<ChatMessage> getMessages(UUID conversationId) {
        return jdbc.query("""
                SELECT type, content, "timestamp"
                FROM spring_ai_chat_memory
                WHERE conversation_id = ?
                ORDER BY "timestamp"
                """,
                (rs, row) -> new ChatMessage(
                        rs.getString("type"),
                        rs.getString("content"),
                        rs.getTimestamp("timestamp").toLocalDateTime()
                ),
                conversationId.toString());
    }
}
