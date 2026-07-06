package de.serav.cv.assistant.ui;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.List;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

@Route("login")
@PageTitle("Login — Sergiu Avram — CV Assistant")
@AnonymousAllowed
public class LoginView extends Div implements BeforeEnterObserver {

    private final AuthenticationManager authManager;
    private final PasswordField tokenField = new PasswordField("Access token");

    public LoginView(AuthenticationManager authManager) {
        this.authManager = authManager;

        getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("width", "100%")
                .set("height", "100vh")
                .set("background", "#F8FAFC")
                .set("font-family", "'Inter', 'Segoe UI', sans-serif");

        var card = new Div();
        card.getStyle()
                .set("background", "white")
                .set("border-radius", "16px")
                .set("box-shadow", "0 4px 24px rgba(0,0,0,0.08)")
                .set("padding", "40px 48px")
                .set("width", "100%")
                .set("max-width", "400px")
                .set("box-sizing", "border-box");

        var title = new H1("Sergiu Avram's CV Assistant");
        title.getStyle()
                .set("margin", "0 0 6px 0")
                .set("font-size", "1.6rem")
                .set("font-weight", "700")
                .set("color", "#1E293B");

        var subtitle = new Paragraph("Please enter your access token to continue.");
        subtitle.getStyle()
                .set("margin", "0 0 28px 0")
                .set("font-size", "0.88rem")
                .set("color", "#64748B");

        tokenField.setWidthFull();
        tokenField.getStyle().set("margin-bottom", "16px");

        var error = new Span("Invalid token. Please try again.");
        error.getStyle()
                .set("color", "#DC2626")
                .set("font-size", "0.82rem")
                .set("display", "none");

        var loginBtn = new Button("Continue");
        loginBtn.setWidthFull();
        loginBtn.getStyle()
                .set("background", "#4F46E5")
                .set("color", "white")
                .set("border-radius", "8px")
                .set("font-weight", "600")
                .set("height", "42px")
                .set("cursor", "pointer")
                .set("margin-top", "4px");

        Runnable attempt = () -> {
            try {
                var auth = authManager.authenticate(
                        new UsernamePasswordAuthenticationToken("user", tokenField.getValue()));
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(auth);
                SecurityContextHolder.setContext(context);
                var httpRequest = VaadinServletRequest.getCurrent().getHttpServletRequest();
                var httpResponse = VaadinServletResponse.getCurrent().getHttpServletResponse();
                httpRequest.changeSessionId();
                new HttpSessionSecurityContextRepository().saveContext(context, httpRequest, httpResponse);
                // Full browser redirect so the updated session cookie (from changeSessionId) reaches
                // the browser before any subsequent page load — prevents re-authentication on F5.
                getUI().ifPresent(ui -> ui.getPage().setLocation("/"));
            } catch (CredentialsExpiredException ex) {
                error.setText("This token has expired. Please request a new one.");
                error.getStyle().set("display", "block");
                loginBtn.setEnabled(false);
            } catch (LockedException ex) {
                error.setText("This token has reached its maximum number of uses. Please request a new one.");
                error.getStyle().set("display", "block");
                loginBtn.setEnabled(false);
            } catch (AuthenticationException ex) {
                error.setText("Invalid token. Please try again.");
                error.getStyle().set("display", "block");
                tokenField.clear();
            }
        };

        loginBtn.addClickListener(e -> attempt.run());
        tokenField.addKeyPressListener(Key.ENTER, e -> attempt.run());

        card.add(title, subtitle, tokenField, error, loginBtn);
        add(card);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            event.forwardTo(ChatView.class);
            return;
        }
        var token = event.getLocation().getQueryParameters()
                .getParameters()
                .getOrDefault("token", List.of())
                .stream().findFirst().orElse(null);
        if (token != null) {
            tokenField.setValue(token);
        }
    }
}
