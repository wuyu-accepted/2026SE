package com.ruc.platform.wechat.officialaccount.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruc.platform.common.api.ResultCode;
import com.ruc.platform.common.exception.BizException;
import com.ruc.platform.wechat.officialaccount.config.OfficialAccountProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OfficialAccountMenuService {

    private final OfficialAccountProperties properties;
    private final OfficialAccountAccessTokenService accessTokenService;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public Map<String, Object> refreshDefaultMenu() {
        String homeUrl = requireUrl(properties.getMenu().getHomeUrl(), "wechat.officialaccount.menu.home-url");
        String noticeUrl = requireUrl(properties.getMenu().getNoticeUrl(), "wechat.officialaccount.menu.notice-url");
        String serviceUrl = requireUrl(properties.getMenu().getServiceUrl(), "wechat.officialaccount.menu.service-url");

        Map<String, Object> payload = new HashMap<>();
        List<Map<String, Object>> buttons = new ArrayList<>();
        buttons.add(viewButton("首页", homeUrl));
        buttons.add(viewButton("通知", noticeUrl));
        buttons.add(viewButton("服务", serviceUrl));
        payload.put("button", buttons);

        String accessToken = accessTokenService.getAccessToken();
        String url = "https://api.weixin.qq.com/cgi-bin/menu/create?access_token=" + accessToken;
        String responseBody = sendPostJson(url, writeJson(payload));

        JsonNode json = readJson(responseBody);
        int errCode = json.has("errcode") ? json.get("errcode").asInt() : -1;
        String errMsg = json.hasNonNull("errmsg") ? json.get("errmsg").asText() : "unknown";
        if (errCode != 0) {
            throw new BizException(ResultCode.BIZ_ERROR, "创建公众号菜单失败: errcode=" + errCode + ", errmsg=" + errMsg);
        }

        return objectMapper.convertValue(json, new TypeReference<>() {});
    }

    private Map<String, Object> viewButton(String name, String url) {
        Map<String, Object> button = new HashMap<>();
        button.put("type", "view");
        button.put("name", name);
        button.put("url", url);
        return button;
    }

    private String requireUrl(String value, String configKey) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(ResultCode.BIZ_ERROR, "未配置菜单跳转地址（" + configKey + "）");
        }
        return value;
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new BizException(ResultCode.SYSTEM_ERROR.getCode(), "序列化菜单失败", e);
        }
    }

    private String sendPostJson(String url, String jsonBody) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return response.body();
        } catch (Exception e) {
            throw new BizException(ResultCode.SYSTEM_ERROR.getCode(), "调用微信接口失败", e);
        }
    }

    private JsonNode readJson(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new BizException(ResultCode.SYSTEM_ERROR.getCode(), "解析微信响应失败", e);
        }
    }
}
