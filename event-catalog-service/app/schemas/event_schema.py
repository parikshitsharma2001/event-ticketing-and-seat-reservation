from pydantic import BaseModel
from datetime import datetime

class EventBase(BaseModel):
    venue_id: int
    title: str
    event_type: str
    event_date: datetime
    base_price: float
    status: str

class EventCreate(EventBase):
    pass

class EventResponse(EventBase):
    event_id: int

    model_config = {"from_attributes": True}
