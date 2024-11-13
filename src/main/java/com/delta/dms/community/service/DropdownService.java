package com.delta.dms.community.service;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.delta.dms.community.adapter.MyDmsAdapter;
import com.delta.dms.community.config.EerpConfig;
import com.delta.dms.community.dao.DropdownDao;
import com.delta.dms.community.model.AcceptLanguage;
import com.delta.dms.community.swagger.model.LabelValueDto;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DropdownService {

  private final EerpConfig eerpConfig;
  private final DropdownDao dropdownDao;
  private final MyDmsAdapter myDmsAdapter;

  public List<LabelValueDto> getAppFieldDropdownList() {
    return dropdownDao
        .getAppFieldDropdownList(AcceptLanguage.getLanguageForDb())
        .stream()
        .map(item -> new LabelValueDto().value(item.getId()).label(item.getName()))
        .collect(Collectors.toList());
  }

  public List<LabelValueDto> getRecordDropdownList() {
    return myDmsAdapter
        .getRecordTypes()
        .stream()
        .filter(item -> eerpConfig.getMRecordType().contains(item.getValue()))
        .map(item -> new LabelValueDto().value(item.getValue()).label(item.getLabel()))
        .collect(Collectors.toList());
  }
}
