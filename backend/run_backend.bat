@echo off
setlocal

REM Start backend after selecting a Python 3 environment for Java algorithm calls.
REM Prefer the project Anaconda Python so Java and Python use the same packages.
if not defined KDD_PYTHON if exist "E:\Develop\Anaconda\python.exe" (
  set "KDD_PYTHON=E:\Develop\Anaconda\python.exe"
)

REM Fallback to the Windows Python launcher.
if not defined KDD_PYTHON (
  for /f "usebackq delims=" %%P in (`py -3 -c "import sys; print(sys.executable)" 2^>nul`) do set "KDD_PYTHON=%%P"
)

REM Fallback to python on PATH, but require Python 3.
if not defined KDD_PYTHON (
  for /f "usebackq delims=" %%P in (`python -c "import sys; print(sys.executable if sys.version_info.major == 3 else '')" 2^>nul`) do set "KDD_PYTHON=%%P"
)

if not defined KDD_PYTHON (
  echo Python 3 was not found. Please install Python 3 or set KDD_PYTHON.
  pause
  exit /b 1
)

"%KDD_PYTHON%" -c "import sys; raise SystemExit(0 if sys.version_info.major == 3 else 1)" >nul 2>nul
if errorlevel 1 (
  echo Selected Python is not Python 3: %KDD_PYTHON%
  pause
  exit /b 1
)

"%KDD_PYTHON%" -c "import sklearn" >nul 2>nul
if errorlevel 1 (
  echo scikit-learn is not installed for: %KDD_PYTHON%
  echo Run: "%KDD_PYTHON%" -m pip install scikit-learn
  pause
  exit /b 1
)

echo Using Python: %KDD_PYTHON%
"%KDD_PYTHON%" -c "import sys, sklearn; print('Python ' + sys.version.split()[0]); print('scikit-learn ' + sklearn.__version__)"

REM Set default backend database connection without overriding existing variables.
if not defined USE_MYSQL set "USE_MYSQL=true"
if not defined DB_USER set "DB_USER=root"
if not defined DB_URL set "DB_URL=jdbc:mysql://127.0.0.1:3306/bigdata?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
if not defined DB_PASSWORD set "DB_PASSWORD=password"

mvn spring-boot:run
pause
