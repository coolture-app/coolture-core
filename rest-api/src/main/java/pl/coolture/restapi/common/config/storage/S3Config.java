package pl.coolture.restapi.common.config.storage;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

  /**
   * Server-side operations (putObject, deleteObject, headObject).
   * Uses internalEndpoint (s3garage:3900) — reachable inside Docker network.
   */
  @Bean
  public S3Client s3Client(S3Properties props) {
    S3Configuration serviceConfig = S3Configuration.builder()
            .pathStyleAccessEnabled(true)
            .checksumValidationEnabled(false)
            .build();

    return S3Client.builder()
            .endpointOverride(URI.create(props.getInternalEndpoint()))
            .region(Region.of(props.getRegion()))
            .credentialsProvider(credentials(props))
            .serviceConfiguration(serviceConfig)
            .build();
  }

  /**
   * Generates presigned GET/PUT URLs handed to the browser.
   * Uses publicEndpoint (localhost:3900) — reachable from outside Docker.
   * Signature is computed against this host, so the browser hits the same
   * host and the signature stays valid.
   */
  @Bean
  public S3Presigner s3Presigner(S3Properties props) {
    S3Configuration serviceConfig = S3Configuration.builder()
            .pathStyleAccessEnabled(true)
            .build();

    return S3Presigner.builder()
            .endpointOverride(URI.create(props.getPublicEndpoint()))
            .region(Region.of(props.getRegion()))
            .credentialsProvider(credentials(props))
            .serviceConfiguration(serviceConfig)
            .build();
  }

  private StaticCredentialsProvider credentials(S3Properties props) {
    return StaticCredentialsProvider.create(
            AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey()));
  }
}