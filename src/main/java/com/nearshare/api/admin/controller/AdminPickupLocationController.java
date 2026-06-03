package com.nearshare.api.admin.controller;

import com.nearshare.api.admin.dto.AdminPickupLocationDTO;
import com.nearshare.api.admin.dto.AdminPickupLocationUpsertRequest;
import com.nearshare.api.admin.service.AdminPickupLocationService;
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
public class AdminPickupLocationController {
    private final AdminPickupLocationService service;

    public AdminPickupLocationController(AdminPickupLocationService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<AdminPickupLocationDTO>> list() {
        return ResponseEntity.ok(service.listAll());
    }

    @PostMapping
    public ResponseEntity<AdminPickupLocationDTO> create(@RequestBody AdminPickupLocationUpsertRequest body) {
        return ResponseEntity.ok(service.create(body));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminPickupLocationDTO> update(@PathVariable("id") UUID id, @RequestBody AdminPickupLocationUpsertRequest body) {
        return ResponseEntity.ok(service.update(id, body));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") UUID id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }
}

