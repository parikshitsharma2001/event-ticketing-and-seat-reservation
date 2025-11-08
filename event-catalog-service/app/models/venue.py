from sqlalchemy import Column, Integer, String
from app.db.connection import Base

class Venue(Base):
    __tablename__ = "venues"

    venue_id = Column(Integer, primary_key=True, index=True)
    name = Column(String(100), nullable=False)
    city = Column(String(50), nullable=False)
    capacity = Column(Integer, nullable=False)
