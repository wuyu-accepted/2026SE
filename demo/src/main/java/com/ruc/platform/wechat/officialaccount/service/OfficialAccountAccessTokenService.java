package com.ruc.platform.wechat.officialaccount.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruc.platform.common.api.ResultCode;
import com.ruc.platform.common.exception.BizException;
import com.ruc.platform.wechat.officialaccount.config.OfficialAccountProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OfficialAccountAccessTokenService {

    private final OfficialAccountProperties properties;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private volatile String accessToken;
    private volatile Instant expiresAt;

    public String getAccessToken() {
        if (isTokenValid()) {
            return accessToken;
        }
        synchronized (this) {
            if (isTokenValid()) {
                return accessToken;
            }
            refreshToken();
            return accessToken;
        }
    }

    private boolean isTokenValid() {
        if (!StringUtils.hasText(accessToken) || expiresAt == null) {
            return false;
        }
        return Instant.now().isBefore(expiresAt.minusSeconds(60));
    }

    private void refreshToken() {
        if (!StringUtils.hasText(properties.getAppid()) || !StringUtils.hasText(properties.getSecret())) {
            throw new BizException(ResultCode.BIZ_ERROR, "未配置公众号 appid/secret（wechat.officialaccount.appid / wechat.officialaccount.secret）");
        }

        String appid = urlEncode(properties.getAppid());
        String secret = urlEncode(properties.getSecret());
        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" + appid + "&secret=" + secret;

        String body = sendGet(url);
        JsonNode json = readJson(body);

        JsonNode errCodeNode = json.get("errcode");
        if (errCodeNode != null && errCodeNode.asInt() != 0) {
            String errMsg = json.hasNonNull("errmsg") ? json.get("errmsg").asText() : "unknown";
            throw new BizException(ResultCode.BIZ_ERROR, "获取公众号 access_token 失败: errcode=" + errCodeNode.asInt() + ", errmsg=" + errMsg);
        }

        JsonNode tokenNode = json.get("access_token");
        JsonNode expiresInNode = json.get("expires_in");
        if (tokenNode == null || !tokenNode.isTextual() || expiresInNode == null || !expiresInNode.isNumber()) {
            throw new BizException(ResultCode.BIZ_ERROR, "获取公众号 access_token 失败: 响应格式不正确");
        }

        this.accessToken = tokenNode.asText();
        this.expiresAt = Instant.now().plusSeconds(expiresInNode.asLong());
    }

    private String sendGet(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
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

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
