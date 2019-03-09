package io.jaegertracing.qe.spansreporter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import io.jaegertracing.qe.spansreporter.mqtt.MqttUtils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Utils {

    private static String hostname = null;
    private static Integer id = -1;

    public static String getHostname() {
        if (hostname == null) {
            InetAddress inetAddress;
            try {
                inetAddress = InetAddress.getLocalHost();
                hostname = inetAddress.getHostName();
            } catch (UnknownHostException ex) {
                logger.error("Exception,", ex);
            }
        }
        return hostname;
    }

    public static String getReference() {
        return System.getenv().getOrDefault("REFERENCE", "global");
    }

    public static void updateMe(Map<String, Object> data) {
        String _hostname = (String) data.get("hostname");
        if (_hostname != null && _hostname.equals(hostname)) {
            if (data.get("id") != null) {
                id = (Integer) data.get("id");
            }
        }
    }

    public static Integer getId() {
        return id;
    }

    public static void sendAboutMe() {
        Map<String, Object> aboutMe = new HashMap<>();
        aboutMe.put("hostname", Utils.getHostname());
        aboutMe.put("timestamp", System.currentTimeMillis());
        aboutMe.put("reference", getReference());

        MqttUtils.publish(MqttUtils.TOPIC_ABOUT_REPORTER, aboutMe, 1);
    }

}
