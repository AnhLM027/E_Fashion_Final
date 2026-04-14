#!/bin/bash

echo "🚀 Setting up NexusRAG for E-Fashion..."

# 1. Start NexusRAG if not already running
# Check if we are inside NexusRAG or in the root
if [ -d "backend" ] && [ -d "frontend" ]; then
    PROJECT_ROOT="."
else
    PROJECT_ROOT="NexusRAG"
    cd $PROJECT_ROOT
fi

if [ ! -f .env ]; then
    cp .env.example .env
    echo "⚠️ Created .env from .env.example. Please check it!"
fi

echo "📦 Starting NexusRAG Docker containers..."
docker compose up -d

echo "⏳ Waiting for NexusRAG Backend to be ready..."
until curl -s http://localhost:8080/nexus/health | grep -q "healthy"; do
  printf "."
  sleep 2
done
echo "✅ NexusRAG is ready!"

# 2. Run the extraction script
echo "👕 Extracting product data from E-Fashion..."
python3 tasks/extract_efashion.py

echo "🎉 Done! You can now access the NexusRAG UI at http://localhost:3000"
