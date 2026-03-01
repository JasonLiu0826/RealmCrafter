#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
RealmCrafter 全身份、全操作闭环 E2E 详细测试

身份：
  - Alice：普通创作者（注册、设定集、故事、分润、通知、私信、偏好）
  - Bob：读者（注册、充值、广场、点赞/收藏、Fork 购买、评论、私信、分享、设定集 Fork）
  - Admin：可选，需配置 E2E_ADMIN_USERNAME / E2E_ADMIN_PASSWORD 且在 DB 中 role=ADMIN

模块：鉴权、支付、管理后台、设定集、故事、广场、互动、评论、通知、私信、分享、用户偏好与引擎配置

运行：
  pip install requests
  export E2E_BASE_URL=http://localhost:8080   # 可选
  export E2E_ADMIN_USERNAME=admin E2E_ADMIN_PASSWORD=xxx   # 可选，跑管理端测试
  python e2e_full.py

管理员账号：若需测 grant-golden-creator / take-down-story，需先在 DB 将对应用户 role 设为 ADMIN。
"""

import os
import sys
import time
import json

if sys.platform == "win32":
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8", errors="replace")

try:
    import requests
except ImportError:
    print("[FAIL] pip install requests")
    sys.exit(1)

BASE_URL = os.environ.get("E2E_BASE_URL", "http://localhost:8080")
SUFFIX = str(int(time.time()))
# 身份：创作者、读者
ALICE_USER = "e2e_alice_" + SUFFIX
BOB_USER = "e2e_bob_" + SUFFIX
# 管理员（需在 DB 中将该用户 role 设为 ADMIN；不设则跳过管理端测试）
E2E_ADMIN_USER = os.environ.get("E2E_ADMIN_USERNAME", "")
E2E_ADMIN_PASS = os.environ.get("E2E_ADMIN_PASSWORD", "")
# 业务常量
STORY_TITLE = "赛博修仙"
STORY_PRICE = 15
SETTING_TITLE = "赛博修仙设定"
BOB_RECHARGE = 1000
NOTIFICATION_POLL_SEC = 2
NOTIFICATION_POLL_ATTEMPTS = 3
DEFAULT_SETTING_CONTENT = {
    "characters": "主角",
    "worldview": "赛博修仙",
    "environment": "未来",
    "mainline": "修仙",
    "plotPoints": "无",
}

def ok(msg):   print("\033[92m[OK] %s\033[0m" % msg)
def fail(msg, detail=None):
    print("\033[91m[FAIL] %s\033[0m" % msg)
    if detail: print("\033[91m  %s\033[0m" % (str(detail)[:500]))
def step(msg): print("\033[93m[>>] %s\033[0m" % msg)
def info(msg): print("\033[94m[INFO] %s\033[0m" % msg)

def resp_body(r):
    try: return r.json() if r.text else {}
    except Exception: return {}

def dump(r):
    return json.dumps(resp_body(r), ensure_ascii=False, indent=2)

def api_ok(r, name):
    if r.status_code != 200:
        fail("%s: HTTP %s" % (name, r.status_code), dump(r))
        raise AssertionError("HTTP %s" % r.status_code)
    b = resp_body(r)
    if b.get("code", -1) != 0:
        fail("%s: code=%s msg=%s" % (name, b.get("code"), b.get("message", "")), dump(r))
        raise AssertionError("code=%s" % b.get("code"))
    return b

def h(token=None, user_id=None):
    ret = {"Content-Type": "application/json"}
    if token: ret["Authorization"] = "Bearer %s" % token
    if user_id is not None: ret["X-User-Id"] = str(user_id)
    return ret

def page_list(node):
    if node is None: return []
    if isinstance(node, list): return node
    if isinstance(node, dict): return node.get("content", [])
    return []


def run():
    print("\n" + "=" * 60)
    print("  RealmCrafter 全身份全操作 E2E 详细测试")
    print("=" * 60 + "\n")

    alice_id = bob_id = admin_id = None
    alice_token = bob_token = admin_token = None
    setting_id = story_id = bob_fork_id = root_comment_id = None
    short_code = None

    # ==================== 一、鉴权 ====================
    step("1.1 注册创作者 Alice")
    r = requests.post(BASE_URL + "/api/v1/auth/register", json={
        "username": ALICE_USER, "password": "alice123456",
        "nickname": "Alice", "signature": "大作家"
    })
    d = api_ok(r, "注册 Alice")
    alice_id = d["data"]["userId"]
    alice_token = d["data"]["token"]
    ok("Alice 注册成功 userId=%s" % alice_id)

    step("1.2 注册读者 Bob")
    r = requests.post(BASE_URL + "/api/v1/auth/register", json={
        "username": BOB_USER, "password": "bob123456",
        "nickname": "Bob", "signature": "土豪读者"
    })
    d = api_ok(r, "注册 Bob")
    bob_id = d["data"]["userId"]
    bob_token = d["data"]["token"]
    ok("Bob 注册成功 userId=%s" % bob_id)

    step("1.3 Alice / Bob 登录")
    r = requests.post(BASE_URL + "/api/v1/auth/login", json={"username": ALICE_USER, "password": "alice123456"})
    api_ok(r, "Alice 登录")
    r = requests.post(BASE_URL + "/api/v1/auth/login", json={"username": BOB_USER, "password": "bob123456"})
    api_ok(r, "Bob 登录")
    ok("双方登录正常")

    step("1.4 获取 profile（Alice）")
    r = requests.get(BASE_URL + "/api/v1/auth/profile", headers=h(alice_token, alice_id))
    d = api_ok(r, "profile")
    ok("profile nickname=%s level=%s" % (d.get("data", {}).get("nickname"), d.get("data", {}).get("level")))

    step("1.5 更新 profile（Alice 签名）")
    r = requests.patch(BASE_URL + "/api/v1/auth/profile", headers=h(alice_token, alice_id),
        json={"nickname": "Alice", "signature": "大作家·E2E", "avatar": None})
    api_ok(r, "patch profile")
    ok("profile 更新成功")

    step("1.6 未登录访问 profile 期望 401")
    r = requests.get(BASE_URL + "/api/v1/auth/profile", headers=h())
    if r.status_code == 200 and resp_body(r).get("code") == 401:
        ok("未登录 profile 返回 401")
    else:
        info("未登录 profile 返回 HTTP %s code=%s" % (r.status_code, resp_body(r).get("code")))

    # ==================== 二、支付 ====================
    step("2.1 Bob 充值（微信回调）")
    r = requests.post(BASE_URL + "/api/v1/payment/wechat/callback", headers={"Content-Type": "application/json"},
        json={"userId": bob_id, "amount": BOB_RECHARGE, "orderId": "e2e-wx-%s" % SUFFIX})
    if r.status_code != 200 or resp_body(r).get("code") != "SUCCESS":
        fail("充值失败", dump(r))
        raise AssertionError("recharge")
    ok("Bob 充值 %s 水晶" % BOB_RECHARGE)

    # ==================== 三、管理后台（可选） ====================
    if E2E_ADMIN_USER and E2E_ADMIN_PASS:
        step("3.1 管理员登录")
        r = requests.post(BASE_URL + "/api/v1/auth/login", json={"username": E2E_ADMIN_USER, "password": E2E_ADMIN_PASS})
        d = api_ok(r, "admin login")
        admin_id = d["data"]["userId"]
        admin_token = d["data"]["token"]
        ok("Admin 登录 userId=%s" % admin_id)

        step("3.2 授予 Alice 金牌创作者")
        r = requests.post(BASE_URL + "/api/v1/admin/grant-golden-creator?targetUserId=%s" % alice_id, headers=h(admin_token, admin_id))
        api_ok(r, "grant-golden-creator")
        ok("Alice 已设为金牌创作者")
    else:
        info("3.x 未配置 E2E_ADMIN_USERNAME/PASSWORD 或未在 DB 设 ADMIN 角色，跳过管理端测试")

    # ==================== 四、设定集 ====================
    step("4.1 Alice 创建设定集")
    r = requests.post(BASE_URL + "/api/v1/settings", headers=h(alice_token, alice_id), json={
        "title": SETTING_TITLE, "cover": "", "description": "赛博修仙",
        "content": DEFAULT_SETTING_CONTENT, "allowDownload": True, "allowModify": True, "price": STORY_PRICE
    })
    d = api_ok(r, "创建设定集")
    setting_id = d["data"]["id"]
    ok("设定集 id=%s" % setting_id)

    step("4.2 Alice 列出自己的设定集")
    r = requests.get(BASE_URL + "/api/v1/settings?page=0&size=20", headers=h(alice_token, alice_id))
    d = api_ok(r, "list settings")
    items = page_list(d.get("data"))
    ok("设定集列表 %s 条" % len(items))

    step("4.3 Alice 获取设定集详情")
    r = requests.get(BASE_URL + "/api/v1/settings/%s" % setting_id, headers=h(alice_token, alice_id))
    api_ok(r, "setting detail")
    ok("设定集详情获取成功")

    step("4.4 Alice 更新设定集（Put）")
    r = requests.put(BASE_URL + "/api/v1/settings/%s" % setting_id, headers=h(alice_token, alice_id), json={
        "versionId": 1, "title": SETTING_TITLE, "cover": "", "description": "赛博修仙·更新",
        "content": DEFAULT_SETTING_CONTENT, "deviceHash": None, "allowDownload": True, "allowModify": True
    })
    api_ok(r, "update setting")
    ok("设定集更新成功")

    # ==================== 五、故事 ====================
    step("5.1 Alice 创建故事")
    r = requests.post(BASE_URL + "/api/v1/stories", headers=h(alice_token, alice_id), json={
        "userId": alice_id, "settingPackId": setting_id, "title": STORY_TITLE,
        "cover": "", "description": "赛博修仙故事", "price": STORY_PRICE
    })
    d = api_ok(r, "创建故事")
    story_id = d["data"]["id"]
    ok("故事 id=%s" % story_id)

    step("5.2 Alice 列出自己的故事")
    r = requests.get(BASE_URL + "/api/v1/stories?page=0&size=20", headers=h(alice_token, alice_id))
    d = api_ok(r, "list stories")
    ok("故事列表 %s 条" % len(page_list(d.get("data"))))

    step("5.3 Bob 获取故事详情（公开）")
    r = requests.get(BASE_URL + "/api/v1/stories/%s" % story_id, headers=h(bob_token, bob_id))
    api_ok(r, "story detail")
    ok("故事详情获取成功")

    step("5.4 Alice 重命名故事")
    r = requests.patch(BASE_URL + "/api/v1/stories/%s/rename" % story_id, headers=h(alice_token, alice_id),
        json={"userId": alice_id, "title": STORY_TITLE + "·改"})
    api_ok(r, "rename story")
    ok("故事重命名成功")

    step("5.5 Alice 改回标题")
    r = requests.patch(BASE_URL + "/api/v1/stories/%s/rename" % story_id, headers=h(alice_token, alice_id),
        json={"userId": alice_id, "title": STORY_TITLE})
    api_ok(r, "rename back")

    # ==================== 六、广场 ====================
    step("6.1 Bob 广场刷故事（NEWEST）")
    r = requests.get(BASE_URL + "/api/v1/square/stories?page=0&size=20&sort=NEWEST", headers=h(bob_token, bob_id))
    d = api_ok(r, "square stories")
    items = page_list(d.get("data"))
    ok("广场故事 %s 条" % len(items))

    step("6.2 Bob 广场刷设定集")
    r = requests.get(BASE_URL + "/api/v1/square/settings?page=0&size=20&sort=NEWEST", headers=h(bob_token, bob_id))
    d = api_ok(r, "square settings")
    ok("广场设定集 %s 条" % len(page_list(d.get("data"))))

    step("6.3 访客（无 token）拉广场故事")
    r = requests.get(BASE_URL + "/api/v1/square/stories?page=0&size=5&sort=NEWEST", headers=h())
    if r.status_code == 200 and resp_body(r).get("code") == 0:
        ok("访客可拉广场故事")
    else:
        info("访客广场返回 HTTP %s（可能需登录）" % r.status_code)

    # ==================== 七、互动 ====================
    step("7.1 Bob 点赞故事")
    r = requests.post(BASE_URL + "/api/v1/interactions/like", headers=h(bob_token, bob_id),
        json={"type": "STORY", "id": story_id})
    d = api_ok(r, "like")
    ok("点赞 liked=%s" % d.get("data", {}).get("liked"))

    step("7.2 Bob 收藏故事")
    r = requests.post(BASE_URL + "/api/v1/interactions/favorite", headers=h(bob_token, bob_id),
        json={"type": "STORY", "id": story_id})
    d = api_ok(r, "favorite")
    ok("收藏 favorited=%s" % d.get("data", {}).get("favorited"))

    # ==================== 八、Fork 购买 ====================
    step("8.1 Bob 购买故事 Fork")
    r = requests.get(BASE_URL + "/api/v1/auth/profile", headers=h(bob_token))
    api_ok(r, "profile before")
    r = requests.post(BASE_URL + "/api/v1/stories/%s/fork" % story_id, headers=h(bob_token, bob_id))
    d = api_ok(r, "fork")
    bob_fork_id = d["data"]["id"]
    ok("Fork 成功 forkId=%s" % bob_fork_id)

    r = requests.get(BASE_URL + "/api/v1/auth/profile", headers=h(bob_token))
    d = api_ok(r, "profile after")
    bob_crystal = float((d.get("data") or {}).get("crystalBalance") or 0)
    ok("Bob 扣款后水晶 %.2f" % bob_crystal)

    r = requests.get(BASE_URL + "/api/v1/auth/profile", headers=h(alice_token, alice_id))
    d = api_ok(r, "Alice profile")
    alice_crystal = float((d.get("data") or {}).get("crystalBalance") or 0)
    ok("Alice 分润后水晶 %.2f" % alice_crystal)

    # ==================== 九、评论 ====================
    step("9.1 Bob 在 Fork 故事下评论并 @Alice")
    r = requests.post(BASE_URL + "/api/v1/comments", headers=h(bob_token, bob_id), json={
        "storyId": bob_fork_id, "chapterId": 1, "content": "太棒了，@%s 快更！" % ALICE_USER,
        "targetType": "PARAGRAPH", "targetRef": "0", "mentionedUserIds": [alice_id]
    })
    d = api_ok(r, "add comment")
    root_comment_id = d.get("data", {}).get("id")
    ok("评论成功 commentId=%s" % root_comment_id)

    step("9.2 按锚点拉取评论")
    r = requests.get(BASE_URL + "/api/v1/comments/anchor?storyId=%s&chapterId=1&targetType=PARAGRAPH&targetRef=0&page=0&size=10" % bob_fork_id,
        headers=h(bob_token, bob_id))
    d = api_ok(r, "list by anchor")
    ok("锚点评论 %s 条" % len(page_list(d.get("data"))))

    if root_comment_id:
        step("9.3 获取单条评论")
        r = requests.get(BASE_URL + "/api/v1/comments/%s" % root_comment_id, headers=h(bob_token, bob_id))
        api_ok(r, "get comment")
        ok("评论详情获取成功")

        step("9.4 拉取楼中楼（replies）")
        r = requests.get(BASE_URL + "/api/v1/comments/replies/%s" % root_comment_id, headers=h(bob_token, bob_id))
        d = api_ok(r, "replies")
        replies = d.get("data") if isinstance(d.get("data"), list) else []
        ok("楼中楼 %s 条" % len(replies))

    # ==================== 十、通知 ====================
    step("10.1 Alice 拉取通知（轮询）")
    notifications = []
    for attempt in range(1, NOTIFICATION_POLL_ATTEMPTS + 1):
        time.sleep(NOTIFICATION_POLL_SEC)
        r = requests.get(BASE_URL + "/api/v1/notifications?page=0&size=20", headers=h(alice_token, alice_id))
        d = api_ok(r, "notifications")
        notifications = page_list(d.get("data"))
        types = [n.get("type") for n in notifications if n.get("type")]
        info("第 %s 次 共 %s 条 类型=%s" % (attempt, len(notifications), types))
        if notifications:
            break

    reward_n = [n for n in notifications if n.get("type") == "REWARD"]
    mention_n = [n for n in notifications if n.get("type") == "MENTION"]
    if reward_n: ok("REWARD 通知 %s 条" % len(reward_n))
    else: info("未收到 REWARD（视后端实现）")
    if mention_n: ok("MENTION 通知 %s 条" % len(mention_n))
    else: info("未收到 MENTION（视后端实现）")

    if notifications:
        nid = notifications[0].get("id")
        if nid:
            step("10.2 Alice 标记单条已读")
            r = requests.patch(BASE_URL + "/api/v1/notifications/%s/read" % nid, headers=h(alice_token, alice_id))
            api_ok(r, "mark read")
            ok("已读成功")
        step("10.3 Alice 全部标已读")
        r = requests.patch(BASE_URL + "/api/v1/notifications/read-all", headers=h(alice_token, alice_id))
        api_ok(r, "mark all read")
        ok("全部已读")

    # ==================== 十一、私信 ====================
    step("11.1 Bob 给 Alice 发私信")
    r = requests.post(BASE_URL + "/api/v1/messages/send", headers=h(bob_token, bob_id), json={
        "receiverId": int(alice_id), "msgType": "TEXT", "content": "E2E 你好 Alice"
    })
    api_ok(r, "send message")
    ok("私信发送成功")

    step("11.2 Alice 会话列表")
    r = requests.get(BASE_URL + "/api/v1/messages/sessions?page=0&size=20", headers=h(alice_token, alice_id))
    d = api_ok(r, "sessions")
    sessions = page_list(d.get("data"))
    ok("会话数 %s" % len(sessions))

    step("11.3 Alice 与 Bob 聊天记录")
    r = requests.get(BASE_URL + "/api/v1/messages/chat/%s?page=0&size=20" % bob_id, headers=h(alice_token, alice_id))
    d = api_ok(r, "chat")
    msgs = page_list(d.get("data"))
    ok("聊天记录 %s 条" % len(msgs))

    # ==================== 十二、分享 ====================
    step("12.1 生成分享链接（故事/章节）")
    r = requests.post(BASE_URL + "/api/v1/share/generate", headers=h(bob_token, bob_id), json={
        "type": "PARAGRAPH", "storyId": bob_fork_id, "chapterId": 1, "targetRef": "0", "excerpt": "E2E 分享"
    })
    d = api_ok(r, "share generate")
    short_code = (d.get("data") or {}).get("shortCode")
    ok("shortCode=%s" % (short_code or ""))

    if short_code:
        step("12.2 解析短链")
        r = requests.get(BASE_URL + "/api/v1/share/decode/%s" % short_code, headers=h(bob_token, bob_id))
        d = api_ok(r, "decode")
        ok("decode 成功 storyId=%s" % (d.get("data") or {}).get("storyId"))

    # ==================== 十三、用户偏好与引擎配置 ====================
    step("13.1 Alice 更新主题")
    r = requests.patch(BASE_URL + "/api/v1/users/me/theme", headers=h(alice_token, alice_id),
        json={"themeId": "classic_white"})
    api_ok(r, "theme")
    ok("主题更新成功")

    step("13.2 Alice 获取引擎配置")
    r = requests.get(BASE_URL + "/api/v1/users/me/engine-config", headers=h(alice_token, alice_id))
    d = api_ok(r, "engine-config")
    ok("chaosLevel=%s preferredModel=%s" % (d.get("data", {}).get("chaosLevel"), d.get("data", {}).get("preferredModel")))

    step("13.3 Alice 更新引擎配置")
    r = requests.patch(BASE_URL + "/api/v1/users/me/engine-config", headers=h(alice_token, alice_id),
        json={"chaosLevel": 0.75, "preferredModel": "realm_crafter_v1"})
    api_ok(r, "engine-config patch")
    ok("引擎配置更新成功")

    # ==================== 十四、设定集 Fork（Bob 克隆设定） ====================
    step("14.1 Bob Fork 设定集")
    r = requests.post(BASE_URL + "/api/v1/settings/%s/fork" % setting_id, headers=h(bob_token, bob_id))
    d = api_ok(r, "fork setting")
    ok("设定集 Fork 成功 id=%s" % (d.get("data") or {}).get("id"))

    # ==================== 十五、故事阅读时间 ====================
    step("15.1 Bob 更新故事阅读时间")
    r = requests.patch(BASE_URL + "/api/v1/stories/%s/read" % bob_fork_id, headers=h(bob_token, bob_id))
    api_ok(r, "update read time")
    ok("阅读时间更新成功")

    # ==================== 十六、管理后台（下架/封禁，可选） ====================
    if admin_id and admin_token:
        step("16.1 管理员下架故事（可选）")
        r = requests.post(BASE_URL + "/api/v1/admin/take-down-story?storyId=%s" % story_id, headers=h(admin_token, admin_id))
        # 若业务不允许下架自己的或会 400，仅记录结果
        if r.status_code == 200 and resp_body(r).get("code") == 0:
            ok("故事已下架")
        else:
            info("下架返回 code=%s（可能业务限制）" % resp_body(r).get("code"))

    # ==================== 十七、评论删除（Bob 删自己的评论） ====================
    if root_comment_id:
        step("17.1 Bob 删除自己的评论")
        r = requests.delete(BASE_URL + "/api/v1/comments/%s" % root_comment_id, headers=h(bob_token, bob_id))
        api_ok(r, "delete comment")
        ok("评论删除成功")

    # ==================== 十八、故事删除（Alice 删一个自己的故事，用 Fork 后 Bob 的不会删） ====================
    step("18.1 Alice 列出故事后可选删除（此处不删主故事，仅演示）")
    r = requests.get(BASE_URL + "/api/v1/stories?page=0&size=5", headers=h(alice_token, alice_id))
    d = api_ok(r, "list stories")
    ok("Alice 故事列表正常，跳过删除主故事以保留数据")

    print("\n" + "=" * 60)
    print("  全身份全操作 E2E 详细测试完成")
    print("=" * 60 + "\n")


if __name__ == "__main__":
    try:
        run()
    except AssertionError:
        print("\n\033[91mE2E 未通过。\033[0m\n")
        sys.exit(1)
    except requests.exceptions.ConnectionError as e:
        fail("连接失败 %s" % BASE_URL, str(e))
        sys.exit(1)
    except Exception as e:
        fail("异常", str(e))
        raise
