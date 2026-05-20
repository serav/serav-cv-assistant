package de.serav.flowmetrix.ai.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import de.serav.flowmetrix.ai.chat.ChatService;
import org.vaadin.firitin.components.messagelist.MarkdownMessage;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Route("")
public class ChatView extends VerticalLayout implements RouterLayout {

    private static final int MAX_MESSAGE_LENGTH = 2000;

    private final ChatService chatController;

    ChatView(ChatService chatController) {
        this.chatController = chatController;
        setSizeFull();
        setMargin(false);
        setPadding(false);
        setSpacing(false);
        getStyle()
            .set("height", "100vh")
            .set("max-height", "100vh")
            .set("overflow", "hidden");

        initChatView();
    }

    private void initChatView() {
        removeAll();
        H1 header = new H1("⚡ FlowMetrix AI Assistent");
        header.getStyle()
                .set("margin", "0")
                .set("padding", "var(--lumo-space-m) var(--lumo-space-l)")
                .set("font-size", "1.8rem")
                .set("font-weight", "700")
                .set("text-align", "center")
                .set("width", "100%")
                .set("letter-spacing", "0.04em")
                .set("color", "#6366f1")
                .set("border-bottom", "2px solid var(--lumo-contrast-10pct)");
        add(header);
        var content = createChatContent();
        add(content);
        setFlexGrow(1, content);
    }

    private VerticalLayout createChatContent() {
        var conversationId = UUID.randomUUID();

        VerticalLayout messageList = new VerticalLayout();
        messageList.setWidthFull();
        messageList.setPadding(true);

        Scroller messageScroller = new Scroller(messageList);
        messageScroller.setSizeFull();
        messageScroller.getStyle()
                .set("min-height", "0")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("background", "var(--lumo-base-color)");

        MessageInput messageInput = new MessageInput();
        messageInput.setWidthFull();
        messageInput.getStyle()
                .set("border-top", "2px solid #6366f1")
                .set("padding", "12px 16px 4px 16px")
                .set("background", "#eef2ff")
                .set("border-radius", "0 0 var(--lumo-border-radius-l) var(--lumo-border-radius-l)")
                .set("box-shadow", "0 -4px 16px rgba(99,102,241,0.12)");
        messageInput.setTooltipText("Type your message and press Enter to send. Max " + MAX_MESSAGE_LENGTH + " characters.");

        Span charCounter = new Span("Max " + MAX_MESSAGE_LENGTH + " characters");
        charCounter.getStyle()
                .set("font-size", "0.75rem")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("padding", "0 4px 8px 4px")
                .set("display", "block")
                .set("text-align", "right");

        messageInput.getElement().executeJs("""
                const maxLen = $0;
                const style = document.createElement('style');
                style.textContent = `
                  :host { background: #eef2ff; }
                  [part='input-field'] {
                    font-size: 1rem !important;
                    min-height: 3rem !important;
                    border-radius: 12px !important;
                    border: 2px solid #6366f1 !important;
                    background: #ffffff !important;
                    padding: 8px 16px !important;
                  }
                  [part='input-field'] textarea { background: #ffffff !important; }
                  [part='input-field']:focus-within {
                    border-color: #818cf8 !important;
                    box-shadow: 0 0 0 3px rgba(99,102,241,0.2) !important;
                  }
                  [part='send-button'] {
                    background: #6366f1 !important;
                    color: white !important;
                    border-radius: 10px !important;
                    width: 3rem !important;
                    height: 3rem !important;
                  }
                  [part='send-button']:hover { background: #818cf8 !important; }
                `;
                this.shadowRoot.appendChild(style);
                const textarea = this.shadowRoot.querySelector('textarea');
                """, MAX_MESSAGE_LENGTH);


        VerticalLayout inputWrapper = new VerticalLayout(messageInput, charCounter);
        inputWrapper.setPadding(false);
        inputWrapper.setSpacing(false);
        inputWrapper.setWidthFull();
        inputWrapper.getStyle()
                .set("background", "#eef2ff")
                .set("border-top", "2px solid #6366f1")
                .set("box-shadow", "0 -4px 16px rgba(99,102,241,0.12)")
                .set("border-radius", "0 0 var(--lumo-border-radius-l) var(--lumo-border-radius-l)");
        messageInput.getStyle().remove("border-top").remove("box-shadow").remove("border-radius");

        VerticalLayout content = new VerticalLayout(messageScroller, inputWrapper);
        content.setSizeFull();
        content.setPadding(false);
        content.setSpacing(false);
        content.setFlexGrow(1, messageScroller);
        content.getStyle().set("min-height", "0");

        triggerGreeting(messageList, messageScroller, conversationId);

        messageInput.addSubmitListener(ev -> {
            String text = ev.getValue();
            if (text == null || text.isBlank()) return;
            if (text.length() > MAX_MESSAGE_LENGTH) {
                MarkdownMessage warning = new MarkdownMessage(
                        "⚠️ Your message exceeds the maximum length of " + MAX_MESSAGE_LENGTH + " characters. Please shorten it.", "Assistant");
                styleErrorMessage(warning);
                messageList.add(warning);
                scrollToBottom(messageScroller);
                return;
            }
            messageList.add(new MarkdownMessage(ev.getValue(), "You"));
            scrollToBottom(messageScroller);

            MarkdownMessage reply = new MarkdownMessage("⏳ *Thinking...*", "Assistant");
            messageList.add(reply);
            scrollToBottom(messageScroller);

            messageInput.setEnabled(false);
            var firstChunk = new AtomicBoolean(true);

            chatController.chat(ev.getValue(), conversationId).subscribe(
                cr -> getUI().orElseThrow().access(() -> {
                    if (firstChunk.getAndSet(false)) {
                        reply.setMarkdown(cr);
                    } else {
                        reply.appendMarkdown(cr);
                    }
                    scrollToBottom(messageScroller);
                }),
                err -> getUI().orElseThrow().access(() -> {
                    reply.setMarkdown("⚠️ **Error:** " + err.getMessage());
                    styleErrorMessage(reply);
                    messageInput.setEnabled(true);
                    scrollToBottom(messageScroller);
                }),
                () -> getUI().orElseThrow().access(() -> messageInput.setEnabled(true))
            );
        });

        return content;
    }

    private void triggerGreeting(VerticalLayout messageList, Scroller messageScroller, UUID conversationId) {
        MarkdownMessage greeting = new MarkdownMessage("⏳ *Thinking...*", "Assistant");
        messageList.add(greeting);

        var firstChunk = new AtomicBoolean(true);

        chatController.chat("Introduce yourself shortly, show the tools core features. Then ask the user for his name", conversationId)
                .subscribe(
                    cr -> getUI().orElseThrow().access(() -> {
                        if (firstChunk.getAndSet(false)) {
                            greeting.setMarkdown(cr);
                        } else {
                            greeting.appendMarkdown(cr);
                        }
                        scrollToBottom(messageScroller);
                    }),
                    err -> getUI().orElseThrow().access(() -> {
                        greeting.setMarkdown("⚠️ **Could not connect to AI service.**");
                        styleErrorMessage(greeting);
                        scrollToBottom(messageScroller);
                    }),
                    () -> {}
                );
    }

    private void styleErrorMessage(MarkdownMessage message) {
        message.getStyle()
                .set("background", "#fff1f2")
                .set("border", "1px solid #fecdd3")
                .set("border-radius", "8px")
                .set("padding", "8px 12px")
                .set("color", "#be123c");
    }

    private void scrollToBottom(Scroller scroller) {
        scroller.getElement().executeJs("requestAnimationFrame(() => { this.scrollTop = this.scrollHeight; })");
    }
}
