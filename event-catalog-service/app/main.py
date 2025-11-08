from fastapi import FastAPI
from app.api import routes_venues, routes_events
from app.db.connection import engine
from app.models import venue, event  # ensure models are imported for table creation

app = FastAPI(
    title="Catalog Service",
    version="1.0",
    description="Manages venues and events for the event booking system."
)

# Include routes
app.include_router(routes_venues.router)
app.include_router(routes_events.router)

@app.get("/health")
def health_check():
    return {"status": "Catalog Service is up"}

'''
if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)
'''