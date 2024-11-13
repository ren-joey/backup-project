package com.delta.dms.community.service.view.converter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class GroupMemberConverterFactory {

  private final List<GroupMemberConverter> converters;
  private static final Map<String, GroupMemberConverter> converterMap = new HashMap<>();

  @PostConstruct
  public void init() {
    converters.forEach(converter -> converterMap.put(converter.type().name(), converter));
  }

  public GroupMemberConverter getConverter(String type) {
    if (ManagerConverterType.valuesString().contains(type)) {
      return converterMap.get(type);
    } else {
      return converterMap.get(ManagerConverterType.DEFAULT.name());
    }
  }

  public enum ManagerConverterType {
    __Admin, __Manager, __AllMembers, __KnowledgeAdmin, __KmKnowledgeUnit, __SupplierKU, __Km, DEFAULT;

    public static Set<String> valuesString() {
      return Arrays.stream(values()).map(Enum::name).collect(Collectors.toSet());
    }
  }
}
