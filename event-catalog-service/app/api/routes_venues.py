from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.db.connection import get_db
from app.repository.venue_repo import (
    get_all_venues,
    get_venue_by_id,
    create_venue,
    delete_venue,
)
from app.schemas.venue_schema import VenueCreate, VenueResponse
from typing import List

router = APIRouter(
    prefix="/v1/venues",
    tags=["Venues"]
)

# 🔹 Get all venues
@router.get("/", response_model=List[VenueResponse])
def read_all_venues(db: Session = Depends(get_db)):
    return get_all_venues(db)

# 🔹 Get venue by ID
@router.get("/{venue_id}", response_model=VenueResponse)
def read_venue(venue_id: int, db: Session = Depends(get_db)):
    venue = get_venue_by_id(db, venue_id)
    if not venue:
        raise HTTPException(status_code=404, detail="Venue not found")
    return venue

# 🔹 Create a new venue
@router.post("/", response_model=VenueResponse, status_code=status.HTTP_201_CREATED)
def create_new_venue(venue: VenueCreate, db: Session = Depends(get_db)):
    return create_venue(db, venue)

# 🔹 Delete a venue
@router.delete("/{venue_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_existing_venue(venue_id: int, db: Session = Depends(get_db)):
    deleted = delete_venue(db, venue_id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Venue not found")
    return None
