package com.vicinity24.api.admin.controller;

import com.vicinity24.api.admin.dto.AdminExchangeLocationDTO;
import com.vicinity24.api.admin.dto.AdminExchangeLocationUpsertRequest;
import com.vicinity24.api.admin.service.AdminExchangeLocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/pickup-locations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminExchangeLocationController {
    private final AdminExchangeLocationService service;

    public AdminExchangeLocationController(AdminExchangeLocationService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<AdminExchangeLocationDTO>> list() {
        return ResponseEntity.ok(service.listAll());
    }

    @PostMapping
    public ResponseEntity<AdminExchangeLocationDTO> create(@RequestBody AdminExchangeLocationUpsertRequest body) {
        return ResponseEntity.ok(service.create(body));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminExchangeLocationDTO> update(@PathVariable("id") UUID id, @RequestBody AdminExchangeLocationUpsertRequest body) {
        return ResponseEntity.ok(service.update(id, body));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") UUID id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }
}

