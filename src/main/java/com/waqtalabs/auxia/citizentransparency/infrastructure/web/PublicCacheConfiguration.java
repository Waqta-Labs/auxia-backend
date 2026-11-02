package com.waqtalabs.auxia.citizentransparency.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Lets a CDN or the browser answer repeated public queries for 60 seconds without reaching the
 * API, for the peaks of citizen queries of refinement R-04. A change of stage shows, at most, a
 * minute later.
 */
@Configuration(proxyBeanMethods = false)
class PublicCacheConfiguration implements WebMvcConfigurer {

  static final String PUBLIC_PATHS = "/api/v1/public/**";
  static final String CACHE_CONTROL =
      CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic().getHeaderValue();

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(
            new HandlerInterceptor() {
              @Override
              public boolean preHandle(
                  HttpServletRequest request, HttpServletResponse response, Object handler) {
                response.setHeader(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL);
                return true;
              }
            })
        .addPathPatterns(PUBLIC_PATHS);
  }
}
