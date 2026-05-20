package com.ruc.platform.wechat.officialaccount.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "wechat.officialaccount")
public class OfficialAccountProperties {

    private String appid;

    private String secret;

    private Menu menu = new Menu();

    @Data
    public static class Menu {

        private String homeUrl;

        private String noticeUrl;

        private String serviceUrl;
    }
}
