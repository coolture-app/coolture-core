package pl.coolture.restapi.common.mapper;

import org.mapstruct.MapperConfig;
import org.mapstruct.MappingConstants;
import org.mapstruct.NullValueMappingStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * Shared MapStruct config inherited by every mapper
 *
 * Usage:
 *  @Mapper(config = BaseMapperConfig.class)
 *  public interface PostMapper { ... }
 *
 *  componentModel = SPRING
 *      Mappers are Spring beans that are injected via @Autowired / constructor injection
 *      like any other component. No MapperFactory.getMapper() calls needed.
 *
 *  nullValuePropertyMappingStrategy = IGNORE
 *      Null source fields are skipped instead of overwriting
 *      the existing entity value with null.
 *
 *  nullValueMappingStrategy = RETURN_NULL
 *      When the entire source object is null, return null rather than an
 *      empty target instance.
 *
 *  unmappedTargetPolicy = ERROR
 *      Any target field not covered by a mapping rule causes a compile-time
 *      error.  Forces explicit decisions:
 *      - either map the field
 *      - mark it @Mapping(target = "x", ignore = true)
 *      - or add it to the source DTO
 *      Prevents data loss when entities grow new columns.
 *
 *  unmappedSourcePolicy = WARN
 *      Source fields that are not mapped to any target produce a compile
 *      warning rather than an error.
 */
@MapperConfig(
        componentModel                      = MappingConstants.ComponentModel.SPRING,
        nullValuePropertyMappingStrategy    = NullValuePropertyMappingStrategy.IGNORE,
        nullValueMappingStrategy            = NullValueMappingStrategy.RETURN_NULL,
        unmappedTargetPolicy                = ReportingPolicy.ERROR,
        unmappedSourcePolicy                = ReportingPolicy.WARN
)
public interface BaseMapperConfig {}