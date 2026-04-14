import logging
import sys
import os
from logging.handlers import RotatingFileHandler
from datetime import datetime
from pathlib import Path

# --- Configuration ---
LOGS_DIR = Path("logs")
LOGS_DIR.mkdir(parents=True, exist_ok=True)

DEFAULT_LOG_FORMAT = "%(asctime)s - %(name)s - %(levelname)s - %(message)s"
DATE_FORMAT = "%Y-%m-%d %H:%M:%S"

class InterceptHandler(logging.Handler):
    """
    Optional: Intercept standard logging messages and redirect them.
    Can be used with loguru if you decide to upgrade later.
    """
    def emit(self, record):
        try:
            level = logger.level(record.levelname).name
        except ValueError:
            level = record.levelno

        frame, depth = logging.currentframe(), 2
        while frame.f_code.co_filename == logging.__file__:
            frame = frame.f_back
            depth += 1

        logger.opt(depth=depth, exception=record.exc_info).log(level, record.getMessage())

def setup_logging():
    """
    Configures logging for the entire application.
    Includes console and rotating file handlers.
    """
    # Create logger for the application
    app_logger = logging.getLogger("app")
    app_logger.setLevel(logging.INFO)
    
    # Avoid duplicate logs if setup is called multiple times
    if app_logger.handlers:
        return app_logger

    formatter = logging.Formatter(DEFAULT_LOG_FORMAT, DATE_FORMAT)

    # 1. Console Handler
    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setFormatter(formatter)
    app_logger.addHandler(console_handler)

    # 2. Daily/Rotating File Handler for general logs
    general_log_file = LOGS_DIR / "app.log"
    file_handler = RotatingFileHandler(
        general_log_file, 
        maxBytes=10*1024*1024, # 10MB
        backupCount=5,
        encoding="utf-8"
    )
    file_handler.setFormatter(formatter)
    app_logger.addHandler(file_handler)

    # 3. Dedicated Error Log File
    error_log_file = LOGS_DIR / "error.log"
    error_handler = RotatingFileHandler(
        error_log_file,
        maxBytes=5*1024*1024, # 5MB
        backupCount=5,
        encoding="utf-8"
    )
    error_handler.setLevel(logging.ERROR)
    error_handler.setFormatter(formatter)
    app_logger.addHandler(error_handler)

    # Configure root logger to some extent if needed
    # logging.basicConfig(handlers=[console_handler, file_handler], level=logging.INFO)

    return app_logger

# Initialize the application logger
logger = setup_logging()
