FROM python:3.13-alpine

COPY --from=ghcr.io/astral-sh/uv:0.7.8 /uv /usr/local/bin/uv

WORKDIR /app
COPY pyproject.toml uv.lock* ./
RUN uv sync --frozen --no-dev

COPY main.py models.py settings.py ./

CMD ["uv", "run", "python", "main.py"]
