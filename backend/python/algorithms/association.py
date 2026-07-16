# -*- coding: utf-8 -*-
"""文件作用：实现关联规则挖掘算法。

项目位置：backend/python/algorithms 算法模块之一，由 kdd_algorithms.py 根据 association 操作调用。
交互关系：输入来自 Java 传入的事务篮子和阈值，输出频繁项集、二项指标和规则列表给前端热力图/表格使用。
"""
import itertools

from algorithms.common import as_float, as_text


def association(payload):
    """挖掘频繁项集和简单的二项关联规则。"""
    transactions = payload.get("transactions") or []
    min_support = as_float(payload.get("min_support"), 0.2)
    min_confidence = as_float(payload.get("min_confidence"), 0.4)
    total = float(len(transactions) or 1)
    itemsets = {}

    # 统计每个事务篮子中所有 1 项、2 项和 3 项组合的出现次数。
    for transaction in transactions:
        values = sorted(set([as_text(item) for item in transaction if as_text(item).strip()]))
        for size in (1, 2, 3):
            for combo in itertools.combinations(values, size):
                itemsets[combo] = itemsets.get(combo, 0) + 1

    # 保留出现比例达到支持度阈值的项集。
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

    # 二项指标更适合前端图表展示：每个二项组合的支持度、置信度和提升度。
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

    # 对每个频繁二项组合生成 A -> B 和 B -> A 两个方向的规则。
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
