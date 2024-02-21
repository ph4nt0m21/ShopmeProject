package com.shopme.section;

import com.shopme.common.entity.section.Section;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SectionService {
    @Autowired private SectionRepository repo;

    public List<Section> listEnabledSections() {
        return repo.findAllByEnabledOrderBySectionOrderAsc(true);
    }
}