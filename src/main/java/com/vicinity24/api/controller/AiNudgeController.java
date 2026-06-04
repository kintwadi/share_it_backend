package com.vicinity24.api.controller;

import com.vicinity24.api.model.User;
import com.vicinity24.api.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
public class AiNudgeController {

    private final UserService userService;

    public AiNudgeController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/demand-check")
    public ResponseEntity<?> checkDemand(@AuthenticationPrincipal User user, @RequestBody Map<String, String> payload) {
        // Dummy implementation
        // Returns "high demand" randomly or based on item name
        String itemName = payload.get("itemName");
        boolean highDemand = itemName != null && (itemName.toLowerCase().contains("drill") || itemName.toLowerCase().contains("ladder"));
        
        if (highDemand) {
            return ResponseEntity.ok(Map.of(
                "nudgeType", "give_to_lend",
                "message", "High demand detected! Rent this for €15/day instead.",
                "potentialEarnings", "60",
                "demandScore", 0.85
            ));
        }
        
        return ResponseEntity.ok(Map.of("nudgeType", "none"));
    }
}
