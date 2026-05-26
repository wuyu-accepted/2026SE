package com.ruc.platform.ai.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WechatArticleFetchServiceImplTest {

    private final WechatArticleFetchServiceImpl service = new WechatArticleFetchServiceImpl();

    @Test
    void parseExtractsWechatArticleTitleAndCleanContent() {
        String html = """
                <html>
                  <head>
                    <meta property="og:title" content="公众号标题&amp;报名通知">
                  </head>
                  <body>
                    <div id="js_content">
                      <p>人工智能讲座&nbsp;报名安排</p>
                      <span>请在周五前提交材料。</span>
                      <style>.hidden{display:none}</style>
                    </div>
                    <script>window.bad = '不要进入正文';</script>
                  </body>
                </html>
                """;

        WechatArticleFetchService.WechatArticleContent content = service.parse(html);

        assertThat(content).isNotNull();
        assertThat(content.title()).isEqualTo("公众号标题&报名通知");
        assertThat(content.content()).contains("人工智能讲座 报名安排", "请在周五前提交材料。");
        assertThat(content.content()).doesNotContain("window.bad", "不要进入正文", ".hidden");
    }
}
