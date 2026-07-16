"""文件作用：声明 algorithms 是一个 Python 包。

项目位置：backend/python/algorithms 包入口。
交互关系：让 kdd_algorithms.py 可以用 algorithms.xxx 的形式导入各个算法模块。
"""
"""KDD 后端使用的算法模块。

每个模块暴露一个算法入口函数。Java 后端会调用 kdd_algorithms.py，
再由它分发到这些模块并返回 JSON。
"""
