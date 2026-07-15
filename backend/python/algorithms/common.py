# -*- coding: utf-8 -*-

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
