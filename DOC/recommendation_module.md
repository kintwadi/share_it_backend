
## Role & Context
You are an experienced Java developer tasked with building a simple recommendation system for a peer-to-peer item listing platform. 
The system will help users decide whether to lend or sell items instead of giving them away for free, based on historical transaction data.
Yor task will be to extand the functionality to the existing rest application. The main idea is to create a separated module and do not change the existing code base in a significant way.
Allowing the existing application to function normaly even without this new module.

## Project Overview
Build a Spring Boot application that uses Apache Mahout's collaborative filtering to analyze past item transactions and provide recommendations. When a user lists an item for giveaway, the system should:
1. Find similar items from the database that were previously lent or sold
2. Analyze the transaction patterns
3. Suggest whether the user should lend or sell the item instead
4. Recommend an appropriate price based on historical data

## Technical Requirements

### Core Technologies
- PostgreSQL database
- Apache Mahout for collaborative filtering
- RESTful API design

### Functional Requirements

#### 1. Data Models
Create entities to represent (if not yet exist):
- **Items**: Store all item details including name, category, description, estimated value
- **Transactions**: Track what happened to each item (given away, sold, lent) with associated prices
- **Users**: Track which users listed/interacted with items

#### 2. Database Layer
Implement repositories with Spring Data JPA to:
- Store and retrieve item transaction history
- Find items by category and transaction type
- Query for items that were successfully transacted (sold/lent)
- Support basic CRUD operations for items

#### 3. Recommendation Engine
Build a service layer that:
- Converts database records into the format Mahout expects (userID, itemID, preference score)
- Calculates preference scores based on transaction type and price
- Builds and maintains a collaborative filtering model using Mahout
- Finds similar items based on user behavior patterns
- Caches item data for efficient access

#### 4. Business Logic
Implement the core recommendation logic to:
- Accept a new item listing for evaluation
- Query for similar items using Mahout's recommendations
- Calculate an average recommended price from similar items
- Determine the most common transaction type among similar items
- Generate a confidence score based on data quality and quantity
- Produce a human-readable suggestion for the user

#### 5. REST API
Create endpoints for:
- **POST /api/listings/evaluate**: Submit an item for recommendation analysis
- **POST /api/listings/create**: Save the final listing with user's chosen transaction type
- **POST /api/listings/admin/rebuild-model**: Manually trigger model rebuild (admin only)

#### 6. Configuration & Initialization
- Configure database connection properties
- Initialize the recommendation model on application startup
- Generate sample data for testing and development ( extend seeded data)

### Non-Functional Requirements

#### Performance
- Recommendation generation should complete within 2-3 seconds
- Cache frequently accessed data to minimize database queries
- Handle cases where insufficient data exists gracefully

#### Code Quality

- Follow standard Java naming conventions and best practices
- Include comprehensive error handling and logging
- Write clean, maintainable code with proper separation of concerns
- Add meaningful comments for complex logic

## Expected Deliverables

Provide a complete implementation that includes:

1. **Project Structure**: Well-organized packages for models, repositories, services, controllers, and DTOs

2. **Database Schema**: Entity classes with proper JPA annotations and relationships

3. **Repository Layer**: Spring Data JPA repositories with custom query methods

4. **Service Layer**:
   - Mahout integration service for building and querying the recommendation model
   - Business logic service for evaluating items and generating suggestions
   - Data initialization service for sample data

5. **API Layer**: REST controllers with proper request/response handling

6. **Configuration**: Application properties and configuration classes

7. **Documentation**: Clear comments explaining the recommendation logic and Mahout integration

## Success Criteria
The implementation should:
- Successfully identify similar items based on user behavior patterns
- Provide reasonable price suggestions based on historical data
- Include confidence scoring to indicate reliability of recommendations
- Handle cases with insufficient data without crashing
- Be easily testable with sample data
- Follow Java best practices and Spring Boot conventions

## Constraints & Considerations
- Start with collaborative filtering before considering more complex AI approaches
- Focus on simplicity and maintainability over complex algorithms
- Ensure the system can be extended later with more sophisticated ML models
- Consider the cold-start problem when little historical data exists
- Design for scalability as the dataset grows

## Multi-Tenant Environment Note

This project now supports static database-per-tenant routing in the backend configuration layer.

- Main env vars: `SETTING_USE_DEFAULT_DATABASE`, `TENANT_HEADER_NAME`, `TENANT_DEFAULT_ID`, `TENANT_DEFAULT_DB_URL`, `TENANT_DEFAULT_DB_USERNAME`, `TENANT_DEFAULT_DB_PASSWORD`, `TENANT_DEFAULT_DB_DRIVER`
- Optional extra tenant examples: `TENANT_A_*`, `TENANT_B_*`
- Default behavior is backward compatible when `SETTING_USE_DEFAULT_DATABASE=true`
- Full setup details live in `DOC/configuration-guide.md` and `.env.template`

