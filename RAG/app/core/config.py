import os
from pydantic_settings import BaseSettings
from pydantic import Field
from pathlib import Path

class Settings(BaseSettings):
    APP_NAME: str = "MBA_RAG_Hybrid"
    DEBUG: bool = True
    LOG_LEVEL: str = Field(default="INFO")
    LOG_DIR: str = Field(default="logs")
    
    # Storage
    DATA_ROOT: str = Field(default="/app/data_mba_large")
    
    @property
    def DATA_BASE_PATH(self) -> Path:
        return Path(self.DATA_ROOT) / "data"

    # Database
    DATABASE_URL: str = Field(default="postgresql+asyncpg://postgres:postgres@localhost:5432/e_fashion_rag")

    # Qdrant Database
    QDRANT_HOST: str = Field(default="localhost")
    QDRANT_PORT: int = Field(default=6333)
    QDRANT_API_KEY: str | None = Field(default=None)

    # MongoDB
    MONGO_URI: str = Field(default="mongodb://localhost:27017")
    MONGO_DB_NAME: str = Field(default="MBA")

    # Neo4j
    NEO4J_URI: str = Field(default="bolt://localhost:7687")
    NEO4J_USER: str = Field(default="neo4j")
    NEO4J_PASSWORD: str = Field(default="password123")

    # LLM Settings
    OPENAI_API_KEY: str = Field(default="")
    OPENAI_API_BASE: str | None = Field(default=None)
    GOOGLE_AI_API_KEY: str = Field(default="")
    LLM_PROVIDER: str = Field(default="openai") # openai, gemini, ollama
    LLM_MODEL: str = Field(default="gpt-4o-mini")
    LLM_MODEL_FAST: str = Field(default="gemini-1.5-flash")
    LLM_THINKING_LEVEL: str = Field(default="low")
    LLM_MAX_TOKENS: int = Field(default=4096)
    LLM_MAX_ASYNC: int = Field(default=4)
    EMBEDDING_MODEL: str = Field(default="text-embedding-3-large")

    # Ollama Settings
    OLLAMA_HOST: str = Field(default="http://localhost:11434")
    OLLAMA_MODEL: str = Field(default="llama3")

    # KG Embedding Settings
    KG_EMBEDDING_PROVIDER: str = Field(default="gemini")
    KG_EMBEDDING_MODEL: str = Field(default="gemini-embedding-001")
    KG_EMBEDDING_DIMENSION: int = Field(default=3072)

    # NexusRAG Pipeline Settings
    NEXUSRAG_CHUNK_MAX_TOKENS: int = 512
    NEXUSRAG_DOCUMENT_PARSER: str = "docling"
    NEXUSRAG_EMBEDDING_MODEL: str = "BAAI/bge-m3"
    NEXUSRAG_RERANKER_MODEL: str = "BAAI/bge-reranker-v2-m3"
    NEXUSRAG_MIN_RELEVANCE_SCORE: float = 0.5
    NEXUSRAG_RERANKER_TOP_K: int = 10
    NEXUSRAG_VECTOR_PREFETCH: int = 30
    
    # Docling specific settings
    NEXUSRAG_ENABLE_IMAGE_EXTRACTION: bool = True
    NEXUSRAG_DOCLING_IMAGES_SCALE: float = 2.0
    NEXUSRAG_ENABLE_FORMULA_ENRICHMENT: bool = True
    NEXUSRAG_ENABLE_TABLE_CAPTIONING: bool = True
    NEXUSRAG_ENABLE_IMAGE_CAPTIONING: bool = True
    NEXUSRAG_MAX_IMAGES_PER_DOC: int = 20
    NEXUSRAG_MAX_TABLE_MARKDOWN_CHARS: int = 10000
    
    # Deduplication settings
    NEXUSRAG_DEDUP_ENABLED: bool = True
    NEXUSRAG_DEDUP_MIN_CHUNK_LENGTH: int = 50
    NEXUSRAG_DEDUP_NEAR_THRESHOLD: float = 0.85
    NEXUSRAG_CHUNK_DEDUP_THRESHOLD: float = 0.95

    # Knowledge Graph Settings (LightRAG)
    NEXUSRAG_ENABLE_KG: bool = Field(default=True)
    NEXUSRAG_KG_LANGUAGE: str = "Vietnamese"
    NEXUSRAG_KG_ENTITY_TYPES: list[str] = [
        "Organization", "Person", "Product", "Location", "Event",
        "Financial_Metric", "Technology", "Date", "Regulation",
        "Document", "Concept", "Course"
    ]
    NEXUSRAG_KG_CHUNK_TOKEN_SIZE: int = 1200
    NEXUSRAG_KG_QUERY_TIMEOUT: float = 30.0

    # Static settings
    BASE_DIR: Path = Path(__file__).resolve().parent.parent.parent

    model_config = {
        "env_file": ".env",
        "extra": "ignore"
    }

    def __init__(self, **values):
        super().__init__(**values)
        self._load_yaml_config()

    def _load_yaml_config(self):
        """Override settings with values from llm_settings.yaml dynamically."""
        yaml_path = self.BASE_DIR / "llm_settings.yaml"
        if not yaml_path.exists():
            return

        try:
            import yaml
            with open(yaml_path, "r", encoding="utf-8") as f:
                config = yaml.safe_load(f)
            
            active_profile_name = config.get("active_profile")
            profile = config.get("profiles", {}).get(active_profile_name)
            
            if profile:
                print(f"--- [DYNAMIC CONFIG] Loading Profile: {active_profile_name} ---")
                
                # Dynamic mapping
                for key, value in profile.items():
                    setting_key = key.upper()
                    # Try direct match
                    if hasattr(self, setting_key):
                        setattr(self, setting_key, value)
                    # Try with LLM_ prefix
                    elif hasattr(self, f"LLM_{setting_key}"):
                        setattr(self, f"LLM_{setting_key}", value)
                    # Try with KG_ prefix
                    elif hasattr(self, f"KG_{setting_key}"):
                        setattr(self, f"KG_{setting_key}", value)

                # Special mapping for api_key
                if "api_key" in profile:
                    apikey = profile["api_key"]
                    if self.LLM_PROVIDER == "gemini":
                        self.GOOGLE_AI_API_KEY = apikey
                    elif self.LLM_PROVIDER == "openai":
                        self.OPENAI_API_KEY = apikey

            # 2. Pipeline Settings (Global/Pipeline defaults)
            pipeline = config.get("pipeline", {})
            if pipeline:
                for key, value in pipeline.items():
                    # Map to NEXUSRAG_ prefix
                    setting_key = f"NEXUSRAG_{key.upper()}"
                    if hasattr(self, setting_key):
                        setattr(self, setting_key, value)
                    # Try direct match
                    elif hasattr(self, key.upper()):
                        setattr(self, key.upper(), value)
                        
                        
        except Exception as e:
            print(f"Failed to load llm_settings.yaml: {e}")

settings = Settings()
