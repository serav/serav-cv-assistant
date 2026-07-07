package de.serav.cv.assistant.auth;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Component
public class TokenAuthenticationProvider implements AuthenticationProvider {

    private final AccessTokenRepository repository;
    private final MeterRegistry meterRegistry;
    private final String adminLabel;

    public TokenAuthenticationProvider(AccessTokenRepository repository,
                                       MeterRegistry meterRegistry,
                                       @Value("${app.console-label}") String adminLabel) {
        this.repository = repository;
        this.meterRegistry = meterRegistry;
        this.adminLabel = adminLabel;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        var tokenValue = sha256((String) authentication.getCredentials());

        var accessToken = repository.findByToken(tokenValue)
                .orElseThrow(() -> new BadCredentialsException("Invalid token"));

        if (accessToken.isExpired()) {
            throw new CredentialsExpiredException("Token has expired");
        }

        if (accessToken.isExhausted()) {
            throw new LockedException("Token has reached its maximum number of uses");
        }

        // Increment usage at most once per HTTP session — page reloads must not burn an attempt.
        var sessionKey = "scv_counted_" + accessToken.id();
        var attrs = RequestContextHolder.getRequestAttributes();
        var httpSession = attrs instanceof ServletRequestAttributes sra
                ? sra.getRequest().getSession(false) : null;
        if (httpSession == null || httpSession.getAttribute(sessionKey) == null) {
            repository.save(accessToken.incrementUsage());
            meterRegistry.counter("cv.login", "label", accessToken.label()).increment();
            if (httpSession != null) {
                httpSession.setAttribute(sessionKey, Boolean.TRUE);
            }
        }

        var authorities = new ArrayList<SimpleGrantedAuthority>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (adminLabel.equalsIgnoreCase(accessToken.label())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedToken(accessToken.id(), accessToken.label()), null, authorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private static String sha256(String input) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
