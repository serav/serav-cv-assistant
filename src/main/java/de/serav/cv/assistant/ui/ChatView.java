package de.serav.cv.assistant.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.serav.cv.assistant.auth.AuthenticatedToken;
import de.serav.cv.assistant.auth.ConversationSessionRepository;
import de.serav.cv.assistant.chat.ChatService;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Route("")
@PageTitle("Sergiu Avram — CV Assistant")
@PermitAll
public class ChatView extends Div {

    private static final int MAX_LEN = 2000;

    // Palette
    private static final String C_BG          = "#F1F5F9";
    private static final String C_ACCENT      = "#6366F1";
    private static final String C_USER_BUBBLE = "#4338CA";
    private static final String C_ASST_BUBBLE = "#FFFFFF";
    private static final String C_BORDER      = "#E0E7FF";
    private static final String C_TEXT_DARK   = "#1E293B";
    private static final String C_TEXT_MED    = "#475569";
    private static final String C_TEXT_LIGHT  = "#94A3B8";

    // Identity
    private static final String NAME  = "Sergiu Avram";
    private static final String TITLE = "Cloud-native Software Engineer · Java & Spring Boot";

    private final ChatService chatService;
    private final AuthenticationContext authContext;
    private final UUID conversationId;
    private final Locale locale;
    private final UiStrings strings;
    private final AtomicBoolean streaming = new AtomicBoolean(false);
    private final List<Div> chips = new ArrayList<>();

    private Div messageList;
    private TextField inputField;
    private Button sendButton;

    public ChatView(ChatService chatService, AuthenticationContext authContext,
                    ConversationSessionRepository conversationSessionRepository) {
        this.chatService = chatService;
        this.authContext = authContext;
        var principal = (AuthenticatedToken) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var session = VaadinSession.getCurrent();

        var manualLocale = (Locale) session.getAttribute("selectedLocale");
        var detected = session.getLocale();
        this.locale = manualLocale != null ? manualLocale
                : (detected != null && detected.getLanguage().equals("de") ? Locale.GERMAN : Locale.ENGLISH);
        this.strings = UiStrings.forLocale(this.locale);

        var storedId = (UUID) session.getAttribute("conversationId");
        if (storedId == null) {
            storedId = UUID.randomUUID();
            session.setAttribute("conversationId", storedId);
            conversationSessionRepository.registerIfAbsent(storedId, principal.tokenId());
        }
        this.conversationId = storedId;
        getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("width", "100%")
                .set("height", "100vh")
                .set("margin", "0")
                .set("padding", "0")
                .set("box-sizing", "border-box")
                .set("overflow", "hidden");

        injectStyles();

        var header = buildHeader();
        var body   = buildBody();
        add(header, body);
        body.getStyle().set("flex", "1 1 0").set("min-height", "0").set("width", "100%");
    }

    @Override
    protected void onAttach(AttachEvent e) {
        super.onAttach(e);
        setInputEnabled(false);
        streaming.set(true);
        var bubble = addAssistantBubble();
        var sb = new StringBuilder();
        chatService.chat(strings.greetingPrompt(), conversationId, locale).subscribe(
                chunk -> getUI().orElseThrow().access(() -> {
                    sb.append(chunk);
                    renderBubble(bubble, sb.toString());
                    scrollToBottom();
                }),
                err -> getUI().orElseThrow().access(() -> {
                    renderBubble(bubble, "Hello! I'm Sergiu's CV assistant. Feel free to ask me anything about his background and experience.");
                    enableInput();
                }),
                () -> getUI().orElseThrow().access(this::enableInput)
        );
    }

    // ── Header ──────────────────────────────────────────────────────────────

    private Div buildHeader() {

        var nameSpan = new Span(NAME + "'s CV Assistant");
        nameSpan.getStyle().set("color", "#fff").set("font-weight", "700").set("font-size", "1.05rem");

        var sub = new Span(TITLE);
        sub.getStyle().set("color", "rgba(255,255,255,0.6)").set("font-size", "0.77rem");

        var textBlock = new Div(nameSpan, sub);
        textBlock.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "2px");

        var badge = new Span("✦ AI-powered");
        badge.getStyle()
                .set("background", "rgba(99,102,241,0.2)")
                .set("color", "#A5B4FC").set("font-size", "0.7rem").set("font-weight", "600")
                .set("padding", "3px 11px").set("border-radius", "20px")
                .set("border", "1px solid rgba(99,102,241,0.4)")
                .set("white-space", "nowrap").set("align-self", "center");

        var langToggle = buildLangToggle();

        var logoutBtn = new Button(strings.signOut());
        logoutBtn.getStyle()
                .set("background", "transparent")
                .set("color", "rgba(255,255,255,0.65)")
                .set("border", "1px solid rgba(255,255,255,0.25)")
                .set("border-radius", "6px")
                .set("font-size", "0.75rem")
                .set("cursor", "pointer")
                .set("padding", "4px 12px");
        logoutBtn.addClickListener(e -> authContext.logout());

        var spacer = new Div();
        spacer.getStyle().set("flex", "1");

        var row = new Div(textBlock, badge, spacer, langToggle, logoutBtn);
        row.getStyle()
                .set("display", "flex").set("align-items", "center").set("gap", "14px")
                .set("width", "100%");

        var header = new Div(row);
        header.getStyle()
                .set("background", "linear-gradient(135deg, #0F172A 0%, #1E3A8A 100%)")
                .set("padding", "14px 24px")
                .set("width", "100%")
                .set("box-sizing", "border-box")
                .set("flex-shrink", "0");
        return header;
    }

    private Div buildLangToggle() {
        var wrap = new Div();
        wrap.getStyle()
                .set("display", "flex").set("gap", "4px").set("align-items", "center");

        for (var lang : new Locale[]{Locale.ENGLISH, Locale.GERMAN}) {
            var isActive = lang.getLanguage().equals(locale.getLanguage());
            var btn = new Button(lang.getLanguage().toUpperCase());
            btn.getStyle()
                    .set("background", isActive ? "rgba(255,255,255,0.18)" : "transparent")
                    .set("color", isActive ? "#fff" : "rgba(255,255,255,0.5)")
                    .set("border", "1px solid " + (isActive ? "rgba(255,255,255,0.45)" : "rgba(255,255,255,0.2)"))
                    .set("border-radius", "5px").set("font-size", "0.7rem").set("font-weight", "600")
                    .set("cursor", "pointer").set("padding", "3px 9px").set("min-width", "0");
            if (!isActive) {
                btn.addClickListener(e -> {
                    VaadinSession.getCurrent().setAttribute("selectedLocale", lang);
                    getUI().ifPresent(ui -> ui.navigate(ChatView.class));
                });
            }
            wrap.add(btn);
        }
        return wrap;
    }

    // ── Body ────────────────────────────────────────────────────────────────

    private Div buildBody() {
        var body = new Div(buildSidebar(), buildChatPanel());
        body.getStyle().set("display", "flex");
        return body;
    }

    // ── Sidebar ─────────────────────────────────────────────────────────────

    private Div buildSidebar() {
        var photo = new Image("/sergiu.png", NAME);
        photo.getStyle()
                .set("width", "88px").set("height", "88px")
                .set("border-radius", "50%")
                .set("object-fit", "cover")
                .set("display", "block")
                .set("margin", "0 auto 12px")
                .set("box-shadow", "0 4px 16px rgba(99,102,241,0.35)")
                .set("border", "3px solid " + C_BORDER);

        var sName = new Span(NAME);
        sName.getStyle()
                .set("font-weight", "700").set("font-size", "1rem").set("color", C_TEXT_DARK)
                .set("display", "block").set("text-align", "center");

        var sRole = new Span("Software Engineer");
        sRole.getStyle()
                .set("font-size", "0.76rem").set("color", C_TEXT_MED)
                .set("display", "block").set("text-align", "center").set("margin-top", "2px");

        var profileCard = new Div(photo, sName, sRole);
        profileCard.getStyle()
                .set("background", "linear-gradient(180deg, #EEF2FF 0%, #FFFFFF 100%)")
                .set("border-radius", "12px").set("padding", "20px 16px")
                .set("border", "1px solid " + C_BORDER).set("margin-bottom", "18px");

        var stats = new Div(
                stat("📍", "Fürth, Bavaria, Germany"),
                stat("🔬", "10+ years of experience"),
                stat("🎓", "CS Diploma (equiv. Master)"),
                stat("🌍", "DE · EN · ES · RO"),
                statLink("👨🏻‍💻", "GitHub", "https://github.com/serav")


        );
        stats.getStyle()
                .set("display", "flex").set("flex-direction", "column")
                .set("gap", "8px").set("margin-bottom", "20px");

        var divider = new Div();
        divider.getStyle().set("height", "1px").set("background", "#E2E8F0").set("margin-bottom", "14px");

        var chipsLabel = new Span(strings.quickQuestionsLabel());
        chipsLabel.getStyle()
                .set("font-size", "0.7rem").set("font-weight", "700").set("color", C_TEXT_LIGHT)
                .set("text-transform", "uppercase").set("letter-spacing", "0.07em")
                .set("display", "block").set("margin-bottom", "8px");

        var chipsDiv = new Div();
        chipsDiv.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "6px");
        for (var q : strings.quickQuestions()) {
            var chip = new Div();
            chip.setText(q);
            chip.addClassName("cv-chip");
            chip.getStyle()
                    .set("background", "#F8FAFC").set("color", C_TEXT_DARK)
                    .set("font-size", "0.79rem").set("border", "1px solid #E2E8F0")
                    .set("border-radius", "8px").set("padding", "8px 12px")
                    .set("cursor", "pointer").set("line-height", "1.4")
                    .set("transition", "all 0.15s");
            chip.getElement().addEventListener("click", e -> {
                if (!streaming.get()) submit(q);
            });
            chipsDiv.add(chip);
            chips.add(chip);
        }

        var sidebar = new Div(profileCard, stats, divider, chipsLabel, chipsDiv);
        sidebar.addClassName("cv-sidebar");
        sidebar.getStyle()
                .set("width", "270px").set("flex-shrink", "0")
                .set("background", "#FFFFFF").set("padding", "20px 14px")
                .set("overflow-y", "auto").set("border-right", "1px solid #E2E8F0")
                .set("box-sizing", "border-box");
        return sidebar;
    }

    private Div stat(String icon, String text) {
        var ic = new Span(icon);
        ic.getStyle().set("font-size", "0.85rem").set("flex-shrink", "0").set("margin-top", "1px");
        var tx = new Span(text);
        tx.getStyle().set("font-size", "0.8rem").set("color", C_TEXT_MED).set("line-height", "1.4");
        var row = new Div(ic, tx);
        row.getStyle().set("display", "flex").set("gap", "8px").set("align-items", "flex-start");
        return row;
    }

    private Div statLink(String icon, String label, String href) {
        var ic = new Span(icon);
        ic.getStyle().set("font-size", "0.85rem").set("flex-shrink", "0").set("margin-top", "1px");
        var link = new Anchor(href, label);
        link.setTarget("_blank");
        link.getStyle()
                .set("font-size", "0.8rem").set("color", C_ACCENT)
                .set("text-decoration", "none").set("line-height", "1.4");
        var row = new Div(ic, link);
        row.getStyle().set("display", "flex").set("gap", "8px").set("align-items", "flex-start");
        return row;
    }

    // ── Chat panel ──────────────────────────────────────────────────────────

    private Div buildChatPanel() {
        messageList = new Div();
        messageList.getStyle()
                .set("display", "flex").set("flex-direction", "column")
                .set("gap", "14px").set("padding", "20px 24px")
                .set("width", "100%").set("box-sizing", "border-box");

        var scroll = new Div(messageList);
        scroll.addClassName("cv-scroll");
        scroll.getStyle()
                .set("flex", "1 1 0").set("min-height", "0")
                .set("width", "100%")
                .set("overflow-y", "auto").set("background", C_BG);

        var panel = new Div(scroll, buildInputBar());
        panel.getStyle()
                .set("display", "flex").set("flex-direction", "column")
                .set("flex", "1 1 0").set("min-height", "0").set("min-width", "0");
        return panel;
    }

    private Div buildInputBar() {
        inputField = new TextField();
        inputField.setPlaceholder(strings.inputPlaceholder());
        inputField.addClassName("cv-input");
        inputField.setWidthFull();
        inputField.addKeyPressListener(Key.ENTER, e -> submit(inputField.getValue()));

        var inputWrap = new Div(inputField);
        inputWrap.getStyle().set("flex", "1").set("min-width", "0");

        sendButton = new Button(new Icon(VaadinIcon.PAPERPLANE));
        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendButton.addClassName("cv-send-btn");
        sendButton.addClickListener(e -> submit(inputField.getValue()));

        var bar = new Div(inputWrap, sendButton);
        bar.getStyle()
                .set("display", "flex").set("align-items", "center").set("gap", "10px")
                .set("padding", "14px 20px").set("background", "#FFFFFF")
                .set("border-top", "1px solid #E2E8F0")
                .set("box-shadow", "0 -4px 12px rgba(0,0,0,0.05)")
                .set("width", "100%").set("box-sizing", "border-box")
                .set("flex-shrink", "0");
        return bar;
    }

    // ── Send / streaming ────────────────────────────────────────────────────

    private void submit(String text) {
        if (text == null || text.isBlank() || streaming.get()) return;
        if (text.length() > MAX_LEN) {
            addSystemNote(strings.messageTooLong().formatted(MAX_LEN));
            return;
        }
        inputField.clear();
        streaming.set(true);
        setInputEnabled(false);

        addUserBubble(text);
        var bubble = addAssistantBubble();
        var sb = new StringBuilder();

        chatService.chat(text, conversationId, locale).subscribe(
                chunk -> getUI().orElseThrow().access(() -> {
                    sb.append(chunk);
                    renderBubble(bubble, sb.toString());
                    scrollToBottom();
                }),
                err -> getUI().orElseThrow().access(() -> {
                    renderBubble(bubble, "Something went wrong: " + err.getMessage());
                    enableInput();
                }),
                () -> getUI().orElseThrow().access(this::enableInput)
        );
    }

    // ── Bubble builders ─────────────────────────────────────────────────────

    private void addUserBubble(String text) {
        var bubble = new Div();
        bubble.getElement().setProperty("innerHTML", esc(text));
        bubble.getStyle()
                .set("background", C_USER_BUBBLE).set("color", "#FFFFFF")
                .set("padding", "10px 16px")
                .set("border-radius", "18px 18px 4px 18px")
                .set("max-width", "70%").set("font-size", "0.9rem").set("line-height", "1.5")
                .set("box-shadow", "0 2px 8px rgba(67,56,202,0.3)");

        var row = new Div(bubble);
        row.addClassName("msg-row");
        row.getStyle().set("display", "flex").set("justify-content", "flex-end");
        messageList.add(row);
        scrollToBottom();
    }

    private Div addAssistantBubble() {
        var bubble = new Div();
        bubble.getElement().setProperty("innerHTML", typingDots());
        bubble.getStyle()
                .set("background", C_ASST_BUBBLE).set("color", C_TEXT_DARK)
                .set("padding", "12px 16px")
                .set("border-radius", "4px 18px 18px 18px")
                .set("max-width", "80%").set("font-size", "0.9rem").set("line-height", "1.65")
                .set("border", "1px solid " + C_BORDER)
                .set("border-left", "3px solid " + C_ACCENT)
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.06)");

        var av = avatar("SA", "0.65rem", "28px", "28px");
        av.getStyle().set("margin-bottom", "2px").set("flex-shrink", "0").set("align-self", "flex-end");

        var row = new Div(av, bubble);
        row.addClassName("msg-row");
        row.getStyle()
                .set("display", "flex").set("align-items", "flex-end").set("gap", "8px");
        messageList.add(row);
        scrollToBottom();
        return bubble;
    }

    private void renderBubble(Div bubble, String text) {
        bubble.getElement().setProperty("innerHTML", mdToHtml(text));
    }

    private void addSystemNote(String text) {
        var msg = new Span(text);
        msg.getStyle()
                .set("display", "block").set("text-align", "center")
                .set("color", "#DC2626").set("font-size", "0.8rem").set("padding", "4px");
        messageList.add(msg);
    }

    // ── Markdown → HTML ─────────────────────────────────────────────────────

    private String mdToHtml(String text) {
        var sb = new StringBuilder();
        var lines = text.split("\\n", -1);
        boolean inList = false;
        for (var raw : lines) {
            var line = raw.trim();
            if (line.startsWith("- ") || line.startsWith("* ")) {
                if (!inList) { sb.append("<ul style=\"margin:4px 0;padding-left:18px\">"); inList = true; }
                sb.append("<li style=\"margin-bottom:3px\">").append(inline(line.substring(2))).append("</li>");
            } else {
                if (inList) { sb.append("</ul>"); inList = false; }
                if (line.isEmpty()) {
                    sb.append("<div style=\"height:5px\"></div>");
                } else if (line.startsWith("### ")) {
                    sb.append("<h4 style=\"margin:6px 0 3px;font-size:0.9rem\">").append(esc(line.substring(4))).append("</h4>");
                } else if (line.startsWith("## ")) {
                    sb.append("<h3 style=\"margin:6px 0 3px;font-size:0.95rem\">").append(esc(line.substring(3))).append("</h3>");
                } else if (line.startsWith("# ")) {
                    sb.append("<h2 style=\"margin:6px 0 4px;font-size:1rem\">").append(esc(line.substring(2))).append("</h2>");
                } else {
                    sb.append("<p style=\"margin:2px 0\">").append(inline(line)).append("</p>");
                }
            }
        }
        if (inList) sb.append("</ul>");
        return sb.toString();
    }

    private String inline(String text) {
        text = esc(text);
        text = text.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
        text = text.replaceAll("\\*(.+?)\\*",        "<em>$1</em>");
        text = text.replaceAll("_(.+?)_",             "<em>$1</em>");
        text = text.replaceAll("`(.+?)`",
                "<code style=\"background:#F1F5F9;padding:1px 5px;border-radius:3px;font-size:0.85em;font-family:monospace\">$1</code>");
        return text;
    }

    private String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String typingDots() {
        return "<span style=\"color:" + C_ACCENT + ";letter-spacing:4px\">" +
               "<span style=\"animation:blink 1.2s infinite 0s\">●</span>" +
               "<span style=\"animation:blink 1.2s infinite 0.4s\">●</span>" +
               "<span style=\"animation:blink 1.2s infinite 0.8s\">●</span></span>";
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private Div avatar(String initials, String fontSize, String width, String height) {
        var av = new Div();
        av.setText(initials);
        av.getStyle()
                .set("background", "linear-gradient(135deg, " + C_ACCENT + " 0%, #7C3AED 100%)")
                .set("color", "#fff").set("font-weight", "700").set("font-size", fontSize)
                .set("width", width).set("height", height).set("border-radius", "50%")
                .set("display", "flex").set("align-items", "center").set("justify-content", "center")
                .set("flex-shrink", "0");
        return av;
    }

    private void setInputEnabled(boolean on) {
        inputField.setEnabled(on);
        sendButton.setEnabled(on);
        chips.forEach(c -> c.getStyle()
                .set("opacity", on ? "1" : "0.45")
                .set("pointer-events", on ? "" : "none"));
    }

    private void enableInput() {
        streaming.set(false);
        setInputEnabled(true);
        inputField.focus();
    }

    private void scrollToBottom() {
        getElement().executeJs(
                "var el=this.querySelector('.cv-scroll');if(el)el.scrollTop=el.scrollHeight;"
        );
    }

    private void injectStyles() {
        getElement().executeJs("""
            if(!document.getElementById('cv-chat-css')){
              var s=document.createElement('style');
              s.id='cv-chat-css';
              s.textContent=`
                html, body, #outlet { margin: 0 !important; padding: 0 !important; overflow: hidden; }
                @keyframes blink  { 0%,80%,100%{ opacity:.2 } 40%{ opacity:1 } }
                @keyframes fadeUp { from{ opacity:0; transform:translateY(7px) } to{ opacity:1; transform:none } }
                .msg-row { animation: fadeUp .2s ease-out; }
                .cv-chip:hover {
                  background: #EEF2FF !important;
                  border-color: #6366F1 !important;
                  color: #4338CA !important;
                }
                .cv-input::part(input-field) {
                  border-radius: 24px;
                  background: #F8FAFC;
                }
                .cv-input::part(input-field):focus-within {
                  box-shadow: 0 0 0 2px rgba(99,102,241,0.25);
                }
                .cv-send-btn::part(base) {
                  background: #6366F1;
                  border-radius: 50%;
                  min-width: 44px;
                  min-height: 44px;
                  border: none;
                }
                .cv-send-btn::part(base):hover {
                  background: #4338CA;
                }
                @media (max-width: 680px) { .cv-sidebar { display: none !important; } }
              `;
              document.head.appendChild(s);
            }
            // Zero out every ancestor's margin/padding so the header truly reaches the edges
            var el = this;
            while (el) {
              el.style.margin = '0';
              el.style.padding = '0';
              el = el.parentElement;
            }
        """);
    }
}
