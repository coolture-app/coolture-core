FROM mcr.microsoft.com/playwright/python:latest

WORKDIR /app

COPY requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

COPY 3city.py ./3city.py
COPY seeder.py ./seeder.py
COPY entrypoint.sh ./entrypoint.sh
RUN chmod +x ./entrypoint.sh

CMD ["./entrypoint.sh"]
