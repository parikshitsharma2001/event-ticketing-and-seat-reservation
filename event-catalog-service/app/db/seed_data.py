import pandas as pd
from sqlalchemy import create_engine
from dotenv import load_dotenv
import os
from psycopg2 import OperationalError
import time

# Load .env variables
load_dotenv()
DATABASE_URL = os.getenv("DATABASE_URL")

# Wait until DB is ready
engine = None
for _ in range(10):
    try:
        engine = create_engine(DATABASE_URL)
        with engine.connect() as conn:
            print("✅ Database connection established.")
            break
    except OperationalError:
        print("⏳ Waiting for database to be ready...")
        time.sleep(3)

# Base path for seed files
BASE_DIR = os.path.dirname(os.path.dirname(__file__))  # /app/app
DATA_DIR = os.path.join(BASE_DIR, "etsr_seed_dataset")  # /app/app/etsr_seed_dataset

def load_csv_to_db(filename, table_name):
    csv_path = os.path.join(DATA_DIR, filename)
    print(f"Loading {filename} → {table_name}")
    df = pd.read_csv(csv_path)
    df.to_sql(table_name, con=engine, if_exists="append", index=False)
    print(f"✅ Loaded {len(df)} records into {table_name}")

def main():
    load_csv_to_db("etsr_venues.csv", "venues")
    load_csv_to_db("etsr_events.csv", "events")

if __name__ == "__main__":
    main()
