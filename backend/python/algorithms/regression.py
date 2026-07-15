# -*- coding: utf-8 -*-
import random

from algorithms.common import as_float


def regression(payload):
    rows = payload.get("rows") or []
    x_field = payload.get("x_field") or "x"
    y_field = payload.get("y_field") or "y"
    points = []
    for index, row in enumerate(rows):
        points.append({
            "id": row.get("id", index + 1),
            "x": as_float(row.get(x_field)),
            "y": as_float(row.get(y_field)),
            "type": row.get("type", row.get("species", u"\u6837\u672c")),
        })

    train_points, test_points = train_test_split_points(points)
    linear = fit_linear(train_points)
    polynomial = fit_quadratic(train_points)
    ransac = fit_ransac(train_points)

    for item in points:
        item["linear_predicted"] = predict_linear(linear, item["x"])
        item["polynomial_predicted"] = predict_quadratic(polynomial, item["x"])
        item["ransac_predicted"] = predict_linear(ransac, item["x"])
        item["predicted"] = item["linear_predicted"]
        item["residual"] = round(item["y"] - item["linear_predicted"], 4)

    models = [
        build_regression_model("linear", u"\u4e00\u5143\u7ebf\u6027\u56de\u5f52", linear, test_points, predict_linear),
        build_regression_model("polynomial", u"\u4e00\u5143\u4e8c\u6b21\u591a\u9879\u5f0f\u56de\u5f52", polynomial, test_points, predict_quadratic),
        build_regression_model("ransac", u"RANSAC \u7a33\u5065\u56de\u5f52", ransac, test_points, predict_linear),
    ]

    for item in points:
        item["linear_predicted"] = round(item["linear_predicted"], 4)
        item["polynomial_predicted"] = round(item["polynomial_predicted"], 4)
        item["ransac_predicted"] = round(item["ransac_predicted"], 4)
        item["predicted"] = round(item["predicted"], 4)
        item["x"] = round(item["x"], 4)
        item["y"] = round(item["y"], 4)

    return {
        "points": points,
        "models": models,
        "train_size": len(train_points),
        "test_size": len(test_points),
        "slope": models[0]["slope"],
        "intercept": models[0]["intercept"],
        "r2": models[0]["r2"],
    }


def train_test_split_points(points, test_ratio=0.2, seed=0):
    indexed = list(range(len(points)))
    state = seed or 1
    for index in range(len(indexed) - 1, 0, -1):
        state = (1103515245 * state + 12345) % (2 ** 31)
        swap_index = state % (index + 1)
        indexed[index], indexed[swap_index] = indexed[swap_index], indexed[index]
    test_size = max(1, int(round(len(points) * test_ratio))) if points else 0
    test_indexes = set(indexed[:test_size])
    for index, point in enumerate(points):
        point["split"] = "test" if index in test_indexes else "train"
    train = [point for index, point in enumerate(points) if index not in test_indexes]
    test = [point for index, point in enumerate(points) if index in test_indexes]
    return train or points, test or points


def fit_linear(points):
    count = float(len(points) or 1)
    mean_x = sum(item["x"] for item in points) / count
    mean_y = sum(item["y"] for item in points) / count
    denominator = sum((item["x"] - mean_x) ** 2 for item in points) or 1.0
    slope = sum((item["x"] - mean_x) * (item["y"] - mean_y) for item in points) / denominator
    intercept = mean_y - slope * mean_x
    return {"slope": slope, "intercept": intercept}


def predict_linear(model, x_value):
    return model.get("slope", 0.0) * x_value + model.get("intercept", 0.0)


def fit_quadratic(points):
    if len(points) < 3:
        linear = fit_linear(points)
        return {"a": 0.0, "b": linear["slope"], "c": linear["intercept"]}

    sx0 = float(len(points))
    sx1 = sum(item["x"] for item in points)
    sx2 = sum(item["x"] ** 2 for item in points)
    sx3 = sum(item["x"] ** 3 for item in points)
    sx4 = sum(item["x"] ** 4 for item in points)
    sy = sum(item["y"] for item in points)
    sxy = sum(item["x"] * item["y"] for item in points)
    sx2y = sum((item["x"] ** 2) * item["y"] for item in points)
    matrix = [
        [sx4, sx3, sx2, sx2y],
        [sx3, sx2, sx1, sxy],
        [sx2, sx1, sx0, sy],
    ]
    solution = solve_linear_system(matrix)
    return {"a": solution[0], "b": solution[1], "c": solution[2]}


def solve_linear_system(matrix):
    size = len(matrix)
    for column in range(size):
        pivot = max(range(column, size), key=lambda row: abs(matrix[row][column]))
        matrix[column], matrix[pivot] = matrix[pivot], matrix[column]
        pivot_value = matrix[column][column]
        if abs(pivot_value) < 1e-12:
            return [0.0 for _ in range(size)]
        for item in range(column, size + 1):
            matrix[column][item] /= pivot_value
        for row in range(size):
            if row == column:
                continue
            factor = matrix[row][column]
            for item in range(column, size + 1):
                matrix[row][item] -= factor * matrix[column][item]
    return [matrix[row][size] for row in range(size)]


def predict_quadratic(model, x_value):
    return model.get("a", 0.0) * x_value * x_value + model.get("b", 0.0) * x_value + model.get("c", 0.0)


def fit_ransac(points):
    if len(points) < 2:
        return fit_linear(points)
    y_values = [item["y"] for item in points]
    threshold = max((max(y_values) - min(y_values)) * 0.08, 8.0)
    best_inliers = []
    best_model = fit_linear(points)
    rng = random.Random(7)
    for _ in range(180):
        left_index, right_index = rng.sample(range(len(points)), 2)
        left = points[left_index]
        right = points[right_index]
        if abs(right["x"] - left["x"]) < 1e-12:
            continue
        slope = (right["y"] - left["y"]) / (right["x"] - left["x"])
        intercept = left["y"] - slope * left["x"]
        model = {"slope": slope, "intercept": intercept}
        inliers = [item for item in points if abs(item["y"] - predict_linear(model, item["x"])) <= threshold]
        if len(inliers) > len(best_inliers):
            best_inliers = inliers
            best_model = model
    if len(best_inliers) >= 2:
        best_model = fit_linear(best_inliers)
    return best_model


def regression_metrics(points, model, predictor):
    if not points:
        return {"mse": 0.0, "r2": 0.0}
    mean_y = sum(item["y"] for item in points) / float(len(points))
    residual = sum((item["y"] - predictor(model, item["x"])) ** 2 for item in points)
    total = sum((item["y"] - mean_y) ** 2 for item in points) or 1.0
    return {
        "mse": round(residual / float(len(points)), 4),
        "r2": round(1 - residual / total, 4),
    }


def build_regression_model(key, name, model, test_points, predictor):
    metrics = regression_metrics(test_points, model, predictor)
    result = {
        "key": key,
        "name": name,
        "mse": metrics["mse"],
        "r2": metrics["r2"],
    }
    if key == "polynomial":
        result.update({
            "a": round(model.get("a", 0.0), 4),
            "b": round(model.get("b", 0.0), 4),
            "c": round(model.get("c", 0.0), 4),
            "formula": "y = %.4fx^2 %s %s" % (
                model.get("a", 0.0),
                signed_term(model.get("b", 0.0), "x"),
                signed_term(model.get("c", 0.0), ""),
            ),
        })
    else:
        result.update({
            "slope": round(model.get("slope", 0.0), 4),
            "intercept": round(model.get("intercept", 0.0), 4),
            "formula": "y = %.4fx %s" % (
                model.get("slope", 0.0),
                signed_term(model.get("intercept", 0.0), ""),
            ),
        })
    return result


def signed_term(value, suffix):
    sign = "+" if value >= 0 else "-"
    return "%s %.4f%s" % (sign, abs(value), suffix)
