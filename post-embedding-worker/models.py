from datetime import datetime
from typing import Optional
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field


class PostEmbeddingMessage(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    post_id: UUID = Field(alias="postId")
    title: str
    description: str
    tags: Optional[list[str]] = None
    created_at: datetime = Field(alias="createdAt")

    @property
    def text(self) -> str:
        tags_str = " ".join(self.tags or [])
        return f"{self.title} {self.description} {tags_str}".strip()


class PostEmbeddingRecord(BaseModel):
    post_id: UUID
    embedding: list[float]
    model: str
