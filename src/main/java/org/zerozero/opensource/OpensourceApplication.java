package org.zerozero.opensource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = {"org.zerozero.opensource", "zerozero.opensource"})
@ConfigurationPropertiesScan(basePackages = {"org.zerozero.opensource", "zerozero.opensource"})
public class OpensourceApplication {

  public static void main(String[] args) {
    SpringApplication.run(OpensourceApplication.class, args);
  }
}
