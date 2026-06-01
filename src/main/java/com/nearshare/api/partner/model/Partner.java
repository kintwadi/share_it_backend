package com.nearshare.api.partner.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "partners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Partner {
    @Id
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String contactPerson;
    @Enumerated(EnumType.STRING)
    private PartnerStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
