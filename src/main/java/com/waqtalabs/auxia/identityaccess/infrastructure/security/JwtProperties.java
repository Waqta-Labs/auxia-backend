package com.waqtalabs.auxia.identityaccess.infrastructure.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Signing of the access tokens, under {@code auxia.security.jwt}.
 *
 * @param keys signing keys by their id ({@code kid}), each one a Base64 secret of at least 32
 *     bytes. A new key is added next to the old one and made active; tokens signed with the old
 *     key stay valid until they expire (key rotation without restart of the clients, R-05).
 * @param activeKeyId the key that signs new tokens
 * @param expiration how long a token is valid
 */
@Validated
@ConfigurationProperties("auxia.security.jwt")
public record JwtProperties(
    @NotEmpty Map<String, String> keys,
    @NotBlank String activeKeyId,
    @DefaultValue("auxia-backend") String issuer,
    @NotNull @DefaultValue("8h") Duration expiration) {}
