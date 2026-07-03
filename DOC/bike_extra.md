# Domain-Driven Design (DDD) & Modular System Architecture Specification
**Target Domain:** Bicycle Vertical Module (`com.vicinity24.api.bicycle` / `frontend/bike`)  
**Stack Alignment:** Java Spring Boot Backend / Angular Frontend  
**Core Business Objectives:** Endmontage workshop buffer integration & Rent-to-Own dynamic financial conversion engine  

---

## Part 1: Backend Architecture (com.vicinity24.api.bicycle)

To protect the stability of the core engine, the bicycle domain utilizes composition and interface inheritance instead of deep class inheritance. This isolates bicycle-specific data attributes from the generic platform logic.

### 1. Package Directory Layout

```text
com.vicinity24
 ├── core                  <-- Existing generic platform
 │    ├── model            <-- (Listing.java, Booking.java)
 │    ├── service          <-- (PaymentService.java, AuthService.java)
 │    └── controller
 └── bicycle               <-- Isolated Module (Zero changes to Core required)
      ├── domain
      │    ├── model       <-- Extends or wraps Core entities
      │    └── valueobject <-- Bike-specific structures (FrameSize, BikeType)
      ├── repository       <-- Bike-specific DB queries
      ├── service          <-- Handles Endmontage buffers & conversion logic
      ├── dto              <-- Specialized payload models
      └── controller       <-- Endpoints for /api/v1/bikes
```

### 2. Java Domain Implementation Example

```java
package com.vicinity24.api.bicycle.domain.model;

import com.vicinity24.core.model.Listing; // Reusing your core listing
import java.math.BigDecimal;

public class BicycleListing {
    
    // Composition: Wraps the core listing (holds generic title, price, store_id)
    private Listing coreListing; 
    
    // Bike-Specific Domain Attributes
    private String frameSize;
    private String bikeType; // E-Bike, Road, Cargo, etc.
    private int assemblyBufferMinutes; 
    private boolean isRentToOwnEligible;
    private BigDecimal retailPurchasePrice;

    // Getters, Setters, and Domain Logic
    public boolean requiresHeavyAssembly() {
        return this.assemblyBufferMinutes > 120;
    }
}
```

### 3. Service Layer Integration (Reusing Payments/Auth)

```java
package com.vicinity24.api.bicycle.service;

import com.vicinity24.core.service.PaymentService; // Reusing Core Payment Engine
import com.vicinity24.api.bicycle.domain.model.BicycleListing;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class BicycleConversionService {

    @Autowired
    private PaymentService corePaymentService;

    public void convertRentalToPurchase(String bookingId, BicycleListing bike) {
        // 1. Fetch rental history totals from core database
        BigDecimal amountAlreadyPaid = corePaymentService.getTotalPaidForBooking(bookingId);
        
        // 2. Calculate remaining dynamic balance
        BigDecimal finalPrice = bike.getRetailPurchasePrice().subtract(amountAlreadyPaid);
        
        // 3. Execute final settlement using your existing payment gateway wrapper
        corePaymentService.executeCharge(bookingId, finalPrice);
    }
}
```

---

## Part 2: Angular Frontend Architecture (frontend/bike)

The bicycle user experience is encapsulated entirely within a single lazy-loaded feature module. This keeps the application bundle lightweight and guarantees physical separation of vertical-specific source code.

### 1. Frontend Directory Layout

```text
frontend
 ├── src
 │    ├── app
 │    │    ├── core          <-- Core layout, AuthService, Interceptors
 │    │    └── shared        <-- Reusable UI elements (Buttons, generic maps)
 │    └── bike               <-- Isolated Bike Feature Module
 │         ├── components    <-- Presentational Views
 │         │    ├── handover-checklist
 │         │    └── rent-to-own-calculator
 │         ├── pages
 │         │    ├── bike-detail
 │         │    └── bike-search
 │         ├── services      <-- Connects to /api/v1/bikes endpoints
 │         ├── bike-routing.module.ts
 │         └── bike.module.ts <-- Self-contained declarations
```

### 2. Angular Core Routing Configuration

```typescript
import { Routes } from '@angular/router';
import { HomeComponent } from './home.component';

const routes: Routes = [
  { path: '', component: HomeComponent },
  { 
    path: 'bikes', 
    loadChildren: () => import('./bike/bike.module').then(m => m.BikeModule) 
  }
];
```

---

## Part 3: UI/UX Wireframe & Template Concepts

The isolated module frontend components directly interface with the domain model extension to display data metrics for both assembly lead times and pricing adjustments.

### Component 1: The "Ready-to-Ride" Delivery Badge

```html
<!-- bike-detail.component.html -->
<div class="status-badge" [ngClass]="bike.inventoryStatus">
  @if (bike.inventoryStatus === 'on_floor_assembled') {
    <span class="instant">⚡ Ready for Pickup in 45 Mins</span>
  } @else {
    <span class="buffered">🔧 Workshop Prep Required: Ready in {{ bike.assemblyBufferMinutes / 60 }} Hours</span>
  }
</div>
```

### Component 2: The Rent-to-Own Interactive Checkout Widget

```text
+-------------------------------------------------------------+

|               CHOOSE YOUR RIDING EXPERIENCE                 |
|                                                             |
|  ( ) Short-Term Rental Only       [ €45 / Day ]             |
|  (X) Rent-to-Own Trial (3-Days)   [ €135 Total ]            |
|                                                             |
|  ---------------------------------------------------------  |
|  FINANCIAL BREAKDOWN IF YOU KEEP THE BIKE:                  |
|  Retail Price:                                   €2,999.00  |
|  Your Rental Credit Applied:                     - €135.00  |
|  ---------------------------------------------------------  |
|  Guaranteed Purchase Price on Day 3:             €2,864.00  |
|                                                             |
|  [ BOOK & RESERVE FOR PICKUP ]                              |
+-------------------------------------------------------------+
```
