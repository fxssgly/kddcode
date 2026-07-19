# -*- coding: utf-8 -*-
"""文件作用：实现 CART 风格决策树分类。

项目位置：backend/python/algorithms 算法模块之一，对应前端“分类分析”页面。
交互关系：接收 DatasetService 准备的 Iris 行数据，返回带预测标签的数据、树结构和评估指标。
"""

from algorithms.common import FEATURES, as_float, as_text


def entropy(groups):
    """计算候选划分的加权信息熵，用于 ID3 决策树。"""
    import math

    total = sum(len(group) for group in groups) or 1
    score = 0.0
    for group in groups:
        if not group:
            continue
        counts = {}
        for row in group:
            species = row.get("species", "unknown")
            counts[species] = counts.get(species, 0) + 1
        group_score = 0.0
        for count in counts.values():
            ratio = count / float(len(group))
            if ratio:
                group_score -= ratio * math.log(ratio, 2)
        score += group_score * (len(group) / float(total))
    return score


def gini(groups):
    """计算候选划分的加权基尼不纯度。"""
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
    """返回一组数据中出现次数最多的类别标签。"""
    counts = {}
    for row in rows:
        species = row.get("species", "unknown")
        counts[species] = counts.get(species, 0) + 1
    return max(counts, key=counts.get) if counts else "unknown"


def class_counts(rows, class_names):
    """按固定类别顺序返回计数，方便前端展示决策树。"""
    counts = {}
    for row in rows:
        species = row.get("species", "unknown")
        counts[species] = counts.get(species, 0) + 1
    return [counts.get(name, 0) for name in class_names]


def split_score(groups, criterion):
    """根据算法类型选择划分评价指标。"""
    if criterion == "entropy":
        return entropy(groups)
    return gini(groups)


def node_impurity(rows, criterion="gini"):
    """计算单个节点不纯度的便捷封装。"""
    return split_score([rows], criterion)


def node_payload(rows, class_names, name, depth, criterion="gini"):
    """构建每个树节点上保存的可序列化元数据。"""
    counts = class_counts(rows, class_names)
    class_name = majority(rows)
    return {
        "name": name,
        "criterion": criterion,
        "impurity": round(node_impurity(rows, criterion), 3),
        "samples": len(rows),
        "value": counts,
        "className": class_name,
        "depth": depth,
    }


def rows_to_matrix(rows):
    """把 Iris 行数据转换成 scikit-learn 需要的 X、y。

    X 是 4 个连续特征组成的二维数组；y 是 species 类别标签。这个函数只
    服务于实验要求中的 DecisionTreeClassifier，不影响手写 CART 逻辑。
    """
    x_values = []
    y_values = []
    for row in rows:
        x_values.append([as_float(row.get(feature)) for feature in FEATURES])
        y_values.append(row.get("species", "unknown"))
    return x_values, y_values


def ordered_sklearn_counts(raw_counts, estimator_classes, class_names, samples):
    """把 sklearn 节点中的类别计数转换成前端固定类别顺序。

    不同 scikit-learn 版本中 tree_.value 可能表现为计数或比例；这里统一
    转成 class_names 顺序下的整数计数，保证前端 value=[...] 的含义不变。
    """
    values = [float(item) for item in raw_counts]
    if values and abs(sum(values) - 1.0) < 1e-6:
        values = [item * samples for item in values]
    counts_by_class = {}
    for index, class_name in enumerate(estimator_classes):
        counts_by_class[str(class_name)] = int(round(values[index])) if index < len(values) else 0
    return [counts_by_class.get(name, 0) for name in class_names]


def sklearn_tree_payload(estimator, node_id, depth, class_names):
    """把 DecisionTreeClassifier 训练出的 tree_ 转成前端树节点。

    实验要求中的“ID3”使用 criterion='entropy'，但 scikit-learn 的
    DecisionTreeClassifier 仍然采用 CART 二叉树结构，所以内部节点名称
    会是 feature <= threshold；这正是实验要求对应的可视化结果。
    """
    tree_model = estimator.tree_
    left_child = tree_model.children_left[node_id]
    right_child = tree_model.children_right[node_id]
    samples = int(tree_model.n_node_samples[node_id])
    counts = ordered_sklearn_counts(tree_model.value[node_id][0], estimator.classes_, class_names, samples)
    best_index = counts.index(max(counts)) if counts else 0
    class_name = class_names[best_index] if best_index < len(class_names) else "unknown"
    is_leaf = left_child == right_child
    if is_leaf:
        name = class_name
    else:
        feature = FEATURES[tree_model.feature[node_id]]
        threshold = float(tree_model.threshold[node_id])
        name = "%s <= %.2f" % (feature, threshold)

    node = {
        "name": name,
        "criterion": "entropy",
        "impurity": round(float(tree_model.impurity[node_id]), 3),
        "samples": samples,
        "value": counts,
        "className": class_name,
        "depth": depth,
    }
    if not is_leaf:
        node.update({
            "feature": FEATURES[tree_model.feature[node_id]],
            "threshold": round(float(tree_model.threshold[node_id]), 3),
            "children": [
                sklearn_tree_payload(estimator, left_child, depth + 1, class_names),
                sklearn_tree_payload(estimator, right_child, depth + 1, class_names),
            ],
        })
    return node


def build_id3_tree(rows, max_depth, min_leaf, class_names):
    """按实验要求训练 entropy 决策树并用 GridSearchCV 调参。

    这里的 ID3 按课程实验口径实现：DecisionTreeClassifier 使用
    criterion='entropy'，GridSearchCV 使用 5 折交叉验证搜索 max_depth
    和 min_samples_leaf。返回值包含前端可画的树和最优参数。
    """
    from sklearn.model_selection import GridSearchCV
    from sklearn.tree import DecisionTreeClassifier

    x_values, y_values = rows_to_matrix(rows)
    depth_limit = max(1, int(max_depth))
    leaf_limit = max(1, int(min_leaf))
    param_grid = {
        "max_depth": list(range(1, max(8, depth_limit) + 1)),
        "min_samples_leaf": list(range(1, max(10, leaf_limit) + 1)),
    }
    search = GridSearchCV(
        DecisionTreeClassifier(criterion="entropy", random_state=123),
        param_grid,
        cv=5,
    )
    search.fit(x_values, y_values)
    tree = sklearn_tree_payload(search.best_estimator_, 0, 0, class_names)
    return tree, {
        "max_depth": int(search.best_params_["max_depth"]),
        "min_samples_leaf": int(search.best_params_["min_samples_leaf"]),
        "best_score": round(float(search.best_score_), 4),
    }


def build_tree(rows, depth, max_depth, min_leaf, class_names, criterion="gini"):
    """递归构建 CART 风格的二叉决策树。"""
    labels = set(row.get("species", "unknown") for row in rows)
    if depth >= max_depth or len(rows) <= min_leaf or len(labels) == 1:
        return node_payload(rows, class_names, majority(rows), depth, criterion)

    # 尝试每个特征阈值，并保留基尼不纯度最低的划分。
    best = None
    for feature in FEATURES:
        values = sorted(set(as_float(row.get(feature)) for row in rows))
        for threshold in values[1:]:
            left = [row for row in rows if as_float(row.get(feature)) <= threshold]
            right = [row for row in rows if as_float(row.get(feature)) > threshold]
            if len(left) < min_leaf or len(right) < min_leaf:
                continue
            score = split_score([left, right], criterion)
            if best is None or score < best["score"]:
                best = {
                    "feature": feature,
                    "threshold": threshold,
                    "left": left,
                    "right": right,
                    "score": score,
                }

    if best is None:
        return node_payload(rows, class_names, majority(rows), depth, criterion)

    # 同时保存展示字段和机器可读字段，方便前端可视化。
    name = "%s <= %.2f" % (best["feature"], best["threshold"])
    node = node_payload(rows, class_names, name, depth, criterion)
    node.update({
        "feature": best["feature"],
        "threshold": round(best["threshold"], 3),
        "children": [
            build_tree(best["left"], depth + 1, max_depth, min_leaf, class_names, criterion),
            build_tree(best["right"], depth + 1, max_depth, min_leaf, class_names, criterion),
        ],
    })
    return node


def predict_tree(node, row):
    """沿着决策树向下查找，直到到达叶子类别。"""
    if "children" not in node:
        return node["name"]
    feature, raw_threshold = node["name"].split(" <= ")
    child_index = 0 if as_float(row.get(feature)) <= float(raw_threshold) else 1
    return predict_tree(node["children"][child_index], row)


def clean_classification_rows(rows):
    """移除不可用标签，并用均值填补无效数值。"""
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
    # 均值填补可以让演示模型在上传数据不完美时仍然运行。
    for row in cleaned:
        for name in FEATURES:
            if row.get(name) is None:
                row[name] = round(means[name], 4)
    return cleaned


def train_test_split_rows(rows, test_ratio=0.2, seed=123):
    """不依赖外部库，生成可复现的训练集和测试集划分。"""
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
    """在测试集上计算宏平均 precision、recall、F1 和准确率。"""
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
    """执行完整分类流程，并返回数据行、决策树和评估指标。"""
    rows = clean_classification_rows([dict(row) for row in (payload.get("rows") or [])])
    max_depth = int(as_float(payload.get("max_depth"), 3))
    min_leaf = int(as_float(payload.get("min_leaf"), 2))
    train_rows, test_rows = train_test_split_rows(rows)
    class_names = sorted(set(row.get("species", "unknown") for row in rows))
    tree = build_tree(train_rows, 0, max_depth, min_leaf, class_names, "gini")
    id3_tree, id3_best_params = build_id3_tree(train_rows, max_depth, min_leaf, class_names)
    for row in rows:
        predicted = predict_tree(tree, row)
        row["predicted"] = predicted
    metrics = classification_metrics(test_rows)
    return {
        "rows": rows,
        "tree": tree,
        "id3_tree": id3_tree,
        "trees": {
            "cart": tree,
            "id3": id3_tree,
        },
        "accuracy": metrics["accuracy"],
        "precision": metrics["precision"],
        "recall": metrics["recall"],
        "f1": metrics["f1"],
        "train_size": len(train_rows),
        "test_size": len(test_rows),
        "id3_best_params": id3_best_params,
    }
