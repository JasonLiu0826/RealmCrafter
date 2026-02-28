package com.realmcrafter.api.payment;

import com.realmcrafter.application.payment.PaymentCallbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 支付回调网关：微信/支付宝/苹果 IAP 回调验签后调用内部加水晶、续 VIP。
 * 生产环境需按各渠道文档验签并解析 body；此处提供接口骨架与桩验签。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentCallbackService paymentCallbackService;

    /**
     * 微信支付回调。生产：验签 + 解析 XML/JSON 取 out_trade_no、openid、total_fee 等，再映射 userId 与金额。
     */
    @PostMapping(value = "/wechat/callback", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> wechatCallback(@RequestBody Map<String, Object> body) {
        // 桩：从 body 取 userId, amount, orderId；生产需验签并解析微信格式
        Long userId = getLong(body, "userId");
        BigDecimal amount = getBigDecimal(body, "amount");
        String orderId = (String) body.get("orderId");
        if (userId == null || amount == null) {
            return Map.of("code", "FAIL", "message", "参数缺失");
        }
        return processRecharge(userId, amount, orderId);
    }

    /**
     * 支付宝回调。生产：验签 + 解析 form 或 JSON，取 buyer_id / out_trade_no / total_amount 等。
     */
    @PostMapping(value = "/alipay/callback", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> alipayCallback(@RequestBody Map<String, Object> body) {
        Long userId = getLong(body, "userId");
        BigDecimal amount = getBigDecimal(body, "amount");
        String orderId = (String) body.get("orderId");
        if (userId == null || amount == null) {
            return Map.of("code", "FAIL", "message", "参数缺失");
        }
        return processRecharge(userId, amount, orderId);
    }

    /**
     * 苹果 IAP 回调。生产：验证 receipt、解析 productId 与 userId，做补单与续期。
     */
    @PostMapping(value = "/apple/callback", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> appleCallback(@RequestBody Map<String, Object> body) {
        Long userId = getLong(body, "userId");
        String orderId = (String) body.get("transactionId");
        String productId = (String) body.get("productId"); // 如 vip_monthly / crystal_100
        if (userId == null) {
            return Map.of("code", "FAIL", "message", "参数缺失");
        }
        // 桩：按 productId 决定加水晶或续 VIP；生产需验 receipt
        if (productId != null && productId.toLowerCase().startsWith("vip")) {
            int days = productId.contains("year") ? 365 : 30;
            Optional<String> errOpt = paymentCallbackService.renewVipDays(userId, days);
            if (errOpt.isPresent()) return failMap(errOpt.get());
            return successMap();
        }
        BigDecimal amount = getBigDecimal(body, "amount");
        if (amount == null) amount = BigDecimal.ZERO;
        return processRecharge(userId, amount, orderId);
    }

    private Map<String, Object> processRecharge(Long userId, BigDecimal amount, String orderId) {
        Optional<String> errOpt = paymentCallbackService.rechargeCrystal(userId, amount, orderId);
        if (errOpt.isPresent()) return failMap(errOpt.get());
        return successMap();
    }

    private static Map<String, Object> successMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("code", "SUCCESS");
        return m;
    }

    private static Map<String, Object> failMap(String message) {
        Map<String, Object> m = new HashMap<>();
        m.put("code", "FAIL");
        m.put("message", message);
        return m;
    }

    private static Long getLong(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof String) {
            try { return Long.parseLong((String) v); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private static BigDecimal getBigDecimal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Number) return BigDecimal.valueOf(((Number) v).doubleValue());
        if (v instanceof String) {
            try { return new BigDecimal((String) v); } catch (Exception e) { return null; }
        }
        return null;
    }
}
