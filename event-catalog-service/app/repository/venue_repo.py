from sqlalchemy.orm import Session
from app.models.venue import Venue

# 🔹 Get all venues
def get_all_venues(db: Session):
    return db.query(Venue).all()

# 🔹 Get venue by ID
def get_venue_by_id(db: Session, venue_id: int):
    return db.query(Venue).filter(Venue.venue_id == venue_id).first()

# 🔹 Add a new venue
def create_venue(db: Session, venue_data):
    new_venue = Venue(**venue_data.dict())
    db.add(new_venue)
    db.commit()
    db.refresh(new_venue)
    return new_venue

# 🔹 Delete venue
def delete_venue(db: Session, venue_id: int):
    venue = db.query(Venue).filter(Venue.venue_id == venue_id).first()
    if venue:
        db.delete(venue)
        db.commit()
        return True
    return False
