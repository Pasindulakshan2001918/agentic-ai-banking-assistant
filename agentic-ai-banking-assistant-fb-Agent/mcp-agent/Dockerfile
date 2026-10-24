FROM python:3.11-slim
WORKDIR /app

# Install system dependencies if necessary
COPY pyproject.toml .
COPY . .
RUN pip install .

EXPOSE 8290
CMD ["python", "gateway.py"]
