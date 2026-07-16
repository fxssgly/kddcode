# -*- coding: utf-8 -*-
"""文件作用：提供多个 Python 算法共享的小工具。

项目位置：backend/python/algorithms 公共模块，不直接被前端调用。
交互关系：关联、聚类、分类、回归模块都会复用这里的字段列表和类型转换函数。
"""

FEATURES = ["sepal_length", "sepal_width", "petal_length", "petal_width"]

# 兼容 Python 2 风格类型名和 Python 3 类型名。
try:
    text_type = unicode
    binary_type = str
except NameError:
    text_type = str
    binary_type = bytes


def as_float(value, default=0.0):
    """把用户输入或 CSV 值安全转换为浮点数。"""
    try:
        return float(value)
    except (TypeError, ValueError):
        return default


def as_text(value):
    """把文本或字节转换成适合展示的字符串。"""
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
