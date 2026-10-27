package com.waqtalabs.auxia.identityaccess.infrastructure.security;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.PasswordHash;
import com.waqtalabs.auxia.identityaccess.domain.services.PasswordHashingService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** Hashes passwords with BCrypt (Spring Security). */
@Component
class BCryptPasswordHashingService implements PasswordHashingService {

  static final String ALGORITHM = "BCRYPT";

  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  @Override
  public PasswordHash hash(String rawPassword) {
    return new PasswordHash(encoder.encode(rawPassword), ALGORITHM);
  }

  @Override
  public boolean matches(String rawPassword, PasswordHash hash) {
    return ALGORITHM.equals(hash.algorithm()) && encoder.matches(rawPassword, hash.value());
  }
}
