package io.jaegertracing.qe.spansreporter.mqtt;

import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.util.SerializationUtils;

import io.jaegertracing.qe.spansreporter.Utils;
import io.jaegertracing.qe.spansreporter.BeanUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MqttUtils {
    public static final String TOPIC_SPAN_REPORTER = "jaegerqe/spansreporter";
    public static final String TOPIC_SPAN_QUERY = "jaegerqe/spansquery";
    public static final String TOPIC_ABOUT_REPORTER = "jaegerqe/aboutreporter";
    public static final String TOPIC_REQ_CONFIG = "jaegerqe/reqconfig";
    public static final String TOPIC_REPORTER_CONFIG = "jaegerqe/reporterconfig";
    public static final String TOPIC_TEST = "jaegerqe/test";

    private static MqttClient CLIENT;

    public static MqttConf config() {
        return BeanUtil.getBean(MqttConf.class);
    }

    public static MqttClient client() {
        isConnected();
        return CLIENT;
    }

    private static boolean isConnected() {
        if (CLIENT == null || !CLIENT.isConnected()) {
            connect();
        }
        if (!CLIENT.isConnected()) {
            logger.error("MQTT client is not connected!");
        }
        return CLIENT.isConnected();
    }

    private static String clientId() {
        return Utils.getHostname() + RandomStringUtils.randomAlphabetic(5);
    }

    public static void connect() {
        MemoryPersistence persistence = new MemoryPersistence();
        MqttConf conf = config();
        logger.debug("Connecting to the MQTT broker, {}", conf);
        try {
            CLIENT = new MqttClient(conf.getHostUrl(), clientId(), persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setUserName(conf.getUser());
            connOpts.setPassword(conf.getPassword().toCharArray());
            connOpts.setCleanSession(true);
            connOpts.setConnectionTimeout(30000); // 30 seconds
            CLIENT.setCallback(new SimpleCallback());
            CLIENT.connectWithResult(connOpts);
            logger.debug("Connected to the MQTT broker. Connection status:{}", CLIENT.isConnected());
            subscribe(TOPIC_SPAN_REPORTER + "/#",
                    TOPIC_SPAN_QUERY + "/#",
                    TOPIC_TEST + "/#",
                    TOPIC_REPORTER_CONFIG + "/#",
                    TOPIC_REQ_CONFIG + "/#");
        } catch (MqttException ex) {
            logger.error("Exception,", ex);
        }
    }

    public static void subscribe(String... topics) {
        if (isConnected()) {
            try {
                CLIENT.subscribe(topics);
            } catch (MqttException ex) {
                logger.error("Exception,", ex);
            }
        }
    }

    public static void publish(String topic, Object payload, int qos) {
        if (isConnected()) {
            MqttMessage message = new MqttMessage();
            message.setPayload(SerializationUtils.serialize(payload));
            message.setQos(qos);
            try {
                CLIENT.publish(topic, message);
            } catch (MqttException ex) {
                logger.error("Exception,", ex);
            }
        }
    }
}
