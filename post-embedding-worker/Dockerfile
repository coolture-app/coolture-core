FROM python:3.13-alpine

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt --extra-index-url https://download.pytorch.org/whl/cpu
COPY main.py models.py settings.py ./

CMD ["python", "main.py"]
