package de.serav.flowmetrix.ai.ui;

import com.vaadin.flow.component.html.H1;
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
                .set("padding", "12px 16px")
                .set("background", "#eef2ff")
                .set("border-radius", "0 0 var(--lumo-border-radius-l) var(--lumo-border-radius-l)")
                .set("box-shadow", "0 -4px 16px rgba(99,102,241,0.12)");


        VerticalLayout content = new VerticalLayout(messageScroller, messageInput);
        content.setSizeFull();
        content.setPadding(false);
        content.setSpacing(false);
        content.setFlexGrow(1, messageScroller);
        content.getStyle().set("min-height", "0");

        triggerGreeting(messageList, messageScroller, conversationId);

        messageInput.addSubmitListener(ev -> {
            messageList.add(new MarkdownMessage(ev.getValue(), "You"));
            scrollToBottom(messageScroller);

            MarkdownMessage reply = new MarkdownMessage("⏳ *Thinking...*", "Assistant");
            messageList.add(reply);
            scrollToBottom(messageScroller);

            var firstChunk = new AtomicBoolean(true);

            chatController.chat(ev.getValue(), conversationId).subscribe(cr ->
                getUI().orElseThrow().access(() -> {
                    if (firstChunk.getAndSet(false)) {
                        reply.setMarkdown(cr);
                    } else {
                        reply.appendMarkdown(cr);
                    }
                    scrollToBottom(messageScroller);
                })
            );
        });

        return content;
    }

    private void triggerGreeting(VerticalLayout messageList, Scroller messageScroller, UUID conversationId) {
        MarkdownMessage greeting = new MarkdownMessage("⏳ *Thinking...*", "Assistant");
        messageList.add(greeting);

        var firstChunk = new AtomicBoolean(true);

        chatController.chat("Greet the user briefly and let them know what you can help them with.", conversationId)
                .subscribe(cr -> getUI().orElseThrow().access(() -> {
                    if (firstChunk.getAndSet(false)) {
                        greeting.setMarkdown(cr);
                    } else {
                        greeting.appendMarkdown(cr);
                    }
                    scrollToBottom(messageScroller);
                }));
    }

    private void scrollToBottom(Scroller scroller) {
        scroller.getElement().executeJs("requestAnimationFrame(() => { this.scrollTop = this.scrollHeight; })");
    }
}
