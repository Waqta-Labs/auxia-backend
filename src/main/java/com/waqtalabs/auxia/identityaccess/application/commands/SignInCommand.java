package com.waqtalabs.auxia.identityaccess.application.commands;

/** The password is checked and never kept. */
public record SignInCommand(String email, String password) {}
