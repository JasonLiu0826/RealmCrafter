#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
RealmCrafter 全链路 E2E 测试（重新生成版）

流程：注册/登录 -> 充值 -> 创建设定集与故事 -> 点赞 -> Fork 购买 -> 评论 @ 作者 -> 拉取通知
"""

import os
import sys
import time
import json

# Windows 控制台：避免 Unicode 导致编码错误
if sys.platform == "win32":
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8", errors="replace")

try:
    import requests
except ImportError:
    print("[FAIL] 请先安装: pip install requests")
    sys.exit(1)

# ---------- 配置 ----------
BASE_URL = os.environ.get("E2E_BASE_URL", "http://localhost:8080")
SUFFIX = str(int(time.time()))
ALICE_USER = "e2e_alice_" + SUFFIX
BOB_USER = "e2e_bob_" + SUFFIX
STORY_TITLE = "赛博修仙"
STORY_PRICE = 15   # Lv1 定价上限
BOB_RECHARGE = 1000
NOTIFICATION_POLL_SEC = 2
NOTIFICATION_POLL_ATTEMPTS = 3

def log_ok(msg):
    print("\033[92m[OK] %s\033[0m" % msg)

def log_fail(msg, detail=None):
    print("\033[91m[FAIL] %s\033[0m" % msg)
    if detail:
        print("\033[91m  %s\033[0m" % detail)

def log_step(msg):
    print("\033[93m[>>] %s\033[0m" % msg)

def log_info(msg):
    print("\033[94m[INFO] %s\033[0m" % msg)

def resp_body(resp):
    try:
        return resp.json() if resp.text else {}
    except Exception:
        return {}

def dump(resp):
    return json.dumps(resp_body(resp), ensure_ascii=False, indent=2)

def api_ok(resp, step_name):
    """检查 HTTP 200 且 code==0，失败则打印 body 并抛错"""
    if resp.status_code != 200:
        log_fail("%s: HTTP %s" % (step_name, resp.status_code), dump(resp))
        raise AssertionError("HTTP %s" % resp.status_code)
    body = resp_body(resp)
    code = body.get("code", -1)
    if code != 0:
        log_fail("%s: code=%s msg=%s" % (step_name, code, body.get("message", "")), dump(resp))
        raise AssertionError("code=%s" % code)
    return body

def headers(token=None, user_id=None):
    h = {"Content-Type": "application/json"}
    if token:
        h["Authorization"] = "Bearer %s" % token
    if user_id is not None:
        h["X-User-Id"] = str(user_id)
    return h

def extract_list_from_page(data_node):
    """从 Result.data (Page) 中取出列表。支持 data.content 或 data 本身为 list"""
    if data_node is None:
        return []
    if isinstance(data_node, list):
        return data_node
    if isinstance(data_node, dict):
        return data_node.get("content", [])
    return []


def main():
    print("\n" + "=" * 56)
    print("  RealmCrafter E2E 测试")
    print("=" * 56 + "\n")

    alice_id = bob_id = None
    alice_token = bob_token = None
    setting_id = story_id = bob_fork_id = None

    # ---- 1. 注册与登录 ----
    log_step("1. 注册 Alice")
    r = requests.post(BASE_URL + "/api/v1/auth/register", json={
        "username": ALICE_USER,
        "password": "alice123456",
        "nickname": "Alice",
        "signature": "大作家"
    })
    d = api_ok(r, "注册 Alice")
    alice_id = d["data"]["userId"]
    alice_token = d["data"]["token"]
    log_ok("Alice 注册成功 userId=%s" % alice_id)

    log_step("2. 注册 Bob")
    r = requests.post(BASE_URL + "/api/v1/auth/register", json={
        "username": BOB_USER,
        "password": "bob123456",
        "nickname": "Bob",
        "signature": "土豪读者"
    })
    d = api_ok(r, "注册 Bob")
    bob_id = d["data"]["userId"]
    bob_token = d["data"]["token"]
    log_ok("Bob 注册成功 userId=%s" % bob_id)

    log_step("3. 登录校验")
    r = requests.post(BASE_URL + "/api/v1/auth/login", json={"username": ALICE_USER, "password": "alice123456"})
    api_ok(r, "Alice 登录")
    r = requests.post(BASE_URL + "/api/v1/auth/login", json={"username": BOB_USER, "password": "bob123456"})
    api_ok(r, "Bob 登录")
    log_ok("双方登录正常")

    # ---- 2. 充值 ----
    log_step("4. Bob 充值 1000 水晶（支付回调）")
    r = requests.post(BASE_URL + "/api/v1/payment/wechat/callback", json={
        "userId": bob_id,
        "amount": BOB_RECHARGE,
        "orderId": "e2e-recharge-%s" % SUFFIX
    }, headers={"Content-Type": "application/json"})
    if r.status_code != 200 or (resp_body(r) or {}).get("code") != "SUCCESS":
        log_fail("充值失败", dump(r))
        raise AssertionError("充值失败")
    log_ok("Bob 充值 %s 水晶成功" % BOB_RECHARGE)

    # ---- 3. 创建设定集与故事 ----
    log_step("5. Alice 创建设定集《赛博修仙》")
    r = requests.post(
        BASE_URL + "/api/v1/settings",
        headers=headers(alice_token, alice_id),
        json={
            "title": STORY_TITLE,
            "cover": "",
            "description": "赛博修仙设定",
            "content": {
                "characters": "主角",
                "worldview": "赛博修仙",
                "environment": "未来",
                "mainline": "修仙",
                "plotPoints": "无"
            },
            "allowDownload": True,
            "allowModify": True,
            "price": STORY_PRICE
        }
    )
    d = api_ok(r, "创建设定集")
    setting_id = d["data"]["id"]
    log_ok("设定集 id=%s" % setting_id)

    log_step("6. Alice 创建故事《赛博修仙》")
    r = requests.post(
        BASE_URL + "/api/v1/stories",
        headers=headers(alice_token, alice_id),
        json={
            "userId": alice_id,
            "settingPackId": setting_id,
            "title": STORY_TITLE,
            "cover": "",
            "description": "赛博修仙故事",
            "price": STORY_PRICE
        }
    )
    d = api_ok(r, "创建故事")
    story_id = d["data"]["id"]
    log_ok("故事 id=%s" % story_id)

    # ---- 4. 广场与互动 ----
    log_step("7. Bob 广场刷故事（可选校验）")
    r = requests.get(
        BASE_URL + "/api/v1/square/stories?page=0&size=20&sort=NEWEST",
        headers=headers(bob_token, bob_id)
    )
    if r.status_code == 200 and resp_body(r).get("code") == 0:
        page = (resp_body(r).get("data") or {})
        items = extract_list_from_page(page)
        if any((s.get("id") == story_id or s.get("title") == STORY_TITLE) for s in (items or [])):
            log_ok("广场中可见《赛博修仙》")
        else:
            log_info("广场未找到该故事（可能排序/分页），继续")
    else:
        log_info("广场请求非常规响应，继续后续步骤")

    log_step("8. Bob 点赞")
    r = requests.post(
        BASE_URL + "/api/v1/interactions/like",
        headers=headers(bob_token, bob_id),
        json={"type": "STORY", "id": story_id}
    )
    api_ok(r, "点赞")
    log_ok("点赞成功")

    log_step("9. Bob 购买 Fork（%s 水晶）" % STORY_PRICE)
    r = requests.get(BASE_URL + "/api/v1/auth/profile", headers=headers(bob_token))
    d = api_ok(r, "Bob 购买前 profile")
    bob_before = float((d.get("data") or {}).get("crystalBalance") or 0)

    r = requests.post(
        BASE_URL + "/api/v1/stories/%s/fork" % story_id,
        headers=headers(bob_token, bob_id)
    )
    d = api_ok(r, "Fork")
    bob_fork_id = d["data"]["id"]
    log_ok("Fork 成功 forkId=%s" % bob_fork_id)

    r = requests.get(BASE_URL + "/api/v1/auth/profile", headers=headers(bob_token))
    d = api_ok(r, "Bob 购买后 profile")
    bob_after = float((d.get("data") or {}).get("crystalBalance") or 0)
    if abs((bob_before - STORY_PRICE) - bob_after) > 0.01:
        log_fail("Bob 水晶扣减异常 before=%.2f after=%.2f 应减 %s" % (bob_before, bob_after, STORY_PRICE))
        raise AssertionError("扣款异常")
    log_ok("Bob 扣款正确 余额=%.2f" % bob_after)

    r = requests.get(BASE_URL + "/api/v1/auth/profile", headers=headers(alice_token, alice_id))
    d = api_ok(r, "Alice 收益后 profile")
    alice_crystal = float((d.get("data") or {}).get("crystalBalance") or 0)
    low, high = round(STORY_PRICE * 0.7, 2), round(STORY_PRICE * 0.9, 2)
    if not (low <= alice_crystal <= high):
        log_fail("Alice 收益异常 crystal=%.2f 期望约 [%s, %s]" % (alice_crystal, low, high))
        raise AssertionError("分润异常")
    log_ok("Alice 分润正常 crystal=%.2f" % alice_crystal)

    # ---- 5. 评论与通知 ----
    log_step("10. Bob 评论并 @Alice")
    r = requests.post(
        BASE_URL + "/api/v1/comments",
        headers=headers(bob_token, bob_id),
        json={
            "storyId": bob_fork_id,
            "chapterId": 1,
            "content": "太牛逼了，@%s 快更！" % ALICE_USER,
            "targetType": "PARAGRAPH",
            "targetRef": "0",
            "mentionedUserIds": [alice_id]
        }
    )
    api_ok(r, "发表评论")
    log_ok("评论成功并 @Alice")

    log_step("11. Alice 拉取通知（轮询 %s 次，间隔 %ss）" % (NOTIFICATION_POLL_ATTEMPTS, NOTIFICATION_POLL_SEC))
    notifications = []
    for attempt in range(1, NOTIFICATION_POLL_ATTEMPTS + 1):
        time.sleep(NOTIFICATION_POLL_SEC)
        r = requests.get(
            BASE_URL + "/api/v1/notifications?page=0&size=20",
            headers=headers(alice_token, alice_id)
        )
        d = api_ok(r, "通知列表")
        data_node = d.get("data")
        notifications = extract_list_from_page(data_node)
        if not isinstance(notifications, list):
            notifications = []
        types = [n.get("type") for n in notifications if n.get("type")]
        log_info("第 %s 次拉取: 共 %s 条 类型=%s" % (attempt, len(notifications), types))
        if notifications:
            break

    reward = [n for n in notifications if n.get("type") == "REWARD"]
    mention = [n for n in notifications if n.get("type") == "MENTION"]
    inter = [n for n in notifications if n.get("type") == "INTERACTION"]

    if reward:
        log_ok("REWARD 通知 %s 条" % len(reward))
    else:
        log_fail("未收到 REWARD 通知", "当前 data 结构: %s" % (json.dumps(d.get("data"), ensure_ascii=False)[:300] if d.get("data") else "null"))
        raise AssertionError("缺少 REWARD")

    if mention:
        log_ok("MENTION 通知 %s 条" % len(mention))
    else:
        log_fail("未收到 MENTION 通知")
        raise AssertionError("缺少 MENTION")

    if inter:
        log_ok("INTERACTION 通知 %s 条" % len(inter))
    else:
        log_info("无 INTERACTION 通知（视后端实现而定）")

    print("\n" + "=" * 56)
    print("  E2E 测试全部通过")
    print("=" * 56 + "\n")


if __name__ == "__main__":
    try:
        main()
    except AssertionError:
        print("\n\033[91mE2E 未通过，请根据上方输出排查。\033[0m\n")
        sys.exit(1)
    except requests.exceptions.ConnectionError as e:
        log_fail("无法连接 %s" % BASE_URL, str(e))
        sys.exit(1)
    except Exception as e:
        log_fail("异常", str(e))
        raise
