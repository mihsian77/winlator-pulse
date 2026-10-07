#!/usr/bin/env python3
"""自动同步官方上游时创建或更新 PR 的脚本"""
import os, json, urllib.request

def main():
    token = os.environ.get("PAT_TOKEN") or os.environ.get("GITHUB_TOKEN")
    if not token:
        print("错误: 未找到 PAT_TOKEN 环境变量")
        return 1

    repo = os.environ.get("GITHUB_REPOSITORY", "")
    branch = os.environ.get("SYNC_BRANCH", "")
    upstream_msg = os.environ.get("UPSTREAM_MSG", "")

    if not repo or not branch:
        print("错误: 缺少 GITHUB_REPOSITORY 或 SYNC_BRANCH")
        return 1

    body = """## 自动同步官方上游 brunodev85/winlator-app

此 PR 由定时工作流自动创建，同步官方 Winlator 最新 Java 源码和 res 资源。

### 同步范围
- Java 源码（app/src/main/java/）
- 资源文件（app/src/main/res/）
- 不同步 native 层（cpp/，Pulse 有共存定制）
- 不同步组件包（assets/，Pulse 自选版本）

### 保护文件
Pulse 原创/共存定制文件已排除：XRandR、后台安装、进度对话框、共存核心、中文资源、构建配置。

### 说明
- 此 PR 会自动触发 CI 构建验证
- 构建绿色后再合并

### 官方最近提交
```
""" + upstream_msg + """
```
"""

    headers = {
        "Authorization": "token " + token,
        "Content-Type": "application/json",
        "Accept": "application/vnd.github+json"
    }

    owner = repo.split("/")[0]
    url = "https://api.github.com/repos/" + repo + "/pulls?state=open&head=" + owner + ":" + branch
    req = urllib.request.Request(url, headers=headers)

    try:
        with urllib.request.urlopen(req) as resp:
            existing = json.loads(resp.read())
    except Exception as e:
        print("查询 PR 失败:", e)
        return 1

    if existing:
        pr_number = existing[0]["number"]
        print("更新已有 PR #" + str(pr_number))
        data = json.dumps({"title": "sync: 同步官方上游（自动生成）", "body": body}).encode()
        url = "https://api.github.com/repos/" + repo + "/pulls/" + str(pr_number)
        req = urllib.request.Request(url, data=data, headers=headers, method="PATCH")
    else:
        print("创建新 PR")
        data = json.dumps({
            "title": "sync: 同步官方上游（自动生成）",
            "head": branch,
            "base": "main",
            "body": body
        }).encode()
        url = "https://api.github.com/repos/" + repo + "/pulls"
        req = urllib.request.Request(url, data=data, headers=headers, method="POST")

    try:
        with urllib.request.urlopen(req) as resp:
            result = json.loads(resp.read())
            print("PR 操作完成: #" + str(result.get("number", "unknown")))
            print("URL: " + result.get("html_url", ""))
    except Exception as e:
        print("创建/更新 PR 失败:", e)
        return 1

    return 0

if __name__ == "__main__":
    exit(main())
