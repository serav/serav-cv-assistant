package de.serav.cv.assistant.auth;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccessTokenRepository extends CrudRepository<AccessToken, UUID> {
    Optional<AccessToken> findByToken(String token);
}
