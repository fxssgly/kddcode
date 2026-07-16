@echo off
REM 自动寻找可用的 Python 3，并把路径写入 KDD_PYTHON，供 Spring Boot 调用算法脚本。
set "KDD_PYTHON="

REM 优先尝试 python 命令；只有检测到 Python 3 时才使用。
for /f "usebackq delims=" %%P in (`python -c "import sys; print(sys.executable if sys.version_info.major == 3 else '')" 2^>nul`) do set "KDD_PYTHON=%%P"

REM 如果 python 不可用，再尝试 Windows Python Launcher 的 py -3。
if not defined KDD_PYTHON (
  for /f "usebackq delims=" %%P in (`py -3 -c "import sys; print(sys.executable)" 2^>nul`) do set "KDD_PYTHON=%%P"
)

REM 兼容常见的 Python 3.10 默认安装路径。
if not defined KDD_PYTHON if exist "%LOCALAPPDATA%\Programs\Python\Python310\python.exe" (
  set "KDD_PYTHON=%LOCALAPPDATA%\Programs\Python\Python310\python.exe"
)

REM 找不到 Python 3 时停止启动，避免后端启动后算法接口不可用。
if not defined KDD_PYTHON (
  echo Python 3 was not found. Please check python --version or py -3 --version.
  pause
  exit /b 1
)

echo Using Python: %KDD_PYTHON%
REM 设置默认数据库连接参数；外部环境变量已设置时不会覆盖。
if not defined USE_MYSQL set "USE_MYSQL=true"
if not defined DB_USER set "DB_USER=root"
if not defined DB_URL set "DB_URL=jdbc:mysql://127.0.0.1:3306/bigdata?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
if not defined DB_PASSWORD set "DB_PASSWORD=password"

REM 启动 Spring Boot 后端。
mvn spring-boot:run
pause
