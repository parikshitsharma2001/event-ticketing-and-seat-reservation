from app.db.connection import SessionLocal
from app.repository.venue_repo import get_all_venues, get_venue_by_id
from app.repository.event_repo import get_all_events, get_event_by_id

def test_repositories():
    db = SessionLocal()
    print("Connected to DB ✅")

    print("\n--- Venues ---")
    venues = get_all_venues(db)
    print(f"Total venues: {len(venues)}")
    if venues:
        print(f"First venue: {venues[0].name}, {venues[0].city}")

    print("\n--- Events ---")
    events = get_all_events(db)
    print(f"Total events: {len(events)}")
    if events:
        print(f"First event: {events[0].title} ({events[0].event_type})")

    db.close()
    print("\nDatabase session closed.")

if __name__ == "__main__":
    test_repositories()
