package io.jaegertracing.qe.spansreporter.mqtt;

import java.util.Map;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.util.SerializationUtils;

import io.jaegertracing.qe.spansreporter.Reporter;
import io.jaegertracing.qe.spansreporter.ReporterConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleCallback implements MqttCallback {

    public void connectionLost(Throwable throwable) {
        logger.error("Connection lost to the broker [{}], wait 10 seconds and reconnect",
                MqttUtils.config().getHostUrl());
        try {
            Thread.sleep(1000 * 10);
        } catch (InterruptedException ex) {
            logger.error("Exception,", ex);
        }
        MqttUtils.connect();
    }

    public void messageArrived(String topic, MqttMessage message) {
        if (topic.startsWith(MqttUtils.TOPIC_SPAN_REPORTER)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) SerializationUtils.deserialize(message.getPayload());
            sendSpans(data);
        } else {
            logger.info("Message received: [topic:{}, payload:{}, qos:{}]",
                    topic, new String(message.getPayload()), message.getQos());
        }
    }

    public void deliveryComplete(IMqttDeliveryToken token) {
        logger.debug("delivery complete. {}", token.toString());
    }

    private void sendSpans(Map<String, Object> data) {
        logger.debug("Data:{}", data);
        Reporter reporter = new Reporter(ReporterConfig.get(data));
        new Thread(reporter).start();
    }

}
