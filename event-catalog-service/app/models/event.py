from datetime import datetime
from sqlalchemy import Column, ForeignKey, Integer, String, DateTime, Numeric
from app.db.connection import Base
from sqlalchemy.orm import relationship


class Event(Base):
    """SQLAlchemy model for the `events` table.
    """

    __tablename__ = "events"
    
    event_id = Column(Integer, primary_key=True, index=True)
    venue_id = Column(Integer, ForeignKey("venues.venue_id"), nullable=False)
    title = Column(String(120), nullable=False)
    event_type = Column(String(50), nullable=False)
    event_date = Column(DateTime, default=datetime.utcnow)
    base_price = Column(Numeric(10, 2), nullable=False)
    status = Column(String(20), nullable=False)

    venue = relationship("Venue", backref="events")
