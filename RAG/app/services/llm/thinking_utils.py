"""Helpers to extract embedded reasoning from model output text."""

import re
from typing import Tuple

_THINK_TAG_RE = re.compile(r"<(?:think|thinking)>(.*?)</(?:think|thinking)>", re.IGNORECASE | re.DOTALL)
_THINK_SECTION_RE = re.compile(
    r"(?is)(?:^|\n)(?:THINKING|THINK|Suy nghĩ|Reasoning|Consultation Style|Mental Sandbox Simulation|Strategizing complete)\s*[:\-]*\s*(.*?)"
    r"(?:\n(?:ANSWER|Trả lời|Response|Kết luận|Strategizing complete\. I will now generate the response)\s*[:\-]\s*(.*))?$"
)


def extract_thinking_and_clean_answer(text: str) -> tuple[str, str]:
    """Extract reasoning/thinking from a generated response and return cleaned answer."""
    if not text:
        return "", ""

    thinking_fragments: list[str] = []
    cleaned = text.strip()

    # Extract explicit think tags such as <think>...</think> or <thinking>...</thinking>
    for match in _THINK_TAG_RE.findall(text):
        part = match.strip()
        if part:
            thinking_fragments.append(part)
    if thinking_fragments:
        cleaned = _THINK_TAG_RE.sub("", cleaned).strip()

    if not thinking_fragments:
        match = _THINK_SECTION_RE.search(text)
        if match:
            thinking_text = match.group(1).strip()
            answer_text = (match.group(2) or "").strip()
            if thinking_text:
                thinking_fragments.append(thinking_text)
            if answer_text:
                cleaned = answer_text
            else:
                cleaned = _THINK_SECTION_RE.sub("", cleaned).strip()

    thinking_text = "\n\n".join(thinking_fragments).strip()

    # Remove accidental answer labels from the cleaned answer
    cleaned = re.sub(
        r"(?is)^(?:ANSWER|Trả lời|Response|Kết luận)\s*[:\-]\s*",
        "",
        cleaned,
    ).strip()

    return cleaned, thinking_text
