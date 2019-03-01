package io.jaegertracing.qe.spansreporter;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.Builder.Default;
import lombok.ToString;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@ToString
public class ReporterConfig implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1070098260648917029L;

    @Default
    private AtomicBoolean stop = new AtomicBoolean(false);

    private String jobId;

    private Long startTime;
    private Long endTime;

    private Integer reporterHostCount;
    private Integer queryHostCount;

    private String reportEngineUrl;
    private String reportEngineSuiteId;

    private String serviceName;
    private Integer tracersCount;
    private Integer spansCount; // spans per tracer
    private Boolean useHostname;

    private String sender; // http, udp

    private String jaegerCollectorHost;
    private Integer jaegerCollectorPort;

    private String jaegerQueryHost;
    private Integer jaegerQueryPort;
    private Integer jaegerQueryLimit;
    private Integer jaegerQuerySamples;
    private Integer jaegerQueryIteration;
    private String jaegerQueryOperation;

    private String jaegerAgentHost;
    private Integer jaegerAgentPort;

    private Float jaegerSamplingRate;
    private Integer jaegerFlushInterval;
    private Integer jaegerMaxPocketSize;
    private Integer jaegerMaxQueueSize;

    public static ReporterConfig get(Map<String, Object> map) {
        ReporterConfig config = ReporterConfig.builder()
                .serviceName((String) get(map, "serviceName", "service"))
                .spansCount((Integer) get(map, "spansCount", 3))
                .tracersCount((Integer) get(map, "tracersCount", 2))
                .sender((String) get(map, "sender", "udp"))
                .jaegerCollectorHost((String) get(map, "jaegerCollectorHost", "localhost"))
                .jaegerCollectorPort((Integer) get(map, "jaegerCollectorPort", 14268))
                .jaegerQueryHost((String) get(map, "jaegerQueryHost", "localhost"))
                .jaegerQueryPort((Integer) get(map, "jaegerQueryPort", 16886))
                .jaegerQueryLimit((Integer) get(map, "jaegerQueryLimit", 2000))
                .jaegerQuerySamples((Integer) get(map, "jaegerQuerySamples", 5))
                .jaegerQueryIteration((Integer) get(map, "jaegerQueryIteration", -1))
                .jaegerAgentHost((String) get(map, "jaegerAgentHost", "localhost"))
                .jaegerAgentPort((Integer) get(map, "jaegerAgentPort", 6831))
                .jaegerSamplingRate(((Double) get(map, "jaegerSamplingRate", 1.0)).floatValue())
                .jaegerFlushInterval((Integer) get(map, "jaegerFlushInterval", 200))
                .jaegerMaxPocketSize((Integer) get(map, "jaegerMaxPocketSize", 0))
                .jaegerMaxQueueSize((Integer) get(map, "jaegerMaxQueueSize", 10000))
                .jobId((String) get(map, "jobId", null))
                .useHostname((Boolean) get(map, "useHostname", false))
                .reporterHostCount((Integer) get(map, "reporterHostCount", -1))
                .queryHostCount((Integer) get(map, "queryHostCount", -1))
                .reportEngineUrl((String) get(map, "reportEngineUrl", "http://localhost:18081"))
                .reportEngineSuiteId((String) get(map, "reportEngineSuiteId", null))
                .jaegerQueryOperation((String) get(map, "jaegerQueryOperation", "test"))
                .build();
        // update startTime and endTime
        config.setStartTime(getTimestamp(get(map, "startTime", System.currentTimeMillis()), 0L));
        config.setEndTime(getTimestamp(get(map, "endTime", null), config.getStartTime()));
        return config;
    }

    private static Object get(Map<String, Object> map, String key, Object defaultValue) {
        if (map.containsKey(key)) {
            return map.get(key);
        }
        return defaultValue;
    }

    private static Long getTimestamp(Object value, Long refTimestamp) {
        if (value == null) {
            return null;
        }
        if (refTimestamp == null) {
            refTimestamp = 0L;
        }

        String strValue = String.valueOf(value);
        if (strValue.contains("now")) {
            return System.currentTimeMillis() + refTimestamp;
        } else {
            Long number = Long.valueOf(strValue.replaceAll("[^0-9]", ""));
            Long timestamp = null;
            if (strValue.endsWith("s")) {
                timestamp = number * 1000L;
            } else if (strValue.endsWith("m")) {
                timestamp = number * 1000L * 60;
            } else if (strValue.endsWith("h")) {
                timestamp = number * 1000L * 60 * 60;
            } else if (strValue.endsWith("d")) {
                timestamp = number * 1000L * 60 * 60 * 24;
            } else {
                timestamp = number;
            }

            timestamp += refTimestamp;
            if (timestamp < 1546281000000L) { // 2019-JAN-01
                timestamp += System.currentTimeMillis();
            }
            return timestamp;
        }
    }

    public Long getStartTime() {
        if (startTime == null) {
            startTime = System.currentTimeMillis();
        }
        return startTime;
    }

    public boolean isValid() {
        if (stop.get()) {
            return false;
        } else if (endTime == null) {
            return true;
        } else if (System.currentTimeMillis() < endTime) {
            return true;
        }
        return false;
    }

}
