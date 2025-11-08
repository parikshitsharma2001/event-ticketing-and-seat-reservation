from app.db.connection import Base, engine
from app.models.venue import Venue
from app.models.event import Event
'''from app.models.seat_template import SeatTemplate'''

print("Creating tables...")
Base.metadata.create_all(bind=engine)
print("Done ✅")
