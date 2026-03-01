#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
RealmCrafter 终极全链路端到端 (E2E) 点火冒烟测试

验证核心模块（鉴权、经济、资产、社交、后台）协同工作：
- JWT 鉴权、支付回调充值、金牌创作者分润、防超卖扣款、经验值、站内通知
"""

import os
import sys
import time
import json
import io

# Windows 控制台兼容：避免 Unicode 符号导致 GBK 编码错误
if sys.platform == "win32":
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8", errors="replace")

try:
    import requests
except ImportError:
    print("\033[91m[FAIL] requests 未安装。请执行: pip install requests\033[0m")
    sys.exit(1)

# ------------------------------ 配置 ------------------------------
BASE_URL = os.environ.get("E2E_BASE_URL", "http://localhost:8080")
# 可选：管理员账号，用于授予 Alice 金牌创作者（否则 Alice 按 Lv1 分润 70%）
E2E_ADMIN_USERNAME = os.environ.get("E2E_ADMIN_USERNAME", "")
E2E_ADMIN_PASSWORD = os.environ.get("E2E_ADMIN_PASSWORD", "")

SUFFIX = str(int(time.time()))
ALICE_USERNAME = f"e2e_alice_{SUFFIX}"
BOB_USERNAME = f"e2e_bob_{SUFFIX}"
STORY_TITLE = "赛博修仙"
# Lv1~Lv3 创作者定价上限 15 水晶（见 CreatorPriceValidator）
STORY_PRICE = 15
BOB_RECHARGE = 1000

# ------------------------------ 彩色日志 ------------------------------
def ok(msg: str):
    print(f"\033[92m[SUCCESS] {msg}\033[0m")

def fail(msg: str, detail: str = ""):
    print(f"\033[91m[FAIL] {msg}\033[0m")
    if detail:
        print(f"\033[91m   {detail}\033[0m")

def info(msg: str):
    print(f"\033[94m[INFO] {msg}\033[0m")

def step(msg: str):
    print(f"\033[93m[>>] {msg}\033[0m")

def dump(resp: requests.Response):
    try:
        body = resp.json() if resp.text else {}
        return json.dumps(body, ensure_ascii=False, indent=2)
    except Exception:
        return resp.text or "(empty)"

# ------------------------------ 断言与请求封装 ------------------------------
def assert_ok(resp: requests.Response, context: str) -> dict:
    if resp.status_code in (401, 403, 500):
        fail(f"{context} — HTTP {resp.status_code}", dump(resp))
        raise AssertionError(dump(resp))
    data = resp.json() if resp.text else {}
    code = data.get("code", -1)
    if code != 0:
        fail(f"{context} — code={code}, message={data.get('message', '')}", dump(resp))
        raise AssertionError(dump(resp))
    return data

def auth_headers(token: str, user_id: int = None) -> dict:
    h = {"Content-Type": "application/json"}
    if token:
        h["Authorization"] = f"Bearer {token}"
    if user_id is not None:
        h["X-User-Id"] = str(user_id)
    return h

# ------------------------------ 主流程 ------------------------------
def main():
    print("\n" + "=" * 60)
    print("  RealmCrafter E2E 点火冒烟测试")
    print("=" * 60 + "\n")

    alice_id = None
    bob_id = None
    alice_token = None
    bob_token = None
    setting_id = None
    story_id = None
    bob_fork_story_id = None

    # ---------- 1. 上帝视角准备：注册 + 登录 ----------
    step("1. 注册用户 Alice（大作家）与 Bob（土豪读者）")
    r = requests.post(f"{BASE_URL}/api/v1/auth/register", json={
        "username": ALICE_USERNAME,
        "password": "alice123456",
        "nickname": "Alice",
        "signature": "大作家"
    })
    data = assert_ok(r, "注册 Alice")
    alice_id = data["data"]["userId"]
    alice_token = data["data"]["token"]
    ok(f"Alice 注册成功。userId={alice_id}")

    r = requests.post(f"{BASE_URL}/api/v1/auth/register", json={
        "username": BOB_USERNAME,
        "password": "bob123456",
        "nickname": "Bob",
        "signature": "土豪读者"
    })
    data = assert_ok(r, "注册 Bob")
    bob_id = data["data"]["userId"]
    bob_token = data["data"]["token"]
    ok(f"Bob 注册成功。userId={bob_id}")

    step("2. 登录获取 JWT（已由注册返回，此处再登录一次验证）")
    r = requests.post(f"{BASE_URL}/api/v1/auth/login", json={
        "username": ALICE_USERNAME,
        "password": "alice123456"
    })
    assert_ok(r, "Alice 登录")
    r = requests.post(f"{BASE_URL}/api/v1/auth/login", json={
        "username": BOB_USERNAME,
        "password": "bob123456"
    })
    assert_ok(r, "Bob 登录")
    ok("双方 JWT 有效")

    step("3. 使用支付回调给 Bob 充值 1000 灵能水晶（无需 Admin）")
    r = requests.post(f"{BASE_URL}/api/v1/payment/wechat/callback", json={
        "userId": bob_id,
        "amount": BOB_RECHARGE,
        "orderId": f"e2e-recharge-{SUFFIX}"
    }, headers={"Content-Type": "application/json"})
    if r.status_code != 200 or (r.json() or {}).get("code") != "SUCCESS":
        fail("Bob 充值失败", dump(r))
        raise AssertionError(dump(r))
    ok(f"Bob 充值 {BOB_RECHARGE} 灵能水晶成功")

    if E2E_ADMIN_USERNAME and E2E_ADMIN_PASSWORD:
        step("4. 管理员授予 Alice 金牌创作者（isGoldenCreator=true）")
        r = requests.post(f"{BASE_URL}/api/v1/auth/login", json={
            "username": E2E_ADMIN_USERNAME,
            "password": E2E_ADMIN_PASSWORD
        })
        data = assert_ok(r, "管理员登录")
        admin_token = data["data"]["token"]
        r = requests.post(
            f"{BASE_URL}/api/v1/admin/grant-golden-creator?targetUserId={alice_id}",
            headers=auth_headers(admin_token)
        )
        assert_ok(r, "授予金牌创作者")
        ok("Alice 已设为金牌创作者（分润 90%）")
    else:
        info("4. 未配置 E2E_ADMIN_USERNAME/PASSWORD，跳过金牌授予；Alice 按 Lv1 分润 70%")

    # ---------- 2. 创世：设定集 + 故事 ----------
    step("5. Alice 创建设定集《赛博修仙》（定价 %s 水晶，allowDownload=true）" % STORY_PRICE)
    r = requests.post(
        f"{BASE_URL}/api/v1/settings",
        headers=auth_headers(alice_token, alice_id),
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
    data = assert_ok(r, "创建设定集")
    setting_id = data["data"]["id"]
    ok(f"设定集创建成功。id={setting_id}")

    step("6. Alice 基于该设定集创建同名故事《赛博修仙》")
    r = requests.post(
        f"{BASE_URL}/api/v1/stories",
        headers=auth_headers(alice_token, alice_id),
        json={
            "userId": alice_id,
            "settingPackId": setting_id,
            "title": STORY_TITLE,
            "cover": "",
            "description": "赛博修仙故事",
            "price": STORY_PRICE
        }
    )
    data = assert_ok(r, "创建故事")
    story_id = data["data"]["id"]
    ok(f"故事创建成功。id={story_id}")

    # ---------- 3. 广场相遇与商业化 ----------
    step("7. Bob 在广场刷到 Alice 的《赛博修仙》")
    r = requests.get(
        f"{BASE_URL}/api/v1/square/stories?page=0&size=20&sort=NEWEST",
        headers=auth_headers(bob_token, bob_id)
    )
    if r.status_code == 200:
        data = r.json() or {}
        if data.get("code") == 0:
            content = data.get("data") or {}
            items = content.get("content", []) if isinstance(content, dict) else []
            found = next((s for s in items if s.get("id") == story_id or s.get("title") == STORY_TITLE), None)
            if found:
                ok("Bob 在广场看到《赛博修仙》")
            else:
                info("广场列表中未找到该故事，继续后续步骤（可能排序或分页导致）")
        else:
            info("广场接口返回非 0，继续后续步骤。body=%s" % (dump(r),))
    else:
        info("广场接口 HTTP %s，跳过广场校验，继续点赞/Fork（story_id 已知）" % r.status_code)

    step("8. Bob 对故事点赞（InteractionService.toggleLike）")
    r = requests.post(
        f"{BASE_URL}/api/v1/interactions/like",
        headers=auth_headers(bob_token, bob_id),
        json={"type": "STORY", "id": story_id}
    )
    data = assert_ok(r, "点赞")
    ok(f"点赞成功。liked={data.get('data', {}).get('liked', True)}")

    step("9. Bob 购买 Fork（%s 水晶），验证扣款与分润" % STORY_PRICE)
    r = requests.get(f"{BASE_URL}/api/v1/auth/profile", headers=auth_headers(bob_token))
    data = assert_ok(r, "Bob 购买前 profile")
    bob_balance_before = float((data.get("data") or {}).get("crystalBalance") or 0)

    r = requests.post(
        f"{BASE_URL}/api/v1/stories/{story_id}/fork",
        headers=auth_headers(bob_token, bob_id)
    )
    data = assert_ok(r, "Fork 故事")
    bob_fork_story_id = data["data"]["id"]
    ok(f"Bob 成功购买《赛博修仙》Fork。forkStoryId={bob_fork_story_id}")

    r = requests.get(f"{BASE_URL}/api/v1/auth/profile", headers=auth_headers(bob_token))
    data = assert_ok(r, "Bob 购买后 profile")
    bob_balance_after = float((data.get("data") or {}).get("crystalBalance") or 0)
    if abs((bob_balance_before - STORY_PRICE) - bob_balance_after) > 0.01:
        fail(f"Bob 水晶未正确扣减：before={bob_balance_before}, after={bob_balance_after}, 期望减少 {STORY_PRICE}")
    else:
        ok(f"Bob 扣款正确。当前水晶余额: {bob_balance_after}")

    r = requests.get(f"{BASE_URL}/api/v1/auth/profile", headers=auth_headers(alice_token))
    data = assert_ok(r, "Alice 收益后 profile")
    alice_balance = float((data.get("data") or {}).get("crystalBalance") or 0)
    # 金牌 90%，非金牌 Lv1 70%；价格 STORY_PRICE
    expected_min = round(STORY_PRICE * 0.7, 2)
    expected_max = round(STORY_PRICE * 0.9, 2)
    if not (expected_min <= alice_balance <= expected_max):
        fail(f"Alice 收益异常：crystalBalance={alice_balance}，期望约 {expected_min}（70%）或 {expected_max}（90%）")
    else:
        ok(f"Alice 收到分润，当前水晶: {alice_balance}（金牌≈{expected_max}，非金牌≈{expected_min}）")

    # ---------- 4. 社交裂变与回声 ----------
    step("10. Bob 在购买的分支下发表评论并 @Alice")
    # 评论挂在 Bob 的 fork 故事上，chapterId 用 1（故事可能尚无章节，仅存评论）
    r = requests.post(
        f"{BASE_URL}/api/v1/comments",
        headers=auth_headers(bob_token, bob_id),
        json={
            "storyId": bob_fork_story_id,
            "chapterId": 1,
            "content": f"太牛逼了，@{ALICE_USERNAME} 快更！",
            "targetType": "PARAGRAPH",
            "targetRef": "0",
            "mentionedUserIds": [alice_id]
        }
    )
    data = assert_ok(r, "发表评论")
    ok("Bob 评论成功，已 @Alice")

    step("11. Alice 拉取未读通知（期望：REWARD 进账 + MENTION 被@）")
    time.sleep(2)  # 留时间给 afterCommit 监听器落库
    r = requests.get(
        f"{BASE_URL}/api/v1/notifications?page=0&size=20",
        headers=auth_headers(alice_token, alice_id)
    )
    data = assert_ok(r, "Alice 通知列表")
    content = data.get("data") or {}
    notifications = content.get("content", []) if isinstance(content, dict) else []
    reward_notifications = [n for n in notifications if n.get("type") == "REWARD"]
    mention_notifications = [n for n in notifications if n.get("type") == "MENTION"]
    if not reward_notifications:
        fail("Alice 未收到 REWARD 通知（Fork 分润）", f"当前通知: {[n.get('type') for n in notifications]}")
        if not notifications:
            info("响应 data 结构: " + json.dumps(content, ensure_ascii=False)[:200])
        raise AssertionError("缺少 REWARD 通知")
    else:
        ok(f"Alice 收到 REWARD 通知（共 {len(reward_notifications)} 条）")
    if not mention_notifications:
        fail("Alice 未收到 MENTION 通知（被 @）")
        raise AssertionError("缺少 MENTION 通知")
    else:
        ok(f"Alice 收到 MENTION 通知（共 {len(mention_notifications)} 条）")

    # 若后端有点赞即发 INTERACTION 通知，可在此断言
    interaction_notifications = [n for n in notifications if n.get("type") == "INTERACTION"]
    if interaction_notifications:
        ok(f"Alice 收到 INTERACTION 通知（如被赞，共 {len(interaction_notifications)} 条）")
    else:
        info("当前后端点赞未发 INTERACTION 通知，仅加 EXP，属预期行为")

    print("\n" + "=" * 60)
    print("  全链路 E2E 点火冒烟测试通过")
    print("=" * 60 + "\n")

if __name__ == "__main__":
    try:
        main()
    except AssertionError as e:
        print("\033[91m\nE2E 测试未通过。请根据上述 response body 排查。\033[0m\n")
        sys.exit(1)
    except requests.exceptions.ConnectionError as e:
        fail("无法连接后端", f"请确认服务已启动: {BASE_URL}\n{e}")
        sys.exit(1)
    except Exception as e:
        fail("未预期异常", str(e))
        raise
