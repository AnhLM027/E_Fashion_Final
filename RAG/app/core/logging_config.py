import logging
import os
import sys
from logging.handlers import TimedRotatingFileHandler
from pathlib import Path

def setup_logging():
    """ Setup professional logging with daily rotation. """
    from app.core.config import settings
    
    # 1. Ensure log directory exists
    log_dir = Path(settings.LOG_DIR)
    log_dir.mkdir(parents=True, exist_ok=True)
    
    log_file = log_dir / "app.log"
    log_level = getattr(logging, settings.LOG_LEVEL.upper(), logging.INFO)
    
    # 2. Create formatters
    # Professional format: Timestamp - Name - Level - [File:Line] - Message
    log_format = logging.Formatter(
        "%(asctime)s - %(name)s - %(levelname)s - [%(filename)s:%(lineno)d] - %(message)s"
    )
    
    # 3. Create handlers
    
    # A. Console Handler
    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setFormatter(log_format)
    
    # B. Daily Rotating File Handler (Rotation: midnight, Backup: 30 days)
    file_handler = TimedRotatingFileHandler(
        filename=str(log_file),
        when="midnight",
        interval=1,
        backupCount=30,
        encoding="utf-8"
    )
    file_handler.setFormatter(log_format)
    
    # 4. Global Configuration
    logging.root.setLevel(log_level)
    
    # Clear existing handlers
    logging.root.handlers = []
    
    # Add new handlers
    logging.root.addHandler(console_handler)
    logging.root.addHandler(file_handler)
    
    # 5. Redirect specific noisy loggers
    logging.getLogger("uvicorn").handlers = logging.root.handlers
    logging.getLogger("uvicorn.access").handlers = logging.root.handlers
    logging.getLogger("lightrag").setLevel(logging.WARNING) # Silence LightRAG internal spam unless warning
    
    logging.info(f"Logging initialized. Level: {settings.LOG_LEVEL}, Dir: {settings.LOG_DIR}")
