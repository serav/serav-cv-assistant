package de.serav.cv.assistant.auth;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TokenAuthenticationProvider implements AuthenticationProvider {

    private final AccessTokenRepository repository;

    public TokenAuthenticationProvider(AccessTokenRepository repository) {
        this.repository = repository;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        var tokenValue = (String) authentication.getCredentials();

        var accessToken = repository.findByToken(tokenValue)
                .orElseThrow(() -> new BadCredentialsException("Invalid token"));

        if (accessToken.isExpired()) {
            throw new CredentialsExpiredException("Token has expired");
        }

        if (accessToken.isExhausted()) {
            throw new LockedException("Token has reached its maximum number of uses");
        }

        repository.save(accessToken.incrementUsage());

        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedToken(accessToken.id(), accessToken.label()), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
