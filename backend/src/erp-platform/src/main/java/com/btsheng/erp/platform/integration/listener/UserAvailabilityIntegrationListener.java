package com.btsheng.erp.platform.integration.listener;

import com.btsheng.erp.core.integration.IntegrationStreamPoller;
import com.btsheng.erp.core.integration.IntegrationStreams;
import com.btsheng.erp.core.redis.RedisStreamTemplate;
import com.btsheng.erp.platform.auth.service.UserAvailabilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/** HR 可用性集成事件 · business → platform 本地 sys_user */
@Component
public class UserAvailabilityIntegrationListener extends IntegrationStreamPoller {

    private static final Logger log = LoggerFactory.getLogger(UserAvailabilityIntegrationListener.class);
    private static final String GROUP = "erp-platform-user-availability";
    private static final String CONSUMER = "platform-availability-1";

    private final UserAvailabilityService userAvailabilityService;

    public UserAvailabilityIntegrationListener(RedisStreamTemplate redisStreamTemplate,
                                               UserAvailabilityService userAvailabilityService) {
        super(redisStreamTemplate, GROUP, CONSUMER);
        this.userAvailabilityService = userAvailabilityService;
    }

    @Scheduled(fixedDelayString = "${app.integration.poll-ms:3000}")
    public void pollEvents() {
        poll(20, this::handle);
    }

    private void handle(org.springframework.data.redis.connection.stream.MapRecord<String, String, String> record) {
        Map<String, String> body = record.getValue();
        if (!IntegrationStreams.EVENT_USER_AVAILABILITY_CHANGED.equals(field(body, "eventType"))) {
            return;
        }
        Long userId = parseLong(field(body, "userId"));
        if (userId == null) {
            return;
        }
        userAvailabilityService.syncAvailability(
                userId,
                field(body, "availabilityStatus"),
                field(body, "leaveNo"));
        log.debug("[UserAvailabilityIntegrationListener] synced userId={}", userId);
    }

    private static Long parseLong(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
