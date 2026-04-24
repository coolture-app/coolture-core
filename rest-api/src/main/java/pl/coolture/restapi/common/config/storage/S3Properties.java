package pl.coolture.restapi.common.config.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds the storage.s3.* block from application.yml.
 *
 * Two endpoints are needed because presigned URLs must be routable by
 * both the browser and the SSR server:
 *
 * internalEndpoint - internal address used by S3Client for admin operations
 * (create bucket, delete object, etc.). Never exposed to clients.
 *
 * publicEndpoint - address embedded in presigned GET/PUT URLs that are
 * returned to callers. Must be reachable from outside Docker.
 * Pointing at the Gateway (HOST_NAME:GATEWAY_HOST_PORT) works
 * because Gateway has a route for /{bucket-name}/**
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "storage.s3")
public class S3Properties {

  /** e.g. http://s3garage:3900 — internal Docker address */
  private String internalEndpoint;

  /**
   * e.g. http://localhost:8080 — Gateway address reachable by browser and SSR.
   * Set via storage.s3.public-endpoint in application.yml.
   */
  private String publicEndpoint;

  private String region;
  private String accessKey;
  private String secretKey;
  private String bucketName;
}