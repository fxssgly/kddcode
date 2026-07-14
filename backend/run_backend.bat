@echo off
set "KDD_PYTHON="

for /f "usebackq delims=" %%P in (`python -c "import sys; print(sys.executable if sys.version_info.major == 3 else '')" 2^>nul`) do set "KDD_PYTHON=%%P"

if not defined KDD_PYTHON (
  for /f "usebackq delims=" %%P in (`py -3 -c "import sys; print(sys.executable)" 2^>nul`) do set "KDD_PYTHON=%%P"
)

if not defined KDD_PYTHON if exist "%LOCALAPPDATA%\Programs\Python\Python310\python.exe" (
  set "KDD_PYTHON=%LOCALAPPDATA%\Programs\Python\Python310\python.exe"
)

if not defined KDD_PYTHON (
  echo Python 3 was not found. Please check python --version or py -3 --version.
  pause
  exit /b 1
)

echo Using Python: %KDD_PYTHON%
mvn spring-boot:run
pause
