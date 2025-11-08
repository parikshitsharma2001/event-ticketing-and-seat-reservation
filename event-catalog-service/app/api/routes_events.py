from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing import List
from app.db.connection import get_db
from app.repository.event_repo import (
    get_all_events,
    get_event_by_id,
    create_event,
    delete_event,
    search_events
)
from app.schemas.event_schema import EventCreate, EventResponse

router = APIRouter(
    prefix="/v1/events",
    tags=["Events"]
)

# 🔹 Get all events
@router.get("/", response_model=List[EventResponse])
def read_all_events(db: Session = Depends(get_db)):
    return get_all_events(db)

# 🔹 Search events
@router.get("/search", response_model=List[EventResponse])
def search_event_listings(
    city: str | None = None,
    event_type: str | None = None,
    status: str | None = None,
    db: Session = Depends(get_db)
):
    results = search_events(db, city, event_type, status)
    if not results:
        raise HTTPException(status_code=404, detail="No matching events found")
    return results

# 🔹 Get event by ID
@router.get("/{event_id}", response_model=EventResponse)
def read_event(event_id: int, db: Session = Depends(get_db)):
    event = get_event_by_id(db, event_id)
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")
    return event

# 🔹 Create new event
@router.post("/", response_model=EventResponse, status_code=status.HTTP_201_CREATED)
def create_new_event(event: EventCreate, db: Session = Depends(get_db)):
    return create_event(db, event)

# 🔹 Delete event
@router.delete("/{event_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_existing_event(event_id: int, db: Session = Depends(get_db)):
    deleted = delete_event(db, event_id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Event not found")
    return None


