# Render Production Infrastructure Cost Breakdown

This document tracks the active hosting costs for the full-stack application (Angular + Spring Boot + PostgreSQL) deployed on Render. The architecture is optimized for 24/7 production uptime with zero cold starts, minimal network latency, and zero workspace overhead.

## Monthly Cost Summary


| Component | Service Type | Render Tier / Specification | Monthly Cost |
| :--- | :--- | :--- | :--- |
| **Workspace** | Account Management | Hobby Workspace | $0.00 |
| **Frontend** | Angular Application | Static Site | $0.00 |
| **Backend API** | Spring Boot REST API | Starter Compute (512 MB RAM / 0.5 CPU) | $7.00 |
| **Database Compute** | PostgreSQL Instance | Basic-256mb (256 MB RAM) | $6.00 |
| **Database Storage** | PostgreSQL Disk | 15 GB Dedicated Storage | $4.50 |
| **Total Expected Cost** | | | **$17.50 / month** |

---

## Architectural Notes & Optimizations

### 1. Networking & Latency
* The Spring Boot API and the PostgreSQL database both reside within the same Render data center region.
* They communicate using the **Internal Connection String**, dropping database query latency to under 1ms.

### 2. Spring Boot Configuration (`application.properties`)
To safely run on these resource-constrained tiers without triggering Out-Of-Memory (OOM) errors, the following limits are applied:

```properties
# Cap Hikari connection pool size to match the 256MB DB RAM
spring.datasource.hikari.maximum-pool-size=4

# Use the internal Render database URL
spring.datasource.url=\${DATABASE_INTERNAL_URL}
```

### 3. JVM Optimization
If memory usage on the $7.00 Starter compute instance nears its 512 MB ceiling during high traffic, restrict the maximum Java heap size using Render Environment Variables:
* **Key:** `JAVA_TOOL_OPTIONS`
* **Value:** `-Xmx350m`
