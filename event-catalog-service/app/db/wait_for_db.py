import time
import psycopg2
import os
from psycopg2 import OperationalError

DB_URL = os.getenv("DATABASE_URL")

while True:
    try:
        conn = psycopg2.connect(DB_URL)
        conn.close()
        print("✅ Database is ready!")
        break
    except OperationalError:
        print("⏳ Waiting for database to be ready...")
        time.sleep(3)
