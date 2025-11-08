from app.db.connection import Base, engine
from app.models.venue import Venue
from app.models.event import Event
from app.models.seat_template import SeatTemplate

def init_db():
    print("Creating tables in catalog_db ...")
    Base.metadata.create_all(bind=engine)
    print("Tables created successfully!")

if __name__ == "__main__":
    init_db()
