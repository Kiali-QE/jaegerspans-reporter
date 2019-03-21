package io.jaegertracing.qe.spansreporter.mqtt;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.jaegertracing.qe.spansreporter.Utils;

@Component
public class Tasks {

    @Scheduled(initialDelay = 30 * 1000L, fixedRate = 30 * 1000L)
    public void sendHeartbeat() {
        Utils.sendAboutMe();
    }
}
