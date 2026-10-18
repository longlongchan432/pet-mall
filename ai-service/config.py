"""配置：从环境变量或本地 .env 读取，无需额外依赖。"""
import os
from pathlib import Path

# ponytail: python-dotenv 不是必需的，3 行解析 .env 足够
_env_file = Path(__file__).parent / ".env"
if _env_file.exists():
    for line in _env_file.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line and not line.startswith("#") and "=" in line:
            k, v = line.split("=", 1)
            os.environ.setdefault(k.strip(), v.strip())

DEEPSEEK_API_KEY = os.environ.get("DEEPSEEK_API_KEY", "")
DEEPSEEK_BASE_URL = os.environ.get("DEEPSEEK_BASE_URL", "https://api.deepseek.com")
LLM_MODEL = os.environ.get("LLM_MODEL", "deepseek-chat")
MALL_API_BASE = os.environ.get("MALL_API_BASE", "http://localhost:8080/api")

# 多轮对话保留的历史消息条数（节省 token）
MAX_HISTORY = 10
# Agent 单次任务最多调用工具的轮数（防止死循环烧钱）
MAX_TOOL_ROUNDS = 5

# 记忆层（L1 会话 + L2 用户长期记忆）落地文件，docs/ai-memory-v1.md
MEMORY_FILE = os.environ.get("MEMORY_FILE", str(Path(__file__).parent / "data" / "memory.json"))
MAX_FACTS = 20          # 单个用户长期记忆条数上限，超出丢最旧
MAX_SESSIONS = 200      # 保留的会话上限，超出丢最旧
SESSION_TITLE_LEN = 16  # 会话标题长度上限（从前三个用户问题摘要提取），超出截断
