package com.schwab.interview.audit.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class AuditSecurityConfiguration {

  @Configuration
  @Profile("local")
  static class LocalSecurityConfiguration {

    @Bean
    UserDetailsService localUserDetailsService() {
      return new InMemoryUserDetailsManager();
    }

    @Bean
    SecurityFilterChain localSecurityFilterChain(HttpSecurity http) throws Exception {
      return http.csrf(AbstractHttpConfigurer::disable)
          .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
          .build();
    }
  }

  @Configuration
  @Profile("prod")
  @EnableConfigurationProperties(AuditSecurityProperties.class)
  static class ProductionSecurityConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService productionUserDetailsService(
        AuditSecurityProperties properties, PasswordEncoder passwordEncoder) {
      return ignored ->
          User.withUsername(properties.getUsername())
              .password(passwordEncoder.encode(properties.getPassword()))
              .roles(properties.getRoles().toArray(String[]::new))
              .build();
    }

    @Bean
    SecurityFilterChain productionSecurityFilterChain(HttpSecurity http) throws Exception {
      return http.csrf(AbstractHttpConfigurer::disable)
          .sessionManagement(
              session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(
              authorize ->
                  authorize
                      .requestMatchers(HttpMethod.GET, "/audit/**")
                      .hasRole("AUDIT_READER")
                      .requestMatchers(HttpMethod.POST, "/audit/events/*/redactions")
                      .hasRole("AUDIT_PRIVACY_ADMIN")
                      .requestMatchers(HttpMethod.POST, "/audit/retention/run")
                      .hasRole("AUDIT_RETENTION_ADMIN")
                      .requestMatchers(HttpMethod.POST, "/audit/events")
                      .hasRole("AUDIT_WRITER")
                      .requestMatchers(HttpMethod.POST, "/audit/export/verify")
                      .hasRole("AUDIT_READER")
                      .anyRequest()
                      .denyAll())
          .httpBasic(Customizer.withDefaults())
          .build();
    }
  }
}
