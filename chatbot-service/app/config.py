from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "Dịch vụ Chatbot Y tế"
    fhir_base_url: str = Field(default="http://localhost:8080/fhir")
    fhir_request_timeout_seconds: float = Field(default=20)
    llm_provider: str = Field(default="openai")
    llm_model: str = Field(default="gpt-4.1-mini")
    openai_api_key: str | None = Field(default=None)
    llm_request_timeout_seconds: float = Field(default=20)
    enable_llm_answer: bool = Field(default=True)

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    @property
    def normalized_fhir_base_url(self) -> str:
        return self.fhir_base_url.rstrip("/")

    @property
    def use_openai_llm(self) -> bool:
        return self.llm_provider.lower() == "openai" and bool(self.openai_api_key)

    @property
    def use_llm_answer(self) -> bool:
        return self.enable_llm_answer and self.use_openai_llm


@lru_cache
def get_settings() -> Settings:
    return Settings()
