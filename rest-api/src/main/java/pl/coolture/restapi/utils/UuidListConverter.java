package pl.coolture.restapi.utils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Converter
public class UuidListConverter implements AttributeConverter<List<UUID>, String> {

  @Override
  public String convertToDatabaseColumn(List<UUID> attribute) {
    if (attribute == null || attribute.isEmpty()) {
      return "";
    }
    return attribute.stream().map(UUID::toString).collect(Collectors.joining(","));
  }

  @Override
  public List<UUID> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.trim().isEmpty()) {
      return Collections.emptyList();
    }
    return Arrays.stream(dbData.split(",")).map(UUID::fromString).collect(Collectors.toList());
  }
}
