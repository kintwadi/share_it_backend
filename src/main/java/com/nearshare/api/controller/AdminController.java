package com.nearshare.api.controller;

import com.nearshare.api.dto.ReportDTO;
import com.nearshare.api.dto.UserSummaryDTO;
import com.nearshare.api.dto.ListingDTO;
import com.nearshare.api.dto.LocationDTO;
import com.nearshare.api.model.Report;
import com.nearshare.api.model.User;
import com.nearshare.api.model.Listing;
import com.nearshare.api.repository.ReportRepository;
import com.nearshare.api.service.ListingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ReportRepository reportRepository;
    private final ListingService listingService; // Reusing for DTO conversion if needed, or mapping manually

    public AdminController(ReportRepository reportRepository, ListingService listingService) {
        this.reportRepository = reportRepository;
        this.listingService = listingService;
    }

    @GetMapping("/reports")
    public ResponseEntity<List<ReportDTO>> getReports() {
        List<Report> reports = reportRepository.findAll();
        
        // In a real app, use a proper mapper. Here we map manually or use ListingService methods if public
        // Since ListingService.toDTO is private, we'll map manually here for simplicity or expose a mapper
        
        List<ReportDTO> dtos = reports.stream().map(this::toDTO).collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }
    
    @DeleteMapping("/reports/{id}")
    public ResponseEntity<Map<String, String>> deleteReport(@PathVariable UUID id) {
        reportRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }

    private ReportDTO toDTO(Report r) {
        User reporter = r.getReporter();
        Listing l = r.getListing();
        
        UserSummaryDTO reporterDTO = reporter != null ? UserSummaryDTO.builder()
                .id(reporter.getId())
                .name(reporter.getName())
                .avatarUrl(reporter.getAvatarUrl())
                .trustScore(reporter.getTrustScore())
                .build() : null;
                
        // Simplified Listing DTO for the report view
        ListingDTO listingDTO = null;
        if (l != null) {
            listingDTO = ListingDTO.builder()
                .id(l.getId())
                .title(l.getTitle())
                .imageUrl(l.getImageUrl())
                .status(l.getStatus())
                .ownerId(l.getOwner() != null ? l.getOwner().getId() : null)
                .build();
        }

        return ReportDTO.builder()
                .id(r.getId())
                .reason(r.getReason())
                .details(r.getDetails())
                .timestamp(r.getTimestamp())
                .reporter(reporterDTO)
                .listing(listingDTO)
                .build();
    }
}
