package com.vicinity24.api.core.recommendation.controller;

import com.vicinity24.api.core.recommendation.model.EvaluateItemRequest;
import com.vicinity24.api.core.recommendation.model.RecommendationResult;
import com.vicinity24.api.core.recommendation.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
@Tag(name = "Recommendation", description = "Recommendation Engine Endpoints")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(summary = "Evaluate an item for recommendation", description = "Suggests whether to sell, lend, or give based on similar items.")
    @PostMapping("/evaluate")
    public ResponseEntity<RecommendationResult> evaluate(@RequestBody EvaluateItemRequest request) {
        return ResponseEntity.ok(recommendationService.evaluate(request));
    }

    @Operation(summary = "Rebuild recommendation model", description = "Admin only. Triggers a full rebuild of the Mahout model.")
    @PostMapping("/admin/rebuild-model")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> rebuildModel() {
        recommendationService.rebuildModel();
        return ResponseEntity.ok("Model rebuild triggered successfully.");
    }
}
