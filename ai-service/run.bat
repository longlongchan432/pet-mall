@echo off
REM 启动 AI 导购服务（端口 8082）
python -m uvicorn main:app --host 0.0.0.0 --port 8082 --reload
pause
