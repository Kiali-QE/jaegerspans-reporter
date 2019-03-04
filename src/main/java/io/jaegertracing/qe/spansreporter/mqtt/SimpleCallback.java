package io.jaegertracing.qe.spansreporter.mqtt;

import java.util.Map;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.util.SerializationUtils;

import io.jaegertracing.qe.spansquery.RemoteJaegerQueryRunnable;
import io.jaegertracing.qe.spansreporter.Utils;
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

    @SuppressWarnings("unchecked")
    public void messageArrived(String topic, MqttMessage message) {
        if (topic.startsWith(MqttUtils.TOPIC_SPAN_REPORTER)) {
            Map<String, Object> data = (Map<String, Object>) SerializationUtils.deserialize(message.getPayload());
            sendSpans(data);
        } else if (topic.startsWith(MqttUtils.TOPIC_SPAN_QUERY)) {
            Map<String, Object> data = (Map<String, Object>) SerializationUtils.deserialize(message.getPayload());
            executeQuery(data);
        } else if (topic.startsWith(MqttUtils.TOPIC_REPORTER_CONFIG)) {
            Map<String, Object> data = (Map<String, Object>) SerializationUtils.deserialize(message.getPayload());
            Utils.updateMe(data);
        } else if (topic.startsWith(MqttUtils.TOPIC_REQ_CONFIG)) {
            Utils.sendAboutMe();
        } else {
            logger.info("Message received: [topic:{}, payload:{}, qos:{}]",
                    topic, new String(message.getPayload()), message.getQos());
        }
    }

    public void deliveryComplete(IMqttDeliveryToken token) {
        StringBuilder topics = new StringBuilder();
        for (String topic : token.getTopics()) {
            topics.append(topic).append(", ");
        }
        logger.debug("delivery complete. [topics:{}]", topics);
    }

    private void sendSpans(Map<String, Object> data) {
        logger.debug("Data:{}", data);
        try {
            Reporter reporter = new Reporter(ReporterConfig.get(data));
            if (reporter.isValid()) {
                new Thread(reporter).start();
            }
        } catch (Exception ex) {
            logger.error("Exception,", ex);
        }
    }
    
    private void executeQuery(Map<String, Object> data) {
        logger.debug("Data:{}", data);
        try {
            RemoteJaegerQueryRunnable queryRunner = new RemoteJaegerQueryRunnable(ReporterConfig.get(data));
            if (queryRunner.isValid()) {
                new Thread(queryRunner).start();
            }
        } catch (Exception ex) {
            logger.error("Exception,", ex);
        }
    }

}
