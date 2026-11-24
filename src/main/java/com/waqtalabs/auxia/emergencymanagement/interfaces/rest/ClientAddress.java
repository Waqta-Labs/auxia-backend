package com.waqtalabs.auxia.emergencymanagement.interfaces.rest;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Address of the client that sent a request. Azure App Service receives the connection and
 * appends the address of the client, with its port, at the end of {@code X-Forwarded-For}; the
 * entries before it come from the client and can be anything, so only the last one counts.
 */
final class ClientAddress {

  private ClientAddress() {}

  static String of(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor == null || forwardedFor.isBlank()) {
      return request.getRemoteAddr();
    }
    String last = forwardedFor.substring(forwardedFor.lastIndexOf(',') + 1).strip();
    return last.isEmpty() ? request.getRemoteAddr() : withoutPort(last);
  }

  /** {@code 190.12.4.7:52814} and {@code [2001:db8::1]:52814} without the port. */
  private static String withoutPort(String address) {
    if (address.startsWith("[")) {
      int end = address.indexOf(']');
      return end > 0 ? address.substring(1, end) : address;
    }
    int colon = address.indexOf(':');
    boolean ipv4WithPort = colon > 0 && colon == address.lastIndexOf(':');
    return ipv4WithPort ? address.substring(0, colon) : address;
  }
}
