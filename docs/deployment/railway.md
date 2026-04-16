# Deploying TechCellShop in a hosted environment

This guide describes a practical first hosted deployment target for TechCellShop using a production profile and environment-based configuration.

> Recommended first target: Railway  
> Reason: it maps well to the current project structure:
> - one Spring Boot app
> - one PostgreSQL database
> - one RabbitMQ service
> - Docker-based deployment from the existing `Dockerfile`

---

## Deployment model

TechCellShop requires:

- **Application**
    - Spring Boot web service
- **Database**
    - PostgreSQL
- **Messaging**
    - RabbitMQ

The application should run with:

```text
SPRING_PROFILES_ACTIVE=prod
