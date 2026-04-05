package pl.coolture.gateway.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "coolture")
public record CooltureGatewayProperties(Cors cors) {

  public record Cors(List<String> allowedOrigins) {}
}
