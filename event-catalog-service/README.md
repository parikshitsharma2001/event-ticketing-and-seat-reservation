# Event Catalog Service (FastAPI + PostgreSQL + Docker)

The **Event Catalog Service** is a core microservice in the *Event Ticketing System* responsible for managing and publishing information about **events**, **venues**, and their **availability**.  
It provides APIs for CRUD operations, search, and event listing for integration with other services such as **Order**, **Seating**, and **Payment**.

---

##  Features

- **CRUD APIs** for managing events and venues  
- **Search & filtering** endpoints (city, event type, status)  
- **Seeded PostgreSQL database** (auto-loaded via CSV files)  
- **REST-based microservice** built with **FastAPI**  
- Fully **containerized** with **Docker** and **Docker Compose**

---

## Tech Stack

| Layer | Technology |
|-------|-------------|
| Framework | FastAPI (Python 3.11) |
| Database | PostgreSQL 16 (Dockerized) |
| ORM | SQLAlchemy |
| Validation | Pydantic v2 |
| Containerization | Docker & Docker Compose |
| Seeding | CSV via Python script |
| Deployment (optional) | Minikube (Kubernetes) |

---

## Getting Started

### 1️⃣ Clone the repository
```bash
https://github.com/vig-dev/event-catalog-service
cd event-catalog-service
```
### 2️⃣ Setup Environment

Create a .env file in the project root:
```bash

DATABASE_URL=postgresql://catalog_user:password@catalog-db:5432/catalog_db
```
### 3️⃣ Build and Run Containers

Build and start all services (FastAPI + PostgreSQL):

```bash
docker-compose up --build
```

✅ Once started, you’ll see logs similar to:

catalog-db      | database system is ready to accept connections
catalog-service | ✅ Database connection established.
catalog-service | ✅ Loaded 100 records into venues
catalog-service | ✅ Loaded 300 records into events
catalog-service | INFO:     Uvicorn running on http://0.0.0.0:8000


Access the API documentation at:
👉 http://127.0.0.1:8000/docs

###  4️⃣ Verify Seed Data

Open the Swagger UI and test:

GET /v1/venues → Should return a list of seeded venues

GET /v1/events → Should return seeded events

### 5️⃣ Common Docker Commands
| Purpose | Command |
|:-------|:----------|
|Rebuild and restart all containers | docker-compose up --build |
| Run in background (detached) | docker-compose up -d --build |
| View logs | docker-compose logs -f |
| Stop containers | docker-compose down |
| Reset database and reseed data | docker-compose down -v && docker-compose up --build |

### 6️⃣ Project URL Summary
| Component | URL |
| FastAPI Swagger Docs | http://127.0.0.1:8000/docs |
|FastAPI JSON OpenAPI Spec | http://127.0.0.1:8000/openapi.json |
| PostgreSQL (container internal) | postgresql://catalog_user:password@catalog-db:5432/catalog_db |

### 7️⃣ Stop Everything

When done, stop and clean up containers:
```bash
docker-compose down
```

To also delete the database volume (reset completely):
```bash
docker-compose down -v
```

##  API Endpoints

---

###  Venues API

| Method | Endpoint | Description |
|:-------|:----------|:-------------|
| **GET** | `/v1/venues` | Retrieve all venues |
| **GET** | `/v1/venues/{venue_id}` | Retrieve details of a specific venue |
| **POST** | `/v1/venues` | Create a new venue |
| **DELETE** | `/v1/venues/{venue_id}` | Delete a venue by ID |

####  Example Request — Create Venue
**POST** `/v1/venues`

```json
{
  "name": "Symphony Hall",
  "city": "Pune",
  "capacity": 600
}
```
###  Example Response

201 Created
```json
{
  "venue_id": 21,
  "name": "Symphony Hall",
  "city": "Pune",
  "capacity": 600
}
```
### Events API

| Method | Endpoint | Description |
|:-------|:----------|:-------------|
| **GET** | `/v1/events` | Retrieve all events |
| **GET** | `/v1/events/{event_id}` | Retrieve details of a specific event |
| **POST** | `/v1/events` | Create a new event |
| **DELETE** | `/v1/events/{event_id}` | Delete a event by ID |
| **GET** | `/v1/events/search?city=&event_type=&status=` | Search and filter events by city, event type, or status |

#### Example Request

GET

/v1/events/search?city=Pune&event_type=Concert&status=ON_SALE

#### Example Response

200 OK
```json
[
  {
    "event_id": 5,
    "venue_id": 2,
    "title": "Rock Night",
    "event_type": "Concert",
    "event_date": "2025-11-25T19:30:00",
    "base_price": 899.99,
    "status": "ON_SALE"
  },
  {
    "event_id": 8,
    "venue_id": 3,
    "title": "Jazz Under the Stars",
    "event_type": "Concert",
    "event_date": "2025-12-01T20:00:00",
    "base_price": 1250.00,
    "status": "ON_SALE"
  }
]
```