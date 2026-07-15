# 数据挖掘与分析综合实验平台运行说明

## 1. 环境要求

- JDK 11 或更高版本
- Maven 3.6 或更高版本
- Node.js 16 或更高版本
- Python 3

说明：系统默认不依赖 MySQL，会直接读取 `backend/data` 下的 CSV 数据集。

## 2. 解压项目

将压缩包解压到一个英文路径，例如：

```powershell
D:\kddcode
```

后续命令都在解压后的项目目录中执行。

## 3. 启动后端

打开一个终端：

```powershell
cd D:\kddcode\backend
mvn spring-boot:run
```

后端默认端口是：

```text
http://127.0.0.1:5000
```

如果电脑上的 `python` 不是 Python 3，可以先设置 Python 3 路径，例如：

```powershell
$env:KDD_PYTHON="C:\Users\你的用户名\AppData\Local\Programs\Python\Python310\python.exe"
mvn spring-boot:run
```

## 4. 启动前端

再打开一个新的终端：

```powershell
cd D:\kddcode\frontend
npm install
npm run serve
```

前端默认地址是：

```text
http://127.0.0.1:8080
```

## 5. 登录系统

默认账号：

```text
admin
```

默认密码：

```text
123456
```

## 6. 回归分析页面说明

回归分析默认数据集为：

```text
backend/data/regression_experiment.csv
```

页面会显示：

- 数据表格
- 一元线性回归、一元二次多项式回归、RANSAC 的指标表格
- 三张独立拟合图

也可以上传自己的 CSV。CSV 至少需要包含：

```csv
id,x,y,type
1,5.991187,-41.931481,噪声点
2,4.050021,-50.655763,噪声点
```

其中 `id` 和 `type` 可以不填；`x` 和 `y` 必须存在。

## 7. 常见问题

如果前端页面没有数据，请确认：

1. 后端终端没有报错。
2. 后端端口是 `5000`。
3. 前端终端正在运行 `npm run serve`。
4. 浏览器访问的是 `http://127.0.0.1:8080`。

如果 Maven 或 npm 下载很慢，可以换国内镜像源后再运行。
