package com.schwab.interview.audit.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
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
  static class ProductionSecurityConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService productionUserDetailsService(
        @Value("${audit.security.username}") String username,
        @Value("${audit.security.password}") String password,
        PasswordEncoder passwordEncoder) {
      return ignored ->
          User.withUsername(username)
              .password(passwordEncoder.encode(password))
              .roles("AUDIT_READER")
              .build();
    }

    @Bean
    SecurityFilterChain productionSecurityFilterChain(HttpSecurity http) throws Exception {
      return http.csrf(AbstractHttpConfigurer::disable)
          .sessionManagement(
              session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
          .httpBasic(Customizer.withDefaults())
          .build();
    }
  }
}
