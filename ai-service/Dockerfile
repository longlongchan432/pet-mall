# AI 导购（FastAPI + SSE）镜像
FROM python:3.11-slim
WORKDIR /app
ENV PYTHONUNBUFFERED=1
# 依赖层单独缓存；国内网络慢已默认走清华镜像，不需要可去掉 -i 那一段
COPY requirements.txt .
RUN pip install --no-cache-dir -i https://pypi.tuna.tsinghua.edu.cn/simple -r requirements.txt
COPY . .
EXPOSE 8082
# 与 ai-service/run.bat 一致，只去掉 --reload（容器里不需要热重载）
CMD ["python", "-m", "uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8082"]
