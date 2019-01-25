package io.jaegertracing.qe.spansreporter.mqtt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@Data
@ConfigurationProperties("mqtt.broker")
public class MqttConf {
    private String host;
    private Integer port;
    private String user;
    private String password;

    public String getHostUrl() {
        return String.format("tcp://%s:%d", this.host, this.port);
    }
}
