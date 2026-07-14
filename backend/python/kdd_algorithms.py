# -*- coding: utf-8 -*-
import itertools
import json
import math
import random
import sys

FEATURES = ["sepal_length", "sepal_width", "petal_length", "petal_width"]

try:
    text_type = unicode
    binary_type = str
except NameError:
    text_type = str
    binary_type = bytes


def as_float(value, default=0.0):
    try:
        return float(value)
    except (TypeError, ValueError):
        return default


def as_text(value):
    if value is None:
        return ""
    if isinstance(value, text_type):
        return value
    if isinstance(value, binary_type):
        for encoding in ("utf-8", "gbk"):
            try:
                return value.decode(encoding)
            except UnicodeError:
                pass
    return text_type(value)


def association(payload):
    transactions = payload.get("transactions") or []
    min_support = as_float(payload.get("min_support"), 0.2)
    min_confidence = as_float(payload.get("min_confidence"), 0.4)
    total = float(len(transactions) or 1)
    itemsets = {}

    for transaction in transactions:
        values = sorted(set([as_text(item) for item in transaction if as_text(item).strip()]))
        for size in (1, 2, 3):
            for combo in itertools.combinations(values, size):
                itemsets[combo] = itemsets.get(combo, 0) + 1

    frequent = []
    for items, count in itemsets.items():
        support = count / total
        if support >= min_support:
            frequent.append({
                "items": list(items),
                "count": count,
                "support": round(support, 4),
            })
    frequent.sort(key=lambda item: (item["support"], item["count"]), reverse=True)

    pair_metrics = []
    for items, count in itemsets.items():
        if len(items) != 2:
            continue
        support = count / total
        if support < min_support:
            continue
        left, right = items
        left_support = itemsets.get((left,), 0) / total
        right_support = itemsets.get((right,), 0) / total
        confidence_lr = support / float(left_support or 1)
        confidence_rl = support / float(right_support or 1)
        lift = support / float((left_support or 1) * (right_support or 1))
        pair_metrics.append({
            "items": list(items),
            "count": count,
            "support": round(support, 4),
            "confidence": round(max(confidence_lr, confidence_rl), 4),
            "lift": round(lift, 4),
        })
    pair_metrics.sort(key=lambda item: (item["support"], item["lift"]), reverse=True)

    rules = []
    for items, count in itemsets.items():
        if len(items) != 2:
            continue
        support = count / total
        if support < min_support:
            continue
        values = list(items)
        for left, right in ((values[0], values[1]), (values[1], values[0])):
            left_count = itemsets.get((left,), 0)
            right_count = itemsets.get((right,), 0)
            confidence = count / float(left_count or 1)
            right_support = right_count / total
            lift = confidence / float(right_support or 1)
            if confidence >= min_confidence:
                rules.append({
                    "left": [left],
                    "right": [right],
                    "support": round(support, 4),
                    "confidence": round(confidence, 4),
                    "lift": round(lift, 4),
                })

    rules.sort(key=lambda item: (item["confidence"], item["lift"], item["support"]), reverse=True)
    return {
        "transactions": transactions,
        "total": len(transactions),
        "frequent": frequent[:20],
        "frequent_pairs": pair_metrics[:20],
        "pair_metrics": pair_metrics,
        "rules": rules[:20],
    }


def distance(row, center):
    return math.sqrt(sum((as_float(row[name]) - center[name]) ** 2 for name in FEATURES))


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


def gini(groups):
    total = sum(len(group) for group in groups) or 1
    score = 0.0
    for group in groups:
        if not group:
            continue
        counts = {}
        for row in group:
            species = row.get("species", "unknown")
            counts[species] = counts.get(species, 0) + 1
        group_score = 1.0
        for count in counts.values():
            ratio = count / float(len(group))
            group_score -= ratio * ratio
        score += group_score * (len(group) / float(total))
    return score


def majority(rows):
    counts = {}
    for row in rows:
        species = row.get("species", "unknown")
        counts[species] = counts.get(species, 0) + 1
    return max(counts, key=counts.get) if counts else "unknown"


def build_tree(rows, depth, max_depth, min_leaf):
    labels = set(row.get("species", "unknown") for row in rows)
    if depth >= max_depth or len(rows) <= min_leaf or len(labels) == 1:
        return {"name": majority(rows), "value": len(rows)}

    best = None
    for feature in FEATURES:
        values = sorted(set(as_float(row.get(feature)) for row in rows))
        for threshold in values[1:]:
            left = [row for row in rows if as_float(row.get(feature)) <= threshold]
            right = [row for row in rows if as_float(row.get(feature)) > threshold]
            if len(left) < min_leaf or len(right) < min_leaf:
                continue
            score = gini([left, right])
            if best is None or score < best["score"]:
                best = {
                    "feature": feature,
                    "threshold": threshold,
                    "left": left,
                    "right": right,
                    "score": score,
                }

    if best is None:
        return {"name": majority(rows), "value": len(rows)}

    name = "%s <= %.2f" % (best["feature"], best["threshold"])
    return {
        "name": name,
        "children": [
            build_tree(best["left"], depth + 1, max_depth, min_leaf),
            build_tree(best["right"], depth + 1, max_depth, min_leaf),
        ],
    }


def predict_tree(node, row):
    if "children" not in node:
        return node["name"]
    feature, raw_threshold = node["name"].split(" <= ")
    child_index = 0 if as_float(row.get(feature)) <= float(raw_threshold) else 1
    return predict_tree(node["children"][child_index], row)


def clean_classification_rows(rows):
    cleaned = []
    numeric_values = {name: [] for name in FEATURES}
    for row in rows:
        species = as_text(row.get("species", "")).strip()
        if not species or species == "unknown":
            continue
        clean_row = dict(row)
        for name in FEATURES:
            value = as_float(row.get(name))
            if name == "sepal_length" and (value > 30 or value <= 0):
                clean_row[name] = None
            elif name == "petal_length" and value <= 0:
                clean_row[name] = None
            elif value <= 0:
                clean_row[name] = None
            else:
                clean_row[name] = value
                numeric_values[name].append(value)
        clean_row["species"] = species
        cleaned.append(clean_row)
    means = {
        name: (sum(values) / float(len(values))) if values else 0.0
        for name, values in numeric_values.items()
    }
    for row in cleaned:
        for name in FEATURES:
            if row.get(name) is None:
                row[name] = round(means[name], 4)
    return cleaned


def train_test_split_rows(rows, test_ratio=0.2, seed=123):
    # Deterministic lightweight shuffle matching the experiment's fixed random_state spirit.
    indexed = list(range(len(rows)))
    state = seed
    for index in range(len(indexed) - 1, 0, -1):
        state = (1103515245 * state + 12345) % (2 ** 31)
        swap_index = state % (index + 1)
        indexed[index], indexed[swap_index] = indexed[swap_index], indexed[index]
    test_size = max(1, int(round(len(rows) * test_ratio)))
    test_indexes = set(indexed[:test_size])
    train = [row for index, row in enumerate(rows) if index not in test_indexes]
    test = [row for index, row in enumerate(rows) if index in test_indexes]
    for index, row in enumerate(rows):
        row["split"] = "test" if index in test_indexes else "train"
    return train, test


def classification_metrics(rows):
    labels = sorted(set([row.get("species", "unknown") for row in rows] + [row.get("predicted", "unknown") for row in rows]))
    precisions = []
    recalls = []
    f1_scores = []
    correct = 0
    for label in labels:
        tp = sum(1 for row in rows if row.get("species") == label and row.get("predicted") == label)
        fp = sum(1 for row in rows if row.get("species") != label and row.get("predicted") == label)
        fn = sum(1 for row in rows if row.get("species") == label and row.get("predicted") != label)
        precision = tp / float(tp + fp) if (tp + fp) else 0.0
        recall = tp / float(tp + fn) if (tp + fn) else 0.0
        f1 = (2 * precision * recall / float(precision + recall)) if (precision + recall) else 0.0
        precisions.append(precision)
        recalls.append(recall)
        f1_scores.append(f1)
    for row in rows:
        if row.get("species") == row.get("predicted"):
            correct += 1
    count = float(len(rows) or 1)
    return {
        "accuracy": round(correct / count, 4),
        "precision": round(sum(precisions) / float(len(precisions) or 1), 4),
        "recall": round(sum(recalls) / float(len(recalls) or 1), 4),
        "f1": round(sum(f1_scores) / float(len(f1_scores) or 1), 4),
    }


def classification(payload):
    rows = clean_classification_rows([dict(row) for row in (payload.get("rows") or [])])
    max_depth = int(as_float(payload.get("max_depth"), 3))
    min_leaf = int(as_float(payload.get("min_leaf"), 2))
    train_rows, test_rows = train_test_split_rows(rows)
    tree = build_tree(train_rows, 0, max_depth, min_leaf)
    for row in rows:
        predicted = predict_tree(tree, row)
        row["predicted"] = predicted
    metrics = classification_metrics(test_rows)
    return {
        "rows": rows,
        "tree": tree,
        "accuracy": metrics["accuracy"],
        "precision": metrics["precision"],
        "recall": metrics["recall"],
        "f1": metrics["f1"],
        "train_size": len(train_rows),
        "test_size": len(test_rows),
    }


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
            "type": row.get("type", row.get("species", "样本")),
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
        build_regression_model("linear", "一元线性回归", linear, test_points, predict_linear),
        build_regression_model("polynomial", "一元二次多项式回归", polynomial, test_points, predict_quadratic),
        build_regression_model("ransac", "RANSAC 稳健回归", ransac, test_points, predict_linear),
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
            "formula": "y = %.4fx² %s %s" % (
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


def main():
    with open(sys.argv[1], "rb") as file_obj:
        raw_request = file_obj.read()
    if isinstance(raw_request, binary_type):
        raw_request = raw_request.decode("utf-8-sig")
    request = json.loads(raw_request)
    operation = request.get("operation")
    payload = request.get("payload") or {}
    handlers = {
        "association": association,
        "clustering": clustering,
        "classification": classification,
        "regression": regression,
    }
    if operation not in handlers:
        raise ValueError("Unknown operation: %s" % operation)
    sys.stdout.write(json.dumps(handlers[operation](payload), ensure_ascii=True))


if __name__ == "__main__":
    main()
