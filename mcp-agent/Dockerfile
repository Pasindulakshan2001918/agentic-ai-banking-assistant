FROM python:3.11-slim
WORKDIR /app

# Install system dependencies if necessary
COPY pyproject.toml .
COPY . .
RUN pip --default-timeout=180 --retries=8 install .

EXPOSE 8290
CMD ["python", "gateway.py"]
