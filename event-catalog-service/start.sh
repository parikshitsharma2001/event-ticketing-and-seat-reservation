#!/bin/sh
set -e  # stop on first unrecoverable error

echo "🕒 Waiting for database..."
python -m app.db.wait_for_db

echo "🌱 Running seed data (will continue even if it fails)..."
python -m app.db.seed_data || echo "⚠️ Seed data failed, continuing..."

echo "🚀 Starting FastAPI with Uvicorn..."
exec uvicorn app.main:app --host 0.0.0.0 --port 8000
