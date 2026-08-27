package org.zerozero.opensource.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ingest.graph")
public record GraphIngestionProperties(boolean enabled, Path path) {
  public GraphIngestionProperties {
    if (path == null) {
      path = Path.of("/Users/seoyeong/Desktop/companyx-dataset-v1.0/graph");
    }
  }
}
