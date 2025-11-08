from fastapi import FastAPI, Response
from app.api import routes_venues, routes_events
from app.db.connection import engine
from app.models import venue, event  # ensure models are imported for table creation
from prometheus_client import Counter, Histogram, generate_latest, CONTENT_TYPE_LATEST
import time

app = FastAPI(
    title="Catalog Service",
    version="1.0",
    description="Manages venues and events for the event booking system."
)

# Define metrics
REQUEST_COUNT = Counter('http_requests_total', 'Total number of HTTP requests', ['method', 'endpoint', 'http_status'])
REQUEST_LATENCY = Histogram('http_request_duration_seconds', 'Request latency in seconds', ['endpoint'])


# Include routes
app.include_router(routes_venues.router)
app.include_router(routes_events.router)

@app.get("/health")
def health_check():
    return {"status": "Catalog Service is up"}


@app.middleware("http")
async def add_metrics(request, call_next):
    start = time.time()
    response = await call_next(request)
    latency = time.time() - start
    REQUEST_COUNT.labels(request.method, request.url.path, response.status_code).inc()
    REQUEST_LATENCY.labels(request.url.path).observe(latency)
    return response

@app.get("/metrics")
def metrics():
    return Response(generate_latest(), media_type=CONTENT_TYPE_LATEST)



'''
if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)
'''