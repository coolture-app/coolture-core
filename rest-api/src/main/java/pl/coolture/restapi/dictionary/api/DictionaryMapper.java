package pl.coolture.restapi.dictionary.api;

import java.util.List;
import org.mapstruct.Mapper;
import pl.coolture.restapi.common.mapper.BaseMapperConfig;
import pl.coolture.restapi.dictionary.api.dto.CountryCodeDto;
import pl.coolture.restapi.dictionary.domain.CountryCode;

/**
 * Maps dictionary entities to their API DTOs
 */
@Mapper(config = BaseMapperConfig.class)
public interface DictionaryMapper {

    CountryCodeDto toDto(CountryCode countryCode);

    List<CountryCodeDto> toCountryCodeDtoList(List<CountryCode> codes);
}