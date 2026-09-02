# Admin Backend API Guide

## Connection configuration

Local backend URL:

```text
http://localhost:8082
```

All application APIs start with:

```text
http://localhost:8082/api
```

The backend exchanges JSON and uses **snake_case** property names. For example,
Java's `displayOrder` is sent as `display_order`.

No authentication is currently configured. The frontend does not need to send
an authorization header.

## Frontend environment

For a Vite frontend, create or update `.env.development`:

```env
VITE_API_BASE_URL=http://localhost:8082/api
```

Restart the Vite development server after changing an environment file.

Create a shared Axios client:

```javascript
// src/api/client.js
import axios from "axios";

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
    Accept: "application/json",
  },
});
```

Equivalent fetch configuration:

```javascript
const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8082/api";

export async function apiFetch(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
      ...options.headers,
    },
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.message ?? `Request failed: ${response.status}`);
  }

  return response.status === 204 ? null : response.json();
}
```

## Common CRUD operations

Every master controller uses the same HTTP contract:

| Operation | Method | URL | Success |
|---|---|---|---|
| List | GET | `/api/{master}` | `200` and JSON array |
| Get one | GET | `/api/{master}/{id}` | `200` and JSON object |
| Create | POST | `/api/{master}` | `201`, JSON object, and `Location` header |
| Update | PUT | `/api/{master}/{id}` | `200` and updated JSON object |
| Delete | DELETE | `/api/{master}/{id}` | `204`, no body |

`id`, `created_at`, and `updated_at` are server-managed fields and should
not be sent by the frontend.

`PUT` request bodies should contain all editable fields. Omitting an optional
field sets it to `null`; omitting a primitive wrapper with a model default can
restore that default.

## Controller-wise API list

### FieldController

Base path: `/api/fields`

Editable request body:

```json
{
  "code": "TECH",
  "name": "Technology",
  "description": "Technology content",
  "display_order": 1,
  "active": true,
  "icon_name": "cpu"
}
```

Required: `code`, `name`, `active`.

### CategoryController

Base path: `/api/categories`

```json
{
  "name": "Backend",
  "field_id": 1,
  "parent_id": null,
  "has_children": false,
  "level": 1,
  "display_tag": "backend",
  "active": true
}
```

Required: `name`, `field_id`, `level`.

### TechnologyController

Base path: `/api/technologies`

```json
{
  "name": "Spring Boot",
  "description": "Java application framework",
  "category_id": 1,
  "field_id": 1
}
```

Required: `name`.

### TechSourceController

Base path: `/api/tech-sources`

```json
{
  "technology_id": 1,
  "name": "Spring Blog",
  "url": "https://spring.io/blog",
  "type": "RSS",
  "active": true,
  "poll_interval_seconds": 3600,
  "last_polled_at": null,
  "segment_type": "NEWS",
  "parser_type": "RSS",
  "status": "ACTIVE",
  "last_success_at": null,
  "last_failure_at": null,
  "failure_reason": null,
  "retry_count": 0,
  "priority_score": 1,
  "next_fetch_at": null,
  "fetch_interval_minutes": 60
}
```

All fields are nullable according to the current table definition.

### TechUpdateController

Base path: `/api/tech-updates`

```json
{
  "tech_id": 1,
  "title": "Spring Boot release",
  "content": "Release details",
  "source_url": "https://spring.io/blog/example",
  "source": "Spring Blog",
  "pulled_at": "2026-08-24T12:00:00"
}
```

Required: `tech_id`, `title`.

### MedicalCategoryController

Base path: `/api/medical-categories`

```json
{
  "name": "Cardiology",
  "description": "Heart-related medicine",
  "display_order": 1,
  "is_active": true
}
```

Required: `name`, `display_order`, `is_active`.

### MedicalSubjectController

Base path: `/api/medical-subjects`

```json
{
  "category_id": 1,
  "name": "Heart Failure",
  "description": "Heart failure updates",
  "display_order": 1,
  "is_active": true
}
```

Required: `category_id`, `name`, `display_order`, `is_active`.

### MedicalSourceController

Base path: `/api/medical-sources`

```json
{
  "subject_id": 1,
  "name": "Medical Journal",
  "url": "https://example.com/medical-feed",
  "type": "RSS",
  "active": true,
  "poll_interval_seconds": 3600,
  "last_polled_at": null,
  "segment_type": "JOURNAL",
  "parser_type": "RSS",
  "status": "ACTIVE",
  "last_success_at": null,
  "last_failure_at": null,
  "failure_reason": null,
  "retry_count": 0,
  "priority_score": 0,
  "next_fetch_at": null,
  "fetch_interval_minutes": 60
}
```

Required: `subject_id`, `name`, `url`, `type`.

### MedicalUpdateController

Base path: `/api/medical-updates`

```json
{
  "subject_id": 1,
  "title": "New clinical guidance",
  "content": "Guidance details",
  "source_url": "https://example.com/guidance",
  "source": "Medical Journal",
  "pulled_at": "2026-08-24T12:00:00"
}
```

Required: `subject_id`, `title`.

### UserTypeController

Base path: `/api/user-types`

```json
{
  "field_id": 1,
  "code": "DEVELOPER",
  "name": "Developer",
  "display_order": 1,
  "active": true,
  "profile_type": "TECHNICAL"
}
```

Required: `field_id`, `code`, `name`.

### ProfileTypeController

Base path: `/api/profile-types`

```json
{
  "code": "TECHNICAL",
  "name": "Technical Profile",
  "description": "Profile for technical users",
  "display_order": 1,
  "active": true
}
```

Required: `code`, `name`, `active`.

## Phase 1 location and language masters

Run these PostgreSQL scripts in order (both are safe to rerun):

1. `src/main/resources/db/phase1-master-schema.sql`
2. `src/main/resources/db/phase1-master-seed.sql`

The seed contains all Indian states/union territories and scheduled languages,
plus representative districts and cities for initial testing. A complete,
production-approved locality dataset can be loaded later using the same natural
keys. Parent records cannot be deleted while child records refer to them.

All five controllers support the common CRUD operations documented above.

| Master | Base path | Optional list filter |
|---|---|---|
| Country | `/api/countries` | — |
| State | `/api/states` | `?country_id={id}` |
| District | `/api/districts` | `?state_id={id}` |
| City | `/api/cities` | `?district_id={id}` |
| Language | `/api/languages` | — |

Example editable request bodies (shown as JSON with labels for brevity):

```jsonc
// Country
{"code":"IND","name":"India","phone_code":"+91","display_order":1,"active":true}

// State
{"country_id":1,"code":"KL","name":"Kerala","display_order":1,"active":true}

// District
{"state_id":1,"code":"EKM","name":"Ernakulam","display_order":1,"active":true}

// City
{"district_id":1,"code":"KOC","name":"Kochi","postal_code":"682001","display_order":1,"active":true}

// Language
{"code":"ml","name":"Malayalam","native_name":"മലയാളം","display_order":1,"active":true}
```

For dependent frontend controls, load countries first, then call the filtered
state, district, and city endpoints as each parent selection changes.

## Phase 2 maintenance masters

Run these PostgreSQL scripts in order. Both are safe to rerun:

1. `src/main/resources/db/phase2-master-schema.sql`
2. `src/main/resources/db/phase2-master-seed.sql`

| Master | Base path |
|---|---|
| Event Delivery Mode | `/api/event-delivery-modes` |
| Training Delivery Mode | `/api/training-delivery-modes` |
| Event Type | `/api/event-types` |
| Training Type | `/api/training-types` |
| Opportunity Type | `/api/opportunity-types` |
| Organization Type | `/api/organization-types` |

Every Phase 2 master supports the common list, get, create, update, and delete
operations documented at the start of this guide. All use the same editable body:

```json
{
  "code": "ONLINE",
  "name": "Online",
  "description": "Delivered online",
  "display_order": 1,
  "active": true
}
```

Required fields are `code`, `name`, and `active`. The `description` field is
optional. The server manages `id`, `created_at`, and `updated_at`.

### DashboardController

The dashboard is read-only:

```http
GET /api/dashboard/counts
```

It returns record counts keyed by master name:

```json
{
  "fields": 2,
  "categories": 5,
  "technologies": 10,
  "tech_sources": 4,
  "tech_updates": 20,
  "medical_categories": 3,
  "medical_subjects": 8,
  "medical_sources": 5,
  "medical_updates": 15,
  "user_types": 4,
  "profile_types": 3
}
```

## Frontend call examples

```javascript
import { api } from "./client";

// List
const { data: profileTypes } = await api.get("/profile-types");

// Get by ID
const { data: profileType } = await api.get("/profile-types/1");

// Create
const { data: created } = await api.post("/profile-types", {
  code: "TECHNICAL",
  name: "Technical Profile",
  description: "Profile for technical users",
  display_order: 1,
  active: true,
});

// Update (send the complete editable object)
const { data: updated } = await api.put(`/profile-types/${created.id}`, {
  code: "TECHNICAL",
  name: "Updated Technical Profile",
  description: "Updated description",
  display_order: 1,
  active: true,
});

// Delete
await api.delete(`/profile-types/${created.id}`);
```

## Topic images

Create the topic first, then upload or replace its image using the same endpoint:

```http
POST /api/technologies/{id}/image
POST /api/medical-subjects/{id}/image
Content-Type: multipart/form-data
```

The multipart field name is `file`. JPEG, PNG, and WebP files up to 2 MB are
accepted. `PUT` is also supported for compatibility with older Admin App code.
The backend validates the decoded dimensions, generates WebP variants, uploads
them to Azure, and generates the blob keys. The frontend must not send or
construct a blob key or use Azure credentials.

```text
master/topics/{topicType}/{topicId}/{version}/thumb.webp
master/topics/{topicType}/{topicId}/{version}/small.webp
master/topics/{topicType}/{topicId}/{version}/medium.webp
```

`topicType` is `technology` or `medical-subject`. PostgreSQL stores only the
preferred `small.webp` key in `image_key`; `image_url` is transient API output
derived from `MEDIA_PUBLIC_BASE_URL`. Replacing or deleting a topic removes its
old Azure variants only after the database transaction commits.

Set all of these before starting the application:

```text
AZURE_STORAGE_CONNECTION_STRING=<secret connection string>
AZURE_STORAGE_CONTAINER=public-media
MEDIA_PUBLIC_BASE_URL=https://<account>.blob.core.windows.net/public-media
```

The container must already exist and allow public read access, or the public
base URL must be a CDN endpoint. Upload and delete access belongs only to the
backend connection string. Rotate any Azure storage key that was previously
committed to configuration.

Legacy local keys (`master/topics/{fieldId}/...`) are not uploaded automatically.
Re-upload those images through the topic image endpoints (or migrate and verify
them separately) before removing the local media backup.

## Dates

Send timestamps without a timezone in ISO local-date-time format:

```text
2026-08-24T15:30:00
```

## Error responses

Errors use this structure:

```json
{
  "timestamp": "2026-08-24T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "name: must not be blank"
}
```

Important status codes:

| Status | Meaning |
|---|---|
| `400` | Request validation failed |
| `404` | Requested ID does not exist |
| `409` | Unique constraint or foreign-key constraint failed |

## CORS configuration

The backend currently allows `http://localhost:5173`.

To use another frontend origin, set the environment variable before starting
the backend:

PowerShell:

```powershell
$env:APP_CORS_ALLOWED_ORIGINS="http://localhost:3000"
mvn spring-boot:run
```

For a deployed frontend:

```text
APP_CORS_ALLOWED_ORIGINS=https://admin.example.com
```

The value must be an origin only: scheme, host, and optional port. Do not add
`/api` or a trailing path.

## Phase 2 maintenance masters

Run these idempotent PostgreSQL scripts in order:

1. `src/main/resources/db/phase2-master-schema.sql`
2. `src/main/resources/db/phase2-master-seed.sql`

| Master | Base path |
|---|---|
| Event Delivery Mode | `/api/event-delivery-modes` |
| Training Delivery Mode | `/api/training-delivery-modes` |
| Event Type | `/api/event-types` |
| Training Type | `/api/training-types` |
| Opportunity Type | `/api/opportunity-types` |
| Organization Type | `/api/organization-types` |

Every endpoint supports `GET /`, `GET /{id}`, `POST /`, `PUT /{id}`, and
`DELETE /{id}`. Create and update requests use this shape:

```json
{"code":"ONLINE","name":"Online","description":"Delivered online","display_order":1,"active":true}
```

Required fields are `code`, `name`, and `active`. IDs and timestamps are
server-managed. Codes are unique within each master.
