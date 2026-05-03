@echo off
set PGPASSWORD=adminpass
cd C:\Program Files\PostgreSQL\18\bin
psql -U admin -h 127.0.0.1 -d agenticdb -c "SELECT version();"
