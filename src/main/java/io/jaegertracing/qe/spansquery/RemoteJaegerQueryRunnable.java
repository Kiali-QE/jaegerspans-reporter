package io.jaegertracing.qe.spansquery;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.DurationFormatUtils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jaegertracing.qe.spansreporter.Utils;
import io.opentracing.tag.Tags;
import io.jaegertracing.qe.spansreporter.ReporterConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request.Builder;
import okhttp3.Response;

@Slf4j
public class RemoteJaegerQueryRunnable implements Closeable, Runnable {

    private static final int DEFAULT_LIMIT = 20;
    private OkHttpClient okClient;
    private ObjectMapper objectMapper;
    private ReporterConfig config;

    private List<ReMetric> metrics = new ArrayList<>();

    String queryUrl = "http://" + config.getJaegerQueryHost() + ":" + config.getJaegerQueryPort();

    private static final String URL_SERVICE_LIMIT = "%s/api/traces?service=%s&limit=%d&lookback=1h";
    private static final String URL_SERVICE_LIMIT_TAGS = "%s/api/traces?service=%s&limit=%d&lookback=1h&tags=%s";
    private static final String URL_SERVICE_LIMIT_OPERATION = "%s/api/traces?service=%s&limit=%d&lookback=1h&operation=%s";
    private static final String URL_SERVICE_LIMIT_OPERATION_TAGS = "%s/api/traces?service=%s&limit=%d&lookback=1h&operation=%s&tags=%s";

    public RemoteJaegerQueryRunnable(ReporterConfig config) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.config = config;

        this.okClient = new OkHttpClient.Builder()
                .readTimeout(10, TimeUnit.MINUTES)
                .build();
    }

    public boolean isValid() {

        if (config.getQueryHostCount() < 0) {
            return true;
        } else if (Utils.getId() <= config.getQueryHostCount()) {
            return true;
        }
        return false;

    }

    static Map<String, String> getNonseseTags() {
        Map<String, String> tags = new HashMap<>();
        tags.put("fooo.bar1", "fobarhax*+??");
        tags.put("fooo.ba2sar", "true");
        tags.put("fooo.ba4342r", "1");
        tags.put("fooo.ba24r*?%", "hehe");
        tags.put("fooo.bar*?%http.d6cconald", "hehuhoh$?ij");
        tags.put("fooo.bar*?%http.do**2nald", "goobarRAXbaz");
        tags.put("fooo.bar*?%http.don(a44ld", "goobarRAXbaz");
        return tags;
    }

    static Map<String, String> getTags() {
        Map<String, String> tags = new HashMap<>();
        tags.put(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);
        tags.put(Tags.HTTP_METHOD.getKey(), "get");
        tags.put(Tags.HTTP_METHOD.getKey(), "get");
        return tags;
    }

    @Override
    public void close() {
        okClient.dispatcher().executorService().shutdown();
        okClient.connectionPool().evictAll();
    }

    private Map<String, String> createQueries() {
        Map<String, String> urlsMap = new HashMap<>();

        urlsMap.put("urlServiceDefaultLimit",
                String.format(URL_SERVICE_LIMIT, queryUrl, config.getServiceName(), DEFAULT_LIMIT));
        urlsMap.put("urlServiceCustomLimit",
                String.format(URL_SERVICE_LIMIT, queryUrl, config.getServiceName(), config.getJaegerQueryLimit()));
        urlsMap.put(
                "urlServiceOperationDefaultLimit",
                String.format(URL_SERVICE_LIMIT_OPERATION, queryUrl, config.getServiceName(), DEFAULT_LIMIT,
                        config.getJaegerQueryOperation()));
        urlsMap.put(
                "urlServiceOperationCustomLimit",
                String.format(URL_SERVICE_LIMIT_OPERATION, queryUrl, config.getServiceName(),
                        config.getJaegerQueryLimit(), config.getJaegerQueryOperation()));

        updateWithTags(urlsMap, "Nonsense", getNonseseTags());
        updateWithTags(urlsMap, "Tags", getTags());

        return urlsMap;
    }

    private void updateWithTags(Map<String, String> urlsMap, String name, Map<String, String> tags) {
        String tagsQueryString = getTagsQueryString(urlsMap);
        urlsMap.put("urlServiceLimitTagsDefaultLimit" + name,
                String.format(URL_SERVICE_LIMIT_TAGS, queryUrl, config.getServiceName(), DEFAULT_LIMIT,
                        tagsQueryString));
        urlsMap.put("urlServiceLimitTagsCustomLimit" + name,
                String.format(URL_SERVICE_LIMIT_TAGS, queryUrl, config.getServiceName(), config.getJaegerQueryLimit(),
                        tagsQueryString));
        urlsMap.put("urlServiceOperationTagsDefaultLimit" + name,
                String.format(URL_SERVICE_LIMIT_OPERATION_TAGS, queryUrl, config.getServiceName(), DEFAULT_LIMIT,
                        config.getJaegerQueryOperation(), tagsQueryString));
        urlsMap.put(
                "urlServiceOperationTagsCustomLimit" + name,
                String.format(URL_SERVICE_LIMIT_OPERATION_TAGS, queryUrl, config.getServiceName(),
                        config.getJaegerQueryLimit(), config.getJaegerQueryOperation(), tagsQueryString));

    }

    public void execute() {
        long start = 0;
        long startOverAll = System.currentTimeMillis();
        for (int sample = 1; sample <= config.getJaegerQuerySamples(); sample++) {
            start = System.currentTimeMillis();
            executeQueries(sample);
            logger.debug("QueryRun, sample:{}, timeTaken:{}",
                    sample, DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start));
        }
        logger.debug("Overall time taken for the, samples:{}, timeTaken:{}",
                config.getJaegerQuerySamples(),
                DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - startOverAll));
    }

    private void updateMetric(ReMetric metric, long duration, int sample) {
        metric.getData().put("timetaken", duration);
        metric.getData().put("iteration", config.getJaegerQueryIteration());
        metric.getData().put("sample", sample);
        metric.getData().put("hostname", Utils.getHostname());
    }

    private void executeQueries(int sample) {
        Map<String, String> queriesMap = createQueries();
        for (Map.Entry<String, String> query : queriesMap.entrySet()) {
            try {
                long start = System.currentTimeMillis();
                Response response = okClient.newCall(new Builder()
                        .url(query.getValue())
                        .build())
                        .execute();

                if (!response.isSuccessful()) {
                    logger.warn("Not successful request, response code:{}, message:{}.",
                            response.code(), response.message());
                }

                response.body().string();
                long duration = System.currentTimeMillis() - start;
                ReMetric metric = ReMetric.builder()
                        .id(config.getReportEngineSuiteId())
                        .name(query.getKey())
                        .build();
                updateMetric(metric, duration, sample);
                metrics.add(metric);
                logger.trace("[{}] {}: {}",
                        DurationFormatUtils.formatDurationHMS(duration), query.getKey(), query.getValue());
                response.close();
            } catch (IOException ex) {
                logger.error("Exception,", ex);
            }
        }
    }

    private String getTagsQueryString(Map<String, String> tags) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            if (stringBuilder.length() != 1) {
                stringBuilder.append(",");
            }
            stringBuilder.append(String.format("\"%s\":\"%s\"", entry.getKey(), entry.getValue()));
        }
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    @Override
    public void run() {
        try {
            logger.debug("Triggered jaeger query execution");
            execute();
            logger.debug("Completed jaeger query execution");
        } catch (Exception ex) {
            logger.error("Exception,", ex);
        } finally {
            try {
                if (metrics.size() > 0) {
                    ReportEngineClient reClient = new ReportEngineClient(config.getReportEngineUrl());
                    reClient.addMetricsData(metrics);
                }
            } catch (Exception ex) {
                logger.error("Exception,", ex);
            }
        }
    }
}
