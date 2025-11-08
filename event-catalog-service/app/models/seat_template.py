from sqlalchemy import Column, Integer, String, ForeignKey, Numeric
from sqlalchemy.orm import relationship
from app.db.connection import Base

class SeatTemplate(Base):
    __tablename__ = "seat_templates"

    id = Column(Integer, primary_key=True, index=True)
    event_id = Column(Integer, ForeignKey("events.event_id"))
    category = Column(String(50), nullable=False)
    seat_count = Column(Integer, nullable=False)
    price_modifier = Column(Numeric(5, 2), default=1.0)

    event = relationship("Event", backref="seat_templates")
