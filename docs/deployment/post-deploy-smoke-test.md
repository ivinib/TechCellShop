# Post-deploy smoke test checklist

Use this checklist after deploying the application to a hosted environment.

> Replace `https://<your-app-url>` with the real application URL.

---

## 1. Documentation and availability

- [ ] `GET https://<your-app-url>/swagger-ui.html` returns the Swagger UI
- [ ] `GET https://<your-app-url>/v3/api-docs` returns OpenAPI JSON
- [ ] `GET https://<your-app-url>/actuator/health` returns `UP`
- [ ] `GET https://<your-app-url>/actuator/health/readiness` returns `UP`

---

## 2. Authentication

- [ ] `POST /api/v1/auth/login` returns a JWT
- [ ] a protected endpoint returns `401` without token
- [ ] a protected endpoint succeeds with a valid token

Suggested protected endpoint:
- `GET /api/v1/orders/me`

---

## 3. Business flow

- [ ] user creation works
- [ ] device creation works (admin)
- [ ] order placement works with `Idempotency-Key`
- [ ] payment confirmation works (admin)
- [ ] outbox-related flow does not break order creation

Suggested endpoints:
- `POST /api/v1/users`
- `POST /api/v1/devices`
- `POST /api/v1/orders`
- payment endpoints under `/api/v1/payments/**`

---

## 4. Operational verification

- [ ] readiness includes database connectivity
- [ ] readiness includes RabbitMQ connectivity
- [ ] custom health remains healthy under normal conditions
- [ ] application logs do not show startup migration failures
- [ ] application logs do not show unresolved environment placeholders

---

## 5. Release notes for README

Once verified, update the README with:
- public base URL
- public Swagger URL
- public health URL
- one or two screenshots from the hosted app