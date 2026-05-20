# Demo property (Skyline Business Tower)

Ready-to-post JSON for **`POST /api/admin/properties`**. It creates a **public, featured, OPEN** listing with two gallery images (URLs you provided).

## Prerequisites

- Backend running (default e.g. `http://localhost:8080`).
- Admin account; obtain a JWT with **`POST /api/admin/auth/login`** (not the investor token).

## Create the property

From the repo root (or adjust paths):

```bash
export BASE_URL=http://localhost:8080
export ADMIN_TOKEN='<paste accessToken from admin login>'

curl -sS -X POST "$BASE_URL/api/admin/properties" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  --data-binary @"Minilands-backend/scripts/demo-property/create-demo-property.json"
```

You should get **201** and a JSON body with `id`, `slug`, and `media` including both image URLs.

## Verify (investor API, no admin token)

```bash
curl -sS "$BASE_URL/api/properties/slug/demo-skyline-business-tower"
```

Or list/search:

```bash
curl -sS "$BASE_URL/api/properties/search?sortBy=newest&page=0&size=10"
```

## Notes

- **`annualRoi`** and similar yield fields are stored as **percent numbers** (e.g. `10.5` = 10.5% p.a.), consistent with backend valuation math (`annualRoi / 100 / 365` for daily growth).
- **`reraRegistrationId`** is a placeholder; replace for any real listing.
- External image hosts may block hotlinking or change URLs; for production use your own CDN or uploads.
