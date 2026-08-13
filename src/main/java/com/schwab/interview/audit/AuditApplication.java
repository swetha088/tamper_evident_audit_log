package com.schwab.interview.audit;

import com.schwab.interview.audit.config.AuditIntegrityProperties;
import com.schwab.interview.audit.config.AuditPerformanceProperties;
import com.schwab.interview.audit.config.AuditRetentionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
  AuditIntegrityProperties.class,
  AuditPerformanceProperties.class,
  AuditRetentionProperties.class
})
public class AuditApplication {
  public static void main(String[] args) {
    SpringApplication.run(AuditApplication.class, args);
  }
}
