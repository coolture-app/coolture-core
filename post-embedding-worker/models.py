from datetime import datetime
from typing import Optional
from uuid import UUID

from pydantic import BaseModel, computed_field


class PostEmbeddingMessage(BaseModel):
    postId: UUID
    title: str
    description: str
    tags: Optional[list[str]] = None
    createdAt: datetime

    @computed_field
    @property
    def text(self) -> str:
        tags_str = " ".join(self.tags or [])
        return f"{self.title} {self.description} {tags_str}".strip()


class PostEmbeddingRecord(BaseModel):
    post_id: UUID
    embedding: list[float]
    model: str
