package pl.coolture.restapi.common.config.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "storage.s3")
@Data
@Component
public class S3Properties {
  private String region;
  private String endpoint;
  private String bucketName;
  private String accessKey;
  private String secretKey;
}
