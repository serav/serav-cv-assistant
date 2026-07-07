package de.serav.cv.assistant.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.serav.cv.assistant.console.ConsoleRepository;
import de.serav.cv.assistant.console.ChatMessage;
import de.serav.cv.assistant.console.ConversationSummary;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

@Route("console")
@PageTitle("Console — CV Assistant")
@RolesAllowed("ADMIN")
public class ConsoleView extends Div {

    private static final String C_ACCENT      = "#6366F1";
    private static final String C_USER_BUBBLE = "#4338CA";
    private static final String C_BORDER      = "#E0E7FF";
    private static final String C_TEXT_DARK   = "#1E293B";
    private static final String C_TEXT_MED    = "#475569";
    private static final String C_TEXT_LIGHT  = "#94A3B8";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String ALL = "All labels";

    private final ConsoleRepository repo;
    private Grid<ConversationSummary> grid;
    private Div messagePanel;

    public ConsoleView(ConsoleRepository repo) {
        this.repo = repo;

        getStyle()
                .set("display", "flex").set("flex-direction", "column")
                .set("height", "100vh").set("height", "100dvh")
                .set("font-family", "'Inter', 'Segoe UI', sans-serif")
                .set("background", "#F1F5F9").set("overflow", "hidden");

        add(buildHeader(), buildBody());
    }

    // ── Header ──────────────────────────────────────────────────────────────

    private Div buildHeader() {
        var title = new Span("Conversation History");
        title.getStyle().set("color", "#fff").set("font-weight", "700").set("font-size", "1.05rem");

        var backBtn = new Button("← Back to Chat");
        backBtn.getStyle()
                .set("background", "transparent")
                .set("color", "rgba(255,255,255,0.65)")
                .set("border", "1px solid rgba(255,255,255,0.25)")
                .set("border-radius", "6px").set("font-size", "0.75rem")
                .set("cursor", "pointer").set("padding", "4px 12px");
        backBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(ChatView.class)));

        var spacer = new Div();
        spacer.getStyle().set("flex", "1");

        var row = new Div(title, spacer, backBtn);
        row.getStyle()
                .set("display", "flex").set("align-items", "center").set("gap", "14px")
                .set("width", "100%");

        var header = new Div(row);
        header.getStyle()
                .set("background", "linear-gradient(135deg, #0F172A 0%, #1E3A8A 100%)")
                .set("padding", "14px 24px").set("width", "100%")
                .set("box-sizing", "border-box").set("flex-shrink", "0");
        return header;
    }

    // ── Body ────────────────────────────────────────────────────────────────

    private Div buildBody() {
        var body = new Div(buildLeftPanel(), buildRightPanel());
        body.getStyle()
                .set("display", "flex").set("flex", "1 1 0").set("min-height", "0");
        return body;
    }

    // ── Left: filter + grid ─────────────────────────────────────────────────

    private Div buildLeftPanel() {
        // Label filter
        var labels = repo.getLabels();
        var options = Stream.concat(Stream.of(ALL), labels.stream()).toList();

        var select = new Select<String>();
        select.setItems(options);
        select.setValue(ALL);
        select.setWidth("220px");

        var filterLabel = new Span("Label:");
        filterLabel.getStyle().set("font-size", "0.82rem").set("font-weight", "600").set("color", C_TEXT_MED);

        var filterRow = new Div(filterLabel, select);
        filterRow.getStyle()
                .set("display", "flex").set("align-items", "center").set("gap", "10px")
                .set("padding", "12px 16px").set("background", "white")
                .set("border-bottom", "1px solid #E2E8F0").set("flex-shrink", "0");

        // Grid
        grid = new Grid<>(ConversationSummary.class, false);
        grid.addColumn(ConversationSummary::label)
                .setHeader("Label").setFlexGrow(1).setSortable(true);
        grid.addColumn(s -> s.startedAt() != null ? s.startedAt().format(DATE_FMT) : "—")
                .setHeader("Started").setFlexGrow(1).setSortable(true);
        grid.addColumn(ConversationSummary::userMessages)
                .setHeader("User msgs").setWidth("110px").setFlexGrow(0).setSortable(true);
        grid.addColumn(ConversationSummary::totalMessages)
                .setHeader("Total msgs").setWidth("110px").setFlexGrow(0).setSortable(true);

        grid.getStyle().set("flex", "1 1 0").set("min-height", "0");
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.addSelectionListener(e -> e.getFirstSelectedItem().ifPresent(this::loadMessages));

        loadGrid(null);
        select.addValueChangeListener(e -> loadGrid(ALL.equals(e.getValue()) ? null : e.getValue()));

        var left = new Div(filterRow, grid);
        left.getStyle()
                .set("display", "flex").set("flex-direction", "column")
                .set("width", "58%").set("border-right", "1px solid #E2E8F0")
                .set("overflow", "hidden").set("background", "white");
        return left;
    }

    // ── Right: message viewer ───────────────────────────────────────────────

    private Div buildRightPanel() {
        var panelTitle = new Span("Messages");
        panelTitle.getStyle()
                .set("font-weight", "700").set("font-size", "0.9rem").set("color", C_TEXT_DARK);

        var titleBar = new Div(panelTitle);
        titleBar.getStyle()
                .set("padding", "12px 16px").set("background", "white")
                .set("border-bottom", "1px solid #E2E8F0").set("flex-shrink", "0");

        messagePanel = new Div();
        messagePanel.getStyle()
                .set("flex", "1 1 0").set("min-height", "0").set("overflow-y", "auto")
                .set("display", "flex").set("flex-direction", "column")
                .set("gap", "10px").set("padding", "16px").set("background", "#F1F5F9");

        showEmpty();

        var right = new Div(titleBar, messagePanel);
        right.getStyle()
                .set("display", "flex").set("flex-direction", "column")
                .set("flex", "1 1 0").set("min-width", "0").set("overflow", "hidden");
        return right;
    }

    // ── Data helpers ─────────────────────────────────────────────────────────

    private void loadGrid(String labelFilter) {
        grid.setItems(repo.getConversations(labelFilter));
    }

    private void loadMessages(ConversationSummary conv) {
        messagePanel.removeAll();
        var messages = repo.getMessages(conv.conversationId());
        if (messages.isEmpty()) {
            showEmpty();
            return;
        }
        for (var msg : messages) {
            messagePanel.add(buildMessageRow(msg));
        }
        // Scroll to bottom
        messagePanel.getElement().executeJs("this.scrollTop = this.scrollHeight;");
    }

    private void showEmpty() {
        var hint = new Span("Select a conversation to view its messages.");
        hint.getStyle().set("color", C_TEXT_LIGHT).set("font-size", "0.85rem")
                .set("text-align", "center").set("margin", "auto");
        messagePanel.add(hint);
    }

    // ── Message bubble ───────────────────────────────────────────────────────

    private Div buildMessageRow(ChatMessage msg) {
        var isUser = "USER".equals(msg.type());
        var isSystem = "SYSTEM".equals(msg.type()) || "TOOL".equals(msg.type());

        if (isSystem) {
            var note = new Span("[" + msg.type() + "] " + msg.content());
            note.getStyle()
                    .set("font-size", "0.75rem").set("color", C_TEXT_LIGHT)
                    .set("text-align", "center").set("display", "block");
            var row = new Div(note);
            row.getStyle().set("display", "flex").set("justify-content", "center");
            return row;
        }

        var time = new Span(msg.timestamp().format(DateTimeFormatter.ofPattern("HH:mm")));
        time.getStyle().set("font-size", "0.68rem").set("color", C_TEXT_LIGHT)
                .set("margin-top", "3px").set("flex-shrink", "0").set("align-self", "flex-end");

        var bubble = new Div();
        bubble.getElement().setProperty("innerHTML", esc(msg.content()));
        bubble.getStyle()
                .set("padding", "9px 14px").set("font-size", "0.85rem").set("line-height", "1.55")
                .set("max-width", "78%").set("word-break", "break-word");

        if (isUser) {
            bubble.getStyle()
                    .set("background", C_USER_BUBBLE).set("color", "#fff")
                    .set("border-radius", "18px 18px 4px 18px")
                    .set("box-shadow", "0 2px 6px rgba(67,56,202,0.25)");
            var row = new Div(time, bubble);
            row.getStyle().set("display", "flex").set("justify-content", "flex-end")
                    .set("align-items", "flex-end").set("gap", "6px");
            return row;
        }

        // Assistant
        var avatar = new Div();
        avatar.setText("✦");
        avatar.getStyle()
                .set("background", "linear-gradient(135deg, " + C_ACCENT + " 0%, #7C3AED 100%)")
                .set("color", "#fff").set("font-weight", "700").set("font-size", "0.85rem")
                .set("width", "26px").set("height", "26px").set("border-radius", "50%")
                .set("display", "flex").set("align-items", "center").set("justify-content", "center")
                .set("flex-shrink", "0").set("align-self", "flex-end");

        bubble.getStyle()
                .set("background", "white").set("color", C_TEXT_DARK)
                .set("border-radius", "4px 18px 18px 18px")
                .set("border", "1px solid " + C_BORDER)
                .set("border-left", "3px solid " + C_ACCENT)
                .set("box-shadow", "0 2px 6px rgba(0,0,0,0.05)");

        var row = new Div(avatar, bubble, time);
        row.getStyle().set("display", "flex").set("align-items", "flex-end").set("gap", "6px");
        return row;
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
