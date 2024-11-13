package com.delta.dms.community.service.autocomplete;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.MapUtils.emptyIfNull;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import com.delta.datahive.searchapi.SearchManager;
import com.delta.datahive.searchobj.param.SuggestionSetting;
import com.delta.datahive.searchobj.response.Suggestion;
import com.delta.datahive.searchobj.response.Suggestions;
import com.delta.dms.community.config.AutocompleteConfig;
import com.delta.dms.community.model.Jwt;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserAutocompleteService {

  private final SearchManager searchManager;
  private final AutocompleteConfig autocompleteConfig;

  private static final String PAYLOAD_USER_ID = "id";

  public Map<String, String> getSuggestions(String q, int count) {
    SuggestionSetting settings =
        new SuggestionSetting(q, autocompleteConfig.getUserFilters(), count);
    Suggestions suggestions = searchManager.suggest(settings, Jwt.get());
    return Stream.of(suggestions.getPrefixSuggestionList(), suggestions.getSuggestionList())
        .filter(CollectionUtils::isNotEmpty)
        .flatMap(List::stream)
        .filter(Objects::nonNull)
        .filter(
            suggestion ->
                emptyIfNull(suggestion.getPayload())
                    .getOrDefault(PAYLOAD_USER_ID, emptyList())
                    .stream()
                    .findFirst()
                    .filter(StringUtils::isNotBlank)
                    .isPresent())
        .sorted(Comparator.comparingDouble(Suggestion::getWeight).reversed())
        .collect(
            LinkedHashMap::new,
            (map, item) ->
                map.put(
                    item.getPayload().get(PAYLOAD_USER_ID).stream().findFirst().get(),
                    item.getTerm()),
            Map::putAll);
  }
}
