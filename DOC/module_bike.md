# Specification & Implementation Blueprint: Isolated Bicycle Vertical Module

This document details the software architecture, database schemas, and folder structures required to add an isolated bicycle marketplace vertical to the existing `com.vicinity24` generic engine. It leverages interface inheritance, composition, and lazy loading to protect core code integrity.

---

## 1. System Directory Structure

### Backend Architecture (Java + Spring Boot)
The bicycle domain resides entirely within `com.vicinity24.api.bicycle`. It depends on classes in `com.vicinity24.api.core` but changes nothing within that core package.


---

## 5. AI Engineering Generation Prompts

Use these structured prompts to generate remaining codebase layers while enforcing complete module decoupling.

### Prompt 1: Spring Boot Repository & Controller Layer Generation
```text
Act as an expert Java software engineer specialized in Domain-Driven Design architectures. 
I need code written for an isolated module located in 'com.vicinity24.api.bicycle'. 

Generate the following components:
1. `BicycleListingRepository.java`: A Spring Data JPA repository using native PostgreSQL query joins between `public.listings` and `bicycle.bike_listings` to filter by frame_size and bike_type.
2. `BicycleCatalogController.java`: Endpoints mapped to `/api/v1/bikes` supporting search pagination.
3. `RentToOwnConversionService.java`: It must wire in a `com.vicinity24.api.core.service.PaymentService` reference to retrieve historical reservation expenses, subtract them from the bike's retail cost, and process a settlement charge.

Strict constraints:
- Do not import code classes outside of `com.vicinity24.api.core` and standard Spring libraries.
- Do not modify files in the `com.vicinity24.api.core` package. Use composition structures.
- Return only pure executable Java files.
```

### Prompt 2: Angular Component Interface Generation
```text
Act as an expert Angular frontend developer. 
I need you to build two presentation components located in the directory `src/app/bike/components`:

1. `handover-checklist`: A safety checklist layout executing product delivery requirements compliant with German traffic code (StVZO). It requires explicit confirmation fields for front/rear brakes, tires, stem bolt torque settings, electric controller statuses, and an integrated digital signature terminal panel.
