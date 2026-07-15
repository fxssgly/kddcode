# -*- coding: utf-8 -*-
import math

from algorithms.common import FEATURES, as_float


def feature_matrix(rows):
    return [[as_float(row.get(name)) for name in FEATURES] for row in rows]


def standardize_matrix(matrix):
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
        return []
    count = float(len(matrix))
    columns = len(matrix[0])
    means = [sum(row[index] for row in matrix) / count for index in range(columns)]
    centered = [[value - means[index] for index, value in enumerate(row)] for row in matrix]
    denominator = max(count - 1.0, 1.0)
    covariance = []
    for row_index in range(columns):
        covariance_row = []
        for col_index in range(columns):
            value = sum(row[row_index] * row[col_index] for row in centered) / denominator
            covariance_row.append(value)
        covariance.append(covariance_row)
    return covariance, centered


def mat_vec(matrix, vector):
    return [sum(value * vector[index] for index, value in enumerate(row)) for row in matrix]


def vec_norm(vector):
    return math.sqrt(sum(value * value for value in vector)) or 1.0


def raw_vec_norm(vector):
    return math.sqrt(sum(value * value for value in vector))


def power_iteration(matrix, iterations=80, initial=None):
    vector = list(initial) if initial else [1.0 for _ in matrix]
    length = vec_norm(vector)
    vector = [value / length for value in vector]
    for _ in range(iterations):
        next_vector = mat_vec(matrix, vector)
        length = raw_vec_norm(next_vector)
        if length < 1e-12:
            break
        vector = [value / length for value in next_vector]
    eigenvalue = sum(vector[index] * mat_vec(matrix, vector)[index] for index in range(len(vector)))
    return eigenvalue, vector


def deflate(matrix, eigenvalue, vector):
    return [
        [
            matrix[row_index][col_index] - eigenvalue * vector[row_index] * vector[col_index]
            for col_index in range(len(vector))
        ]
        for row_index in range(len(vector))
    ]


def pca_model(matrix):
    covariance, centered = covariance_matrix(matrix)
    if not covariance:
        return None
    first_value, first_vector = power_iteration(covariance)
    second_matrix = deflate(covariance, first_value, first_vector)
    _, second_vector = power_iteration(second_matrix, initial=[0.5, -1.0, 0.75, -0.25])
    count = float(len(matrix))
    means = [sum(row[index] for row in matrix) / count for index in range(len(matrix[0]))]
    return {
        "means": means,
        "vectors": [first_vector, second_vector],
        "centered": centered,
    }


def project_pca(values, model):
    centered_values = [value - model["means"][index] for index, value in enumerate(values)]
    return [
        round(sum(centered_values[index] * vector[index] for index in range(len(FEATURES))), 4)
        for vector in model["vectors"]
    ]


def add_pca_coordinates(rows, matrix):
    model = pca_model(matrix)
    if model is None:
        return None
    for row, centered_values in zip(rows, model["centered"]):
        row["pca1"] = round(sum(centered_values[index] * model["vectors"][0][index] for index in range(len(FEATURES))), 4)
        row["pca2"] = round(sum(centered_values[index] * model["vectors"][1][index] for index in range(len(FEATURES))), 4)
    return model


def vector_distance(values, center):
    return math.sqrt(sum((values[index] - center[index]) ** 2 for index in range(len(values))))


def clustering(payload):
    rows = [dict(row) for row in (payload.get("rows") or [])]
    if not rows:
        return {"rows": [], "centers": []}
    k = max(1, min(int(as_float(payload.get("k"), 3)), len(rows)))
    matrix = standardize_matrix(feature_matrix(rows))
    centers = [list(values) for values in matrix[:k]]

    for _ in range(20):
        groups = [[] for _ in range(k)]
        for row_index, row in enumerate(rows):
            cluster_index = min(range(k), key=lambda index: vector_distance(matrix[row_index], centers[index]))
            row["cluster"] = cluster_index
            groups[cluster_index].append(row_index)
        new_centers = []
        for index, group in enumerate(groups):
            if not group:
                new_centers.append(centers[index])
            else:
                new_centers.append([
                    sum(matrix[row_index][feature_index] for row_index in group) / len(group)
                    for feature_index in range(len(FEATURES))
                ])
        if new_centers == centers:
            break
        centers = new_centers

    model = add_pca_coordinates(rows, matrix)
    center_rows = []
    for index, center in enumerate(centers):
        pca1, pca2 = project_pca(center, model) if model else (0, 0)
        center_rows.append({
            "cluster": index,
            "pca1": pca1,
            "pca2": pca2,
        })
    return {"rows": rows, "centers": center_rows}
