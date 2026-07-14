# KDD Spring Boot Backend

This backend uses Spring Boot as the only web service. Java exposes the REST API, reads demo data from CSV or MySQL, and calls Python only for the data mining algorithms.

## Runtime Split

- Java Spring Boot: HTTP API, upload handling, CSV/MySQL data access.
- Python: association rules, K-Means clustering, decision tree classification, and linear regression.

## Main Files

- `src/main/java/com/kdd/demo/controller/DataMiningController.java`: REST endpoints.
- `src/main/java/com/kdd/demo/service/DataService.java`: CSV upload, demo CSV fallback, optional MySQL access.
- `src/main/java/com/kdd/demo/service/PythonAlgorithmService.java`: Java-to-Python bridge.
- `python/kdd_algorithms.py`: algorithm implementation called by Java.
- `src/main/resources/application.properties`: server, database, and Python command settings.

## Run

Run `run_backend.bat`.

The script sets `KDD_PYTHON` to Python 3.10 and starts Spring Boot on port `5000`.

## Build

Run `install_backend.bat`.

The script builds the Spring Boot project and runs the backend tests.

## API Flow

```text
Vue frontend
  -> Spring Boot Controller
  -> DataService loads CSV/MySQL/uploaded data
  -> PythonAlgorithmService writes a JSON request
  -> python/kdd_algorithms.py calculates the result
  -> Spring Boot returns JSON to the frontend
```

Only the Spring Boot backend is kept as the web service. Python files under `python/` are algorithm modules called by Java.
