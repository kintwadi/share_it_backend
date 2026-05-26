package com.nearshare.api.enterprise.service;

import com.nearshare.api.enterprise.dto.EnterpriseCategoryDTOs;
import com.nearshare.api.enterprise.model.EnterpriseCategory;
import com.nearshare.api.enterprise.repository.EnterpriseCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnterpriseCategoryService {
    private final EnterpriseCategoryRepository repository;

    @Transactional
    public void ensureSeededFromMarkdown() {
        if (repository.countBy() > 0) {
            return;
        }
        String md = loadMarkdown();
        if (md == null || md.isBlank()) {
            return;
        }
        List<EnterpriseCategory> rows = parseMarkdown(md).stream()
                .map(r -> EnterpriseCategory.builder()
                        .id(UUID.randomUUID())
                        .sector(r.sector)
                        .categoryGroup(r.group)
                        .itemLabel(r.item)
                        .keywords(String.join(",", r.keywords))
                        .createdAt(LocalDateTime.now())
                        .build())
                .toList();
        repository.saveAll(rows);
    }

    @Transactional(readOnly = true)
    public List<EnterpriseCategoryDTOs.CategorySector> getHierarchy() {
        List<EnterpriseCategory> rows = repository.findAllByOrderBySectorAscCategoryGroupAscItemLabelAsc();
        Map<String, Map<String, List<EnterpriseCategory>>> grouped = new LinkedHashMap<>();
        for (EnterpriseCategory r : rows) {
            grouped.computeIfAbsent(r.getSector(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(r.getCategoryGroup(), k -> new ArrayList<>())
                    .add(r);
        }

        List<EnterpriseCategoryDTOs.CategorySector> result = new ArrayList<>();
        for (var sectorEntry : grouped.entrySet()) {
            List<EnterpriseCategoryDTOs.CategoryGroup> groups = new ArrayList<>();
            for (var groupEntry : sectorEntry.getValue().entrySet()) {
                List<EnterpriseCategoryDTOs.CategoryItem> items = groupEntry.getValue().stream()
                        .map(r -> EnterpriseCategoryDTOs.CategoryItem.builder()
                                .label(r.getItemLabel())
                                .keywords(splitKeywords(r.getKeywords()))
                                .build())
                        .toList();
                groups.add(EnterpriseCategoryDTOs.CategoryGroup.builder()
                        .label(groupEntry.getKey())
                        .items(items)
                        .build());
            }
            result.add(EnterpriseCategoryDTOs.CategorySector.builder()
                    .label(sectorEntry.getKey())
                    .groups(groups)
                    .build());
        }
        return result;
    }

    private List<String> splitKeywords(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return List.of(raw.split(",")).stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private String loadMarkdown() {
        try {
            Path p = Path.of("categories.md");
            if (Files.exists(p)) {
                return Files.readString(p);
            }
        } catch (Exception ignored) { }

        try {
            ClassPathResource res = new ClassPathResource("categories.md");
            if (!res.exists()) {
                return "";
            }
            try (InputStream in = res.getInputStream()) {
                return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n"));
            }
        } catch (Exception ignored) {
            return "";
        }
    }

    private record ParsedRow(String sector, String group, String item, List<String> keywords) {}

    private List<ParsedRow> parseMarkdown(String md) {
        List<ParsedRow> out = new ArrayList<>();
        String currentSector = null;
        String currentGroup = null;

        for (String raw : md.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            if (line.equals("---")) continue;

            if (line.startsWith("### ")) {
                String label = line.substring(4).trim().replaceFirst("^\\d+\\.\\s*", "").trim();
                currentSector = label;
                currentGroup = null;
                continue;
            }
            if (line.startsWith("#### ")) {
                currentGroup = line.substring(5).trim();
                continue;
            }
            if (line.startsWith("- ")) {
                if (currentSector == null) continue;
                if (currentGroup == null) currentGroup = "General";
                String item = line.substring(2).trim();
                if (item.isEmpty()) continue;
                out.add(new ParsedRow(currentSector, currentGroup, item, tokenize(item)));
            }
        }

        return dedupe(out);
    }

    private List<ParsedRow> dedupe(List<ParsedRow> rows) {
        Set<String> seen = new LinkedHashSet<>();
        List<ParsedRow> out = new ArrayList<>();
        for (ParsedRow r : rows) {
            String k = (r.sector + "|" + r.group + "|" + r.item).toLowerCase(Locale.ROOT);
            if (seen.add(k)) out.add(r);
        }
        return out;
    }

    private List<String> tokenize(String label) {
        String cleaned = label
                .replace("(", " ")
                .replace(")", " ")
                .replace("&", " ")
                .replaceAll("[^\\w\\s-]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);

        Set<String> uniq = new LinkedHashSet<>();
        for (String p : cleaned.split(" ")) {
            if (p.isBlank()) continue;
            if (p.length() < 2) continue;
            uniq.add(p);
        }
        return new ArrayList<>(uniq);
    }
}

