import time
import logging
from fastapi import Request
from starlette.middleware.base import BaseHTTPMiddleware
from app.core.logging import logger

class LoggingMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        start_time = time.time()
        
        # Log request basic info
        method = request.method
        path = request.url.path
        client_ip = request.client.host if request.client else "unknown"
        
        logger.info(f"Incoming request: {method} {path} - IP: {client_ip}")
        
        try:
            response = await call_next(request)
            
            process_time = (time.time() - start_time) * 1000
            formatted_process_time = "{0:.2f}".format(process_time)
            
            logger.info(
                f"Completed: {method} {path} - Status: {response.status_code} - Time: {formatted_process_time}ms"
            )
            
            return response
        except Exception as e:
            process_time = (time.time() - start_time) * 1000
            formatted_process_time = "{0:.2f}".format(process_time)
            logger.error(
                f"Failed: {method} {path} - Error: {str(e)} - Time: {formatted_process_time}ms"
            )
            raise e
