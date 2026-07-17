# -*- coding: utf-8 -*-
"""Clustering algorithms used by the clustering analysis page."""
import math

from algorithms.common import FEATURES, as_float


METHOD_NAMES = {
    "kmeans": "K-Means",
    "agglomerative": "AgglomerativeClustering",
    "birch": "Birch",
    "dbscan": "DBSCAN",
    "meanshift": "MeanShift",
}


def feature_matrix(rows):
    """Extract Iris numeric features from rows."""
    return [[as_float(row.get(name)) for name in FEATURES] for row in rows]


def manual_standardize(matrix):
    """Small dependency-free scaler used only if sklearn is unavailable."""
    if not matrix:
        return []
    count = float(len(matrix))
    columns = len(matrix[0])
    means = [sum(row[index] for row in matrix) / count for index in range(columns)]
    stds = []
    for index in range(columns):
        variance = sum((row[index] - means[index]) ** 2 for row in matrix) / count
        stds.append(math.sqrt(variance) or 1.0)
    return [[(value - means[index]) / stds[index] for index, value in enumerate(row)] for row in matrix]


def covariance_matrix(matrix):
    if not matrix:
        return [], []
    count = float(len(matrix))
    columns = len(matrix[0])
    means = [sum(row[index] for row in matrix) / count for index in range(columns)]
    centered = [[value - means[index] for index, value in enumerate(row)] for row in matrix]
    denominator = max(count - 1.0, 1.0)
    covariance = []
    for row_index in range(columns):
        covariance_row = []
        for col_index in range(columns):
            covariance_row.append(sum(row[row_index] * row[col_index] for row in centered) / denominator)
        covariance.append(covariance_row)
    return covariance, centered


def mat_vec(matrix, vector):
    return [sum(value * vector[index] for index, value in enumerate(row)) for row in matrix]


def vec_norm(vector):
    return math.sqrt(sum(value * value for value in vector)) or 1.0


def power_iteration(matrix, iterations=80, initial=None):
    vector = list(initial) if initial else [1.0 for _ in matrix]
    vector = [value / vec_norm(vector) for value in vector]
    for _ in range(iterations):
        next_vector = mat_vec(matrix, vector)
        length = math.sqrt(sum(value * value for value in next_vector))
        if length < 1e-12:
            break
        vector = [value / length for value in next_vector]
    eigenvalue = sum(vector[index] * mat_vec(matrix, vector)[index] for index in range(len(vector)))
    return eigenvalue, vector


def manual_pca(matrix):
    """Small PCA implementation kept for the Java fallback path compatibility."""
    covariance, centered = covariance_matrix(matrix)
    if not covariance:
        return []
    first_value, first_vector = power_iteration(covariance)
    deflated = [
        [
            covariance[row_index][col_index] - first_value * first_vector[row_index] * first_vector[col_index]
            for col_index in range(len(first_vector))
        ]
        for row_index in range(len(first_vector))
    ]
    _, second_vector = power_iteration(deflated, initial=[0.5, -1.0, 0.75, -0.25])
    return [
        [
            sum(values[index] * first_vector[index] for index in range(len(FEATURES))),
            sum(values[index] * second_vector[index] for index in range(len(FEATURES))),
        ]
        for values in centered
    ]


def normalized_method(value):
    method = str(value or "kmeans").lower().replace("-", "").replace("_", "")
    aliases = {
        "kmeans": "kmeans",
        "agglomerative": "agglomerative",
        "agglomerativeclustering": "agglomerative",
        "hierarchical": "agglomerative",
        "birch": "birch",
        "dbscan": "dbscan",
        "meanshift": "meanshift",
        "mean": "meanshift",
    }
    return aliases.get(method, "kmeans")


def cluster_count(payload, rows):
    return max(1, min(int(as_float(payload.get("k"), 3)), len(rows)))


def build_model(method, payload, rows):
    from sklearn.cluster import AgglomerativeClustering, Birch, DBSCAN, KMeans, MeanShift

    if method == "kmeans":
        return KMeans(n_clusters=cluster_count(payload, rows), random_state=42, n_init=10)
    if method == "agglomerative":
        linkage = str(payload.get("linkage") or "ward")
        if linkage not in ("ward", "complete", "average", "single"):
            linkage = "ward"
        return AgglomerativeClustering(n_clusters=cluster_count(payload, rows), linkage=linkage)
    if method == "birch":
        threshold = max(as_float(payload.get("threshold"), 0.5), 0.01)
        return Birch(n_clusters=cluster_count(payload, rows), threshold=threshold)
    if method == "dbscan":
        eps = max(as_float(payload.get("eps"), 0.5), 0.01)
        min_samples = max(1, int(as_float(payload.get("min_samples"), 5)))
        return DBSCAN(eps=eps, min_samples=min_samples)
    bandwidth = as_float(payload.get("bandwidth"), 0)
    return MeanShift(**({"bandwidth": bandwidth} if bandwidth > 0 else {}))


def fit_predict(method, payload, matrix, rows):
    model = build_model(method, payload, rows)
    labels = model.fit_predict(matrix) if hasattr(model, "fit_predict") else model.fit(matrix).labels_
    return model, [int(label) for label in labels]


def pca_coordinates(matrix):
    from sklearn.decomposition import PCA

    if len(matrix) == 0:
        return []
    projected = PCA(n_components=2, random_state=42).fit_transform(matrix)
    return [[round(float(point[0]), 4), round(float(point[1]), 4)] for point in projected]


def label_sort_key(label):
    return (label < 0, label)


def center_rows(rows):
    groups = {}
    for row in rows:
        label = int(row.get("cluster", 0))
        if label < 0:
            continue
        groups.setdefault(label, []).append(row)
    centers = []
    for label in sorted(groups, key=label_sort_key):
        group = groups[label]
        centers.append({
            "cluster": label,
            "pca1": round(sum(as_float(row.get("pca1")) for row in group) / len(group), 4),
            "pca2": round(sum(as_float(row.get("pca2")) for row in group) / len(group), 4),
        })
    return centers


def sklearn_clustering(payload):
    from sklearn.preprocessing import StandardScaler

    rows = [dict(row) for row in (payload.get("rows") or [])]
    if not rows:
        return {"rows": [], "centers": [], "method": normalized_method(payload.get("method"))}

    method = normalized_method(payload.get("method"))
    raw_matrix = feature_matrix(rows)
    matrix = StandardScaler().fit_transform(raw_matrix)
    coordinates = pca_coordinates(matrix)
    model, labels = fit_predict(method, payload, matrix, rows)

    for row, label, point in zip(rows, labels, coordinates):
        row["cluster"] = label
        row["pca1"] = point[0]
        row["pca2"] = point[1]

    return {
        "rows": rows,
        "centers": center_rows(rows),
        "method": method,
        "methodName": METHOD_NAMES.get(method, METHOD_NAMES["kmeans"]),
        "clusterCount": len(set(label for label in labels if label >= 0)),
        "noiseCount": sum(1 for label in labels if label < 0),
    }


def manual_kmeans(payload):
    rows = [dict(row) for row in (payload.get("rows") or [])]
    if not rows:
        return {"rows": [], "centers": [], "method": "kmeans"}
    k = cluster_count(payload, rows)
    matrix = manual_standardize(feature_matrix(rows))
    coordinates = manual_pca(matrix)
    if coordinates:
        centers = [list(values) for values in coordinates[:k]]
        for _ in range(20):
            groups = [[] for _ in range(k)]
            for row_index, row in enumerate(rows):
                cluster_index = min(
                    range(k),
                    key=lambda index: math.sqrt(sum((coordinates[row_index][feature] - centers[index][feature]) ** 2 for feature in range(2))),
                )
                row["cluster"] = cluster_index
                groups[cluster_index].append(row_index)
            next_centers = []
            for index, group in enumerate(groups):
                if not group:
                    next_centers.append(centers[index])
                else:
                    next_centers.append([
                        sum(coordinates[row_index][feature_index] for row_index in group) / len(group)
                        for feature_index in range(2)
                    ])
            if next_centers == centers:
                break
            centers = next_centers
    for row, point in zip(rows, coordinates):
        row["pca1"] = round(point[0], 4)
        row["pca2"] = round(point[1], 4)
    return {
        "rows": rows,
        "centers": center_rows(rows),
        "method": "kmeans",
        "methodName": METHOD_NAMES["kmeans"],
        "clusterCount": k,
        "noiseCount": 0,
    }


def clustering(payload):
    """Run the selected clustering method and return rows ready for charting."""
    try:
        return sklearn_clustering(payload)
    except ImportError as exc:
        method = normalized_method(payload.get("method"))
        if method == "kmeans":
            return manual_kmeans(payload)
        raise ImportError("sklearn is required for %s clustering." % METHOD_NAMES.get(method, method))
