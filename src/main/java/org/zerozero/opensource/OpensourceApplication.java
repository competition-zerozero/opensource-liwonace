package org.zerozero.opensource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OpensourceApplication {

  public static void main(String[] args) {
    SpringApplication.run(OpensourceApplication.class, args);
  }
}
