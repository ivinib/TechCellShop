# Public demo

This document captures the public deployment entry points for TechCellShop.

> Replace the placeholder URLs below with the real hosted application URLs.

---

## Public endpoints

- Base URL: `https://<your-app-url>`
- Swagger UI: `https://<your-app-url>/swagger-ui.html`
- OpenAPI docs: `https://<your-app-url>/v3/api-docs`
- Health: `https://<your-app-url>/actuator/health`
- Readiness: `https://<your-app-url>/actuator/health/readiness`

---

## Recommended quick evaluation flow

1. Open Swagger UI
2. Inspect health endpoint
3. Authenticate and obtain a JWT
4. Call one protected endpoint
5. Verify business flow through order placement and payment action

---

## Runtime assumptions

The hosted deployment is expected to run with:

- `SPRING_PROFILES_ACTIVE=prod`
- PostgreSQL configured through environment variables
- RabbitMQ configured through environment variables
- Flyway enabled
- JWT secret configured through environment variables

See also:
- `docs/deployment/railway.md`
- `docs/deployment/post-deploy-smoke-test.md`
