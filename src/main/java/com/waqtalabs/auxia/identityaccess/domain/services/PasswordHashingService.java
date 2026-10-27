package com.waqtalabs.auxia.identityaccess.domain.services;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.PasswordHash;

/** Keeps the hashing algorithm of the passwords out of the domain. */
public interface PasswordHashingService {

  PasswordHash hash(String rawPassword);

  boolean matches(String rawPassword, PasswordHash hash);
}
