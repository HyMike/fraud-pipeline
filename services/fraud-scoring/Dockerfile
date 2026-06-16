FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY features.py api.py ./
COPY model/ ./model/

EXPOSE 8001

CMD ["uvicorn", "api:app", "--host", "0.0.0.0", "--port", "8001"]
