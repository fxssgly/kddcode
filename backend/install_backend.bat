@echo off
setlocal

REM 文件作用：构建后端前先检查 Python 3 和 scikit-learn，避免打包后运行算法失败。
REM 优先使用本项目约定的 Anaconda Python。
if not defined KDD_PYTHON if exist "E:\Develop\Anaconda\python.exe" (
  set "KDD_PYTHON=E:\Develop\Anaconda\python.exe"
)

REM 默认路径不可用时，再寻找系统里的 Python 3。
if not defined KDD_PYTHON (
  for /f "usebackq delims=" %%P in (`py -3 -c "import sys; print(sys.executable)" 2^>nul`) do set "KDD_PYTHON=%%P"
)

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

mvn clean package
pause
