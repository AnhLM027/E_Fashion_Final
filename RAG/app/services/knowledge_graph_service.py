"""
Knowledge Graph Service
========================

Per-workspace Knowledge Graph using LightRAG with configurable LLM + embeddings.
File-based storage (NetworkX graph + NanoVectorDB) — no extra Docker services.

Usage:
    kg = KnowledgeGraphService(workspace_id=1)
    await kg.ingest("markdown text from document...")
    result = await kg.query("What are the key themes?", mode="hybrid")
    await kg.cleanup()
"""
from __future__ import annotations

import asyncio
import logging
import os
import shutil
from pathlib import Path
from typing import Optional

import numpy as np

from app.core.config import settings
from app.services.llm import get_embedding_provider, get_llm_provider
from app.services.llm.types import LLMMessage

logger = logging.getLogger(__name__)

# --- VIETNAMESE ULTRA-CONNECTIVITY PROMPTS ---
VI_ENTITY_EXTRACTION_SYSTEM = """
---Vai trò---
Bạn là một chuyên gia về Đồ thị Tri thức (Knowledge Graph) chịu trách nhiệm trích xuất các thực thể và mối quan hệ từ văn bản tiếng Việt để tạo ra một bản đồ tri thức có độ kết nối cực cao (High Density).

---Hướng dẫn Trích xuất---
1. **Thực thể (Entities)**:
    - **Nhận dạng**: Xác định các thực thể rõ ràng và có ý nghĩa. Ưu tiên các thực thể quan trọng trong ngữ cảnh văn bản.
    - **Chuẩn hóa (Entity Resolution)**: Gom tất cả các tên gọi khác nhau của cùng một thực thể về một tên duy nhất.
    - **Loại thực thể**: Chỉ sử dụng các loại sau: {entity_types}. Nếu không rõ, hãy chọn 'Concept' hoặc 'Organization'.
    - **Mô tả**: Cung cấp mô tả ngắn gọn, súc tích về thực thể.
    - **Định dạng đầu ra**: Trình bày đúng 4 trường cho mỗi thực thể, ngăn cách bởi {tuple_delimiter}.
        - Định dạng: entity{tuple_delimiter}{{entity_name}}{tuple_delimiter}{{entity_type}}{tuple_delimiter}{{entity_description}}

2. **Mối quan hệ (Relationships)**:
    - **Nhận dạng**: Tìm ít nhất 4-6 mối quan hệ cho mỗi đoạn văn bản. Kết nối các thực thể mới với nhau.
    - **Định dạng đầu ra**: Trình bày đúng 5 trường cho mỗi quan hệ, ngăn cách bởi {tuple_delimiter}.
        - Định dạng: relation{tuple_delimiter}{{source_entity}}{tuple_delimiter}{{target_entity}}{tuple_delimiter}{{relationship_keywords}}{tuple_delimiter}{{relationship_description}}

3. **Quy tắc Nghiêm ngặt (BẮT BUỘC)**:
    - Toàn bộ đầu ra PHẢI bằng **{language}**.
    - Tuyệt đối KHÔNG sử dụng khối mã markdown (```). CHỈ trả về văn bản TUẦN TỰ theo định dạng nêu trên.
    - KHÔNG thêm bất kỳ câu dẫn, lời chào hay giải thích nào. Chỉ trả về các dòng bắt đầu bằng 'entity' hoặc 'relation'.
    - Phân tách các dòng bằng dấu xuống dòng.
    - KẾT THÚC danh sách bằng chuỗi: {completion_delimiter}
"""

async def _kg_llm_complete(
    prompt: str,
    system_prompt: Optional[str] = None,
    history_messages: Optional[list] = None,
    keyword_extraction: bool = False,
    **kwargs,
) -> str:
    """
    LightRAG-compatible LLM adapter.
    """
    provider = get_llm_provider()
    
    # Convert generic history list to our LLMMessage format

    # Convert generic history list to our LLMMessage format
    messages: list[LLMMessage] = []
    if history_messages:
        for msg in history_messages:
            messages.append(LLMMessage(
                role=msg.get("role", "user"),
                content=msg.get("content", ""),
            ))

    # Add the current prompt
    messages.append(LLMMessage(role="user", content=prompt))

    # All providers now handle system_prompt via native instructions
    # kwargs from LightRAG are ignored or mapped as needed
    # Use dynamic max_tokens from settings (profile-based)
    safe_max_tokens = settings.LLM_MAX_TOKENS

    response = await provider.acomplete(
        messages,
        system_prompt=system_prompt,
        temperature=0.0, # Force deterministic for KG
        max_tokens=safe_max_tokens,
    )
    
    # Process response: strip Markdown blocks if present
    res_str = str(response).strip()
    if res_str.startswith("```"):
        # Find first newline and last ```
        lines = res_str.split("\n")
        if lines[0].startswith("```"):
            lines = lines[1:]
        if lines and lines[-1].startswith("```"):
            lines = lines[:-1]
        res_str = "\n".join(lines).strip()

    # Debug log first 200 chars and last 50 chars of response
    logger.debug(f"KG LLM Response (len={len(res_str)}): {res_str[:200]}...{res_str[-50:]}")
    
    return res_str


async def _kg_embed(texts: list[str]) -> np.ndarray:
    """LightRAG-compatible embedding function using the configured provider."""
    provider = get_embedding_provider()
    return await provider.embed(texts)


# ---------------------------------------------------------------------------
# Main service
# ---------------------------------------------------------------------------

class KnowledgeGraphService:
    """
    Per-workspace Knowledge Graph service backed by LightRAG.

    Storage: file-based (NetworkX for graph, NanoVectorDB for vectors).
    Each knowledge base gets its own working directory.
    """

    def __init__(
        self,
        workspace_id: int,
        kg_language: str | None = None,
        kg_entity_types: list[str] | None = None,
    ):
        self.workspace_id = workspace_id
        self.working_dir = str(
            Path(settings.DATA_ROOT) / "lightrag" / f"kb_{workspace_id}"
        )
        # Per-workspace overrides (fallback to global settings)
        self.kg_language = kg_language or settings.NEXUSRAG_KG_LANGUAGE
        self.kg_entity_types = kg_entity_types or settings.NEXUSRAG_KG_ENTITY_TYPES
        self._rag = None
        self._initialized = False

    async def _get_rag(self):
        """Lazy-initialize LightRAG instance."""
        if self._rag is not None and self._initialized:
            return self._rag

        from lightrag import LightRAG
        from lightrag.utils import wrap_embedding_func_with_attrs
        from lightrag.kg.shared_storage import initialize_pipeline_status
        from lightrag.prompt import PROMPTS

        # Override standard prompts with Vietnamese optimized ones
        PROMPTS["entity_extraction_system_prompt"] = VI_ENTITY_EXTRACTION_SYSTEM
        
        os.makedirs(self.working_dir, exist_ok=True)

        # Dynamic embedding dimension from the configured provider
        emb_provider = get_embedding_provider()
        embedding_dim = emb_provider.get_dimension()

        # Detect dimension mismatch when switching providers
        dim_marker = Path(self.working_dir) / ".embedding_dim"
        if dim_marker.exists():
            prev_dim = int(dim_marker.read_text().strip())
            if prev_dim != embedding_dim:
                logger.warning(
                    f"Embedding dimension changed ({prev_dim} → {embedding_dim}) "
                    f"for workspace {self.workspace_id}. Clearing KG data for rebuild."
                )
                shutil.rmtree(self.working_dir)
                os.makedirs(self.working_dir, exist_ok=True)
        dim_marker.write_text(str(embedding_dim))

        @wrap_embedding_func_with_attrs(embedding_dim=embedding_dim, max_token_size=8192)
        async def embedding_func(texts: list[str]) -> np.ndarray:
            return await _kg_embed(texts)

        print(f"--- INITIALIZING LightRAG with max_async={settings.LLM_MAX_ASYNC} ---")
        self._rag = LightRAG(
            working_dir=self.working_dir,
            llm_model_func=_kg_llm_complete,
            embedding_func=embedding_func,
            chunk_token_size=500,
            chunk_overlap_token_size=50,
            llm_model_max_async=int(settings.LLM_MAX_ASYNC), 
            enable_llm_cache=True,
            kv_storage="JsonKVStorage",
            vector_storage="NanoVectorDBStorage",
            graph_storage="NetworkXStorage",
            doc_status_storage="JsonDocStatusStorage",
            addon_params={
                "timeout": 1200,
                "max_async": int(settings.LLM_MAX_ASYNC),
                "language": self.kg_language,
                "entity_types": self.kg_entity_types,
            },
        )
        # Set timeout attribute directly to bypass init limitations
        self._rag.llm_model_wait_timeout = 1200

        await self._rag.initialize_storages()
        await initialize_pipeline_status()
        self._initialized = True

        logger.info(
            f"LightRAG initialized for workspace {self.workspace_id} "
            f"(embedding_dim={embedding_dim})"
        )
        return self._rag

    async def ingest(self, markdown_content: str) -> None:
        """
        Ingest markdown content into the knowledge graph.
        LightRAG extracts entities and relationships automatically.
        """
        rag = await self._get_rag()

        if not markdown_content.strip():
            logger.warning(f"Empty content for workspace {self.workspace_id}, skipping KG ingest")
            return

        try:
            # Append a hidden timestamp to bypass LightRAG's internal deduplication cache
            import time
            content_to_insert = f"{markdown_content}\n\n<!-- force_reingest_at: {time.time()} -->"
            
            await rag.ainsert(content_to_insert)
            logger.info(
                f"KG ingested {len(markdown_content)} chars for workspace {self.workspace_id} (forced re-ingest)"
            )

            # Check if entities were actually extracted
            try:
                all_nodes = await rag.chunk_entity_relation_graph.get_all_nodes()
                if not all_nodes:
                    from app.core.config import settings
                    model = (
                        settings.OLLAMA_MODEL
                        if settings.LLM_PROVIDER.lower() == "ollama"
                        else settings.LLM_MODEL_FAST
                    )
                    logger.warning(
                        f"KG extraction produced 0 entities for workspace {self.workspace_id}. "
                        f"Model '{model}' may not support LightRAG's entity extraction format. "
                        f"Consider using a larger model (e.g. qwen3:14b, gemma3:12b) for KG."
                    )
            except Exception:
                pass

        except Exception as e:
            logger.error(f"KG ingest failed for workspace {self.workspace_id}: {e}")
            raise

    async def query(
        self,
        question: str,
        mode: str = "hybrid",
        top_k: int = 10,
    ) -> str:
        """
        Query the knowledge graph.

        Args:
            question: Natural language question
            mode: Query mode — "naive", "local", "global", "hybrid"
            top_k: Number of results

        Returns:
            LightRAG response text with KG-augmented answer
        """
        from lightrag import QueryParam

        rag = await self._get_rag()

        try:
            result = await asyncio.wait_for(
                rag.aquery(
                    question,
                    param=QueryParam(mode=mode, top_k=top_k),
                ),
                timeout=settings.NEXUSRAG_KG_QUERY_TIMEOUT,
            )
            return result or ""
        except asyncio.TimeoutError:
            logger.warning(
                f"KG query timed out after {settings.NEXUSRAG_KG_QUERY_TIMEOUT}s "
                f"for workspace {self.workspace_id}"
            )
            return ""
        except Exception as e:
            logger.error(f"KG query failed for workspace {self.workspace_id}: {e}")
            return ""

    async def cleanup(self) -> None:
        """Finalize storages on shutdown."""
        if self._rag:
            try:
                await self._rag.finalize_storages()
                logger.info(f"KG storages finalized for workspace {self.workspace_id}")
            except Exception as e:
                logger.warning(f"KG cleanup failed for workspace {self.workspace_id}: {e}")
            self._rag = None
            self._initialized = False

    def delete_project_data(self) -> None:
        """Delete all KG data for this knowledge base."""
        path = Path(self.working_dir)
        if path.exists():
            shutil.rmtree(path)
            logger.info(f"Deleted KG data for workspace {self.workspace_id}")
        self._rag = None
        self._initialized = False

    # ------------------------------------------------------------------
    # Knowledge Graph exploration (Phase 9)
    # ------------------------------------------------------------------

    async def get_entities(
        self,
        search: str | None = None,
        entity_type: str | None = None,
        limit: int = 200,
        offset: int = 0,
    ) -> list[dict]:
        """
        List all entities in the knowledge graph.

        Returns list of dicts with: name, entity_type, description, degree.
        """
        rag = await self._get_rag()
        storage = rag.chunk_entity_relation_graph

        try:
            all_nodes = await storage.get_all_nodes()
        except Exception as e:
            logger.error(f"Failed to get KG nodes for workspace {self.workspace_id}: {e}")
            return []

        entities = []
        for node in all_nodes:
            node_id = node.get("id", "")
            etype = node.get("entity_type", "Unknown")
            desc = node.get("description", "")

            # Filters
            if entity_type and etype.lower() != entity_type.lower():
                continue
            if search and search.lower() not in node_id.lower():
                continue

            # Get degree (number of relationships)
            try:
                degree = await storage.node_degree(node_id)
            except Exception:
                degree = 0

            entities.append({
                "name": node_id,
                "entity_type": etype,
                "description": desc,
                "degree": degree,
            })

        # Sort by degree descending
        entities.sort(key=lambda e: e["degree"], reverse=True)

        return entities[offset:offset + limit]

    async def get_relationships(
        self,
        entity_name: str | None = None,
        limit: int = 500,
    ) -> list[dict]:
        """
        List relationships in the knowledge graph.

        If entity_name is provided, returns only relationships involving that entity.
        Returns list of dicts with: source, target, description, keywords, weight.
        """
        rag = await self._get_rag()
        storage = rag.chunk_entity_relation_graph

        try:
            all_edges = await storage.get_all_edges()
        except Exception as e:
            logger.error(f"Failed to get KG edges for workspace {self.workspace_id}: {e}")
            return []

        relationships = []
        for edge in all_edges:
            src = edge.get("source", "")
            tgt = edge.get("target", "")

            if entity_name:
                if entity_name.lower() not in (src.lower(), tgt.lower()):
                    continue

            relationships.append({
                "source": src,
                "target": tgt,
                "description": edge.get("description", ""),
                "keywords": edge.get("keywords", ""),
                "weight": float(edge.get("weight", 1.0)),
            })

        return relationships[:limit]

    async def get_graph_data(
        self,
        center_entity: str | None = None,
        max_depth: int = 3,
        max_nodes: int = 150,
    ) -> dict:
        """
        Export graph data for frontend visualization.

        Returns {nodes: [...], edges: [...], is_truncated: bool}.
        Uses get_all_nodes/get_all_edges for reliable data access.
        """
        rag = await self._get_rag()
        storage = rag.chunk_entity_relation_graph

        try:
            all_nodes = await storage.get_all_nodes()
            all_edges = await storage.get_all_edges()
        except Exception as e:
            logger.error(f"Failed to get KG data for workspace {self.workspace_id}: {e}")
            return {"nodes": [], "edges": [], "is_truncated": False}

        if not all_nodes:
            return {"nodes": [], "edges": [], "is_truncated": False}

        # If center_entity requested, do BFS to get subgraph
        if center_entity:
            visited = set()
            frontier = {center_entity.upper()}
            for _ in range(max_depth):
                next_frontier = set()
                for edge in all_edges:
                    src = edge.get("source", "")
                    tgt = edge.get("target", "")
                    if src in frontier and tgt not in visited:
                        next_frontier.add(tgt)
                    if tgt in frontier and src not in visited:
                        next_frontier.add(src)
                visited |= frontier
                frontier = next_frontier - visited
                if not frontier:
                    break
            visited |= frontier
            all_nodes = [n for n in all_nodes if n.get("id", "") in visited]
            all_edges = [
                e for e in all_edges
                if e.get("source", "") in visited and e.get("target", "") in visited
            ]

        is_truncated = len(all_nodes) > max_nodes
        if is_truncated:
            # Keep nodes with highest degree first
            node_ids_in_edges = {}
            for e in all_edges:
                node_ids_in_edges[e.get("source", "")] = node_ids_in_edges.get(e.get("source", ""), 0) + 1
                node_ids_in_edges[e.get("target", "")] = node_ids_in_edges.get(e.get("target", ""), 0) + 1
            all_nodes = sorted(all_nodes, key=lambda n: node_ids_in_edges.get(n.get("id", ""), 0), reverse=True)[:max_nodes]
            kept_ids = {n.get("id", "") for n in all_nodes}
            all_edges = [e for e in all_edges if e.get("source", "") in kept_ids and e.get("target", "") in kept_ids]

        # Build degree map from edges
        degree_map: dict[str, int] = {}
        for e in all_edges:
            src = e.get("source", "")
            tgt = e.get("target", "")
            degree_map[src] = degree_map.get(src, 0) + 1
            degree_map[tgt] = degree_map.get(tgt, 0) + 1

        nodes = []
        for n in all_nodes:
            node_id = n.get("id", "")
            nodes.append({
                "id": node_id,
                "label": node_id,
                "entity_type": (n.get("entity_type") or n.get("type") or "Unknown").lower(),
                "description": (n.get("description") or "")[:200],
                "degree": degree_map.get(node_id, 0),
            })

        edges = []
        seen_edges = set()
        for e in all_edges:
            src = e.get("source", "")
            tgt = e.get("target", "")
            key = f"{src}|{tgt}"
            if key in seen_edges:
                continue
            seen_edges.add(key)
            edges.append({
                "source": src,
                "target": tgt,
                "label": (e.get("description") or e.get("keywords") or "")[:80],
                "weight": float(e.get("weight", 1.0)),
            })

        return {
            "nodes": nodes,
            "edges": edges,
            "is_truncated": is_truncated,
        }

    async def get_relevant_context(
        self,
        question: str,
        max_entities: int = 20,
        max_relationships: int = 30,
    ) -> str:
        """
        Build RAG context from raw KG data (no LLM generation).

        Instead of calling LightRAG's aquery() which uses LLM to generate
        a narrative (and can hallucinate), this method:
          1. Tokenizes the question into keywords
          2. Finds entities whose names match any keyword
          3. Gets relationships connecting those entities
          4. Formats everything as structured factual text

        Returns:
            Structured string of entities + relationships, or "" if nothing found.
        """
        rag = await self._get_rag()
        storage = rag.chunk_entity_relation_graph

        try:
            all_nodes = await storage.get_all_nodes()
            all_edges = await storage.get_all_edges()
        except Exception as e:
            logger.error(f"Failed to get raw KG data for workspace {self.workspace_id}: {e}")
            return ""

        if not all_nodes:
            return ""

        # -- 1. Extract keywords from question --
        # Simple but effective: split, lowercase, filter short words
        raw_tokens = question.lower().split()
        # Also handle hyphenated/versioned tokens like "deepseek-v3.2"
        keywords = set()
        for token in raw_tokens:
            # Remove punctuation at edges
            cleaned = token.strip(".,?!:;\"'()[]{}").lower()
            if len(cleaned) >= 2:
                keywords.add(cleaned)

        if not keywords:
            return ""

        # -- 2. Find matching entities --
        matched_entity_names: set[str] = set()
        entity_info: dict[str, dict] = {}  # name → {type, description}

        for node in all_nodes:
            node_id = node.get("id", "")
            node_lower = node_id.lower()

            # Check if any keyword is a substring of entity name OR vice versa
            matched = False
            for kw in keywords:
                if kw in node_lower or node_lower in kw:
                    matched = True
                    break
                # Also check multi-word keywords (e.g., "deepseek" matches "DEEPSEEK-V3.2")
                for part in node_lower.split("-"):
                    if kw in part or part in kw:
                        matched = True
                        break
                if matched:
                    break

            if matched:
                matched_entity_names.add(node_id)
                entity_info[node_id] = {
                    "entity_type": node.get("entity_type", "Unknown"),
                    "description": node.get("description", ""),
                }

        if not matched_entity_names and len(all_nodes) <= 50:
            # Small graph: include top entities by default
            for node in all_nodes[:10]:
                nid = node.get("id", "")
                matched_entity_names.add(nid)
                entity_info[nid] = {
                    "entity_type": node.get("entity_type", "Unknown"),
                    "description": node.get("description", ""),
                }

        if not matched_entity_names:
            return ""

        # Limit entities
        matched_list = list(matched_entity_names)[:max_entities]

        # -- 3. Find relationships involving matched entities --
        relevant_rels: list[dict] = []
        matched_lower = {n.lower() for n in matched_list}

        for edge in all_edges:
            src = edge.get("source", "")
            tgt = edge.get("target", "")
            if src.lower() in matched_lower or tgt.lower() in matched_lower:
                relevant_rels.append({
                    "source": src,
                    "target": tgt,
                    "description": edge.get("description", ""),
                    "keywords": edge.get("keywords", ""),
                })
                # Also add connected entities we might have missed
                if src not in entity_info:
                    # Find node info
                    for n in all_nodes:
                        if n.get("id", "") == src:
                            entity_info[src] = {
                                "entity_type": n.get("entity_type", "Unknown"),
                                "description": n.get("description", ""),
                            }
                            break
                if tgt not in entity_info:
                    for n in all_nodes:
                        if n.get("id", "") == tgt:
                            entity_info[tgt] = {
                                "entity_type": n.get("entity_type", "Unknown"),
                                "description": n.get("description", ""),
                            }
                            break

            if len(relevant_rels) >= max_relationships:
                break

        # -- 4. Format as structured text --
        parts: list[str] = []

        # Entities section
        if matched_list:
            parts.append("Entities found in documents:")
            for name in matched_list:
                info = entity_info.get(name, {})
                etype = info.get("entity_type", "")
                desc = info.get("description", "")
                # Truncate long descriptions
                if len(desc) > 200:
                    desc = desc[:200] + "..."
                type_str = f" [{etype}]" if etype and etype != "Unknown" else ""
                if desc:
                    parts.append(f"- {name}{type_str}: {desc}")
                else:
                    parts.append(f"- {name}{type_str}")

        # Relationships section
        if relevant_rels:
            parts.append("")
            parts.append("Relationships:")
            for rel in relevant_rels:
                desc = rel["description"]
                if len(desc) > 150:
                    desc = desc[:150] + "..."
                if desc:
                    parts.append(f"- {rel['source']} → {rel['target']}: {desc}")
                else:
                    parts.append(f"- {rel['source']} → {rel['target']}")

        result = "\n".join(parts)
        logger.info(
            f"KG raw context: {len(matched_list)} entities, "
            f"{len(relevant_rels)} relationships for workspace {self.workspace_id}"
        )
        return result

    async def get_analytics(self) -> dict:
        """
        Compute KG analytics summary.

        Returns: entity_count, relationship_count, entity_types, top_entities, avg_degree.
        """
        rag = await self._get_rag()
        storage = rag.chunk_entity_relation_graph

        try:
            all_nodes = await storage.get_all_nodes()
            all_edges = await storage.get_all_edges()
        except Exception as e:
            logger.error(f"Failed to get KG analytics for workspace {self.workspace_id}: {e}")
            return {
                "entity_count": 0,
                "relationship_count": 0,
                "entity_types": {},
                "top_entities": [],
                "avg_degree": 0.0,
            }

        entity_count = len(all_nodes)
        relationship_count = len(all_edges)

        # Count entity types
        type_counts: dict[str, int] = {}
        entities_with_degree = []
        for node in all_nodes:
            etype = node.get("entity_type", "Unknown")
            type_counts[etype] = type_counts.get(etype, 0) + 1
            try:
                degree = await storage.node_degree(node.get("id", ""))
            except Exception:
                degree = 0
            entities_with_degree.append({
                "name": node.get("id", ""),
                "entity_type": etype,
                "description": node.get("description", ""),
                "degree": degree,
            })

        # Sort by degree for top entities
        entities_with_degree.sort(key=lambda e: e["degree"], reverse=True)
        top_entities = entities_with_degree[:1000]

        avg_degree = (
            sum(e["degree"] for e in entities_with_degree) / entity_count
            if entity_count > 0
            else 0.0
        )

        return {
            "entity_count": entity_count,
            "relationship_count": relationship_count,
            "entity_types": type_counts,
            "top_entities": top_entities,
            "avg_degree": round(avg_degree, 2),
        }
