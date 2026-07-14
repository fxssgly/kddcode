# -*- coding: utf-8 -*-
import itertools
import json
import math
import sys

FEATURES = ["sepal_length", "sepal_width", "petal_length", "petal_width"]


def as_float(value, default=0.0):
    try:
        return float(value)
    except (TypeError, ValueError):
        return default


def association(payload):
    transactions = payload.get("transactions") or []
    min_support = as_float(payload.get("min_support"), 0.2)
    min_confidence = as_float(payload.get("min_confidence"), 0.4)
    total = float(len(transactions) or 1)
    itemsets = {}

    for transaction in transactions:
        values = sorted(set([str(item) for item in transaction if str(item).strip()]))
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

    rules = []
    for items, count in itemsets.items():
        if len(items) < 2:
            continue
        support = count / total
        if support < min_support:
            continue
        values = list(items)
        for left_size in range(1, len(values)):
            for left in itertools.combinations(values, left_size):
                right = tuple(sorted(set(values) - set(left)))
                left_count = itemsets.get(tuple(sorted(left)), 0)
                confidence = count / float(left_count or 1)
                if confidence >= min_confidence:
                    rules.append({
                        "left": list(left),
                        "right": list(right),
                        "support": round(support, 4),
                        "confidence": round(confidence, 4),
                    })

    rules.sort(key=lambda item: (item["confidence"], item["support"]), reverse=True)
    return {
        "transactions": transactions,
        "total": len(transactions),
        "frequent": frequent[:20],
        "rules": rules[:20],
    }


def distance(row, center):
    return math.sqrt(sum((as_float(row[name]) - center[name]) ** 2 for name in FEATURES))


def clustering(payload):
    rows = [dict(row) for row in (payload.get("rows") or [])]
    if not rows:
        return {"rows": [], "centers": []}
    k = max(1, min(int(as_float(payload.get("k"), 3)), len(rows)))
    centers = []
    for row in rows[:k]:
        centers.append({name: as_float(row[name]) for name in FEATURES})

    for _ in range(20):
        groups = [[] for _ in range(k)]
        for row in rows:
            cluster_index = min(range(k), key=lambda index: distance(row, centers[index]))
            row["cluster"] = cluster_index
            groups[cluster_index].append(row)
        new_centers = []
        for index, group in enumerate(groups):
            if not group:
                new_centers.append(centers[index])
            else:
                new_centers.append({
                    name: sum(as_float(row[name]) for row in group) / len(group)
                    for name in FEATURES
                })
        if new_centers == centers:
            break
        centers = new_centers

    return {"rows": rows, "centers": centers}


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


def classification(payload):
    rows = [dict(row) for row in (payload.get("rows") or [])]
    max_depth = int(as_float(payload.get("max_depth"), 3))
    min_leaf = int(as_float(payload.get("min_leaf"), 2))
    tree = build_tree(rows, 0, max_depth, min_leaf)
    correct = 0
    for row in rows:
        predicted = predict_tree(tree, row)
        row["predicted"] = predicted
        if predicted == row.get("species"):
            correct += 1
    accuracy = correct / float(len(rows) or 1)
    return {"rows": rows, "tree": tree, "accuracy": round(accuracy, 4)}


def regression(payload):
    rows = payload.get("rows") or []
    x_field = payload.get("x_field") or "petal_length"
    y_field = payload.get("y_field") or "petal_width"
    points = []
    for row in rows:
        points.append({
            "x": as_float(row.get(x_field)),
            "y": as_float(row.get(y_field)),
            "species": row.get("species", "unknown"),
        })

    count = float(len(points) or 1)
    mean_x = sum(item["x"] for item in points) / count
    mean_y = sum(item["y"] for item in points) / count
    denominator = sum((item["x"] - mean_x) ** 2 for item in points) or 1.0
    slope = sum((item["x"] - mean_x) * (item["y"] - mean_y) for item in points) / denominator
    intercept = mean_y - slope * mean_x
    for item in points:
        item["predicted"] = slope * item["x"] + intercept
    ss_total = sum((item["y"] - mean_y) ** 2 for item in points) or 1.0
    ss_residual = sum((item["y"] - item["predicted"]) ** 2 for item in points)
    r2 = 1 - ss_residual / ss_total
    return {
        "points": points,
        "slope": round(slope, 4),
        "intercept": round(intercept, 4),
        "r2": round(r2, 4),
    }


def main():
    with open(sys.argv[1], "r") as file_obj:
        request = json.load(file_obj)
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
