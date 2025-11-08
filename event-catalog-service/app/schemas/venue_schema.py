from pydantic import BaseModel

class VenueBase(BaseModel):
    name: str
    city: str
    capacity: int

class VenueCreate(VenueBase):
    pass

class VenueResponse(VenueBase):
    venue_id: int

    model_config = {"from_attributes": True}
