# -*- coding: utf-8 -*-
import json
import sys

from algorithms.association import association
from algorithms.classification import classification
from algorithms.clustering import clustering
from algorithms.common import binary_type
from algorithms.regression import regression


HANDLERS = {
    "association": association,
    "clustering": clustering,
    "classification": classification,
    "regression": regression,
}


def main():
    """读取一个 JSON 请求文件，分发到对应算法，并把 JSON 结果写到标准输出。"""
    with open(sys.argv[1], "rb") as file_obj:
        raw_request = file_obj.read()
    if isinstance(raw_request, binary_type):
        raw_request = raw_request.decode("utf-8-sig")
    request = json.loads(raw_request)
    operation = request.get("operation")
    payload = request.get("payload") or {}
    if operation not in HANDLERS:
        raise ValueError("Unknown operation: %s" % operation)

    # Spring 会把标准输出当作算法响应读取，所以这里必须只输出 JSON。
    sys.stdout.write(json.dumps(HANDLERS[operation](payload), ensure_ascii=True))


if __name__ == "__main__":
    main()
