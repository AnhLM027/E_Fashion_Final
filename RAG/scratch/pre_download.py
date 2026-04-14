import os
import logging
from sentence_transformers import SentenceTransformer
from docling.document_converter import DocumentConverter, PdfFormatOption
from docling.datamodel.pipeline_options import PdfPipelineOptions

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("pre_download")

def download_models():
    # 1. Download Embedding Model
    model_name = "BAAI/bge-m3"
    logger.info(f"Downloading Embedding model: {model_name}...")
    SentenceTransformer(model_name)
    
    # 2. Download Reranker Model
    reranker_name = "BAAI/bge-reranker-v2-m3"
    logger.info(f"Downloading Reranker model: {reranker_name}...")
    SentenceTransformer(reranker_name)
    
    # 3. Download Docling Models
    logger.info("Downloading Docling models (Layout, OCR, etc.)...")
    pipeline_options = PdfPipelineOptions()
    pipeline_options.generate_picture_images = True
    pipeline_options.do_formula_enrichment = True
    
    # Initializing DocumentConverter triggers the download
    converter = DocumentConverter(
        format_options={
            "pdf": PdfFormatOption(pipeline_options=pipeline_options),
        }
    )
    # Trigger a dummy conversion or just init? 
    # Usually init is enough, but some models download on first convert.
    # We'll just init for now as it's the safest.
    
    logger.info("All models downloaded successfully!")

if __name__ == "__main__":
    download_models()
