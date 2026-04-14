#!/bin/bash

# Setup colors
GREEN='\033[0;32m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== E-Fashion RAG Data Setup ===${NC}"

# 1. Install dependencies
echo "Installing Python dependencies..."
pip install mysql-connector-python python-dotenv httpx

# 2. Export data from MySQL
echo "Exporting product catalog from MySQL to Markdown..."
python3 RAG/scripts/export_data.py

# 3. Import data into RAG
echo "Ingesting data into RAG service..."
python3 RAG/scripts/ingest_data.py

echo -e "${GREEN}Done! Knowledge base updated.${NC}"
echo "You can now upload this file to the RAG workspace."
