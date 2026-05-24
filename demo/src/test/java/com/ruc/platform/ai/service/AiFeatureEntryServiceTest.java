package com.ruc.platform.ai.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AiFeatureEntryServiceTest {

    @Test
    void matchesLeaveEntryFromNaturalLanguageQuestion() {
        AiFeatureEntryService service = new AiFeatureEntryService();

        var actions = service.match("我今天生病了，想申请请假", Set.of("student"), List.of(), 3);

        assertThat(actions).isNotEmpty();
        assertThat(actions.get(0).getCode()).isEqualTo("leave");
        assertThat(actions.get(0).getPath()).isEqualTo("/pages/leave-list/leave-list");
        assertThat(actions.get(0).getTabPage()).isFalse();
    }

    @Test
    void matchesPartyReportEntry() {
        AiFeatureEntryService service = new AiFeatureEntryService();

        var actions = service.match("思想汇报在哪里提交", Set.of("student"), List.of(), 3);

        assertThat(actions).extracting("code").contains("partyReport");
    }

    @Test
    void marksNoticeAsTabPageEntry() {
        AiFeatureEntryService service = new AiFeatureEntryService();

        var actions = service.match("查看通知", Set.of("student"), List.of(), 3);

        assertThat(actions).isNotEmpty();
        assertThat(actions.get(0).getCode()).isEqualTo("notice");
        assertThat(actions.get(0).getTabPage()).isTrue();
    }
}
