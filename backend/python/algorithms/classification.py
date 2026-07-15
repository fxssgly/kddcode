# -*- coding: utf-8 -*-

from algorithms.common import FEATURES, as_float, as_text


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
