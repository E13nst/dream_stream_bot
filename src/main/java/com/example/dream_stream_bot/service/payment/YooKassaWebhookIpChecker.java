package com.example.dream_stream_bot.service.payment;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Проверка IP отправителя HTTP-уведомления ЮKassa (документация «Notification authentication»).
 */
@Component
public class YooKassaWebhookIpChecker {

    private final List<IpAddressMatcher> matchers = List.of(
            new IpAddressMatcher("185.71.76.0/27"),
            new IpAddressMatcher("185.71.77.0/27"),
            new IpAddressMatcher("77.75.153.0/25"),
            new IpAddressMatcher("77.75.156.11"),
            new IpAddressMatcher("77.75.156.35"),
            new IpAddressMatcher("77.75.154.128/25"),
            new IpAddressMatcher("2a02:5180::/32")
    );

    public boolean isAllowed(HttpServletRequest request) {
        String ip = clientIp(request);
        if (ip == null || ip.isBlank()) {
            return false;
        }
        for (IpAddressMatcher m : matchers) {
            if (m.matches(ip)) {
                return true;
            }
        }
        return false;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
