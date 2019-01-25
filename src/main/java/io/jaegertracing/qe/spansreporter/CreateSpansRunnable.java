package io.jaegertracing.qe.spansreporter;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import io.jaegertracing.internal.JaegerTracer;
import io.opentracing.Span;
import io.opentracing.tag.Tags;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateSpansRunnable implements Runnable {
    private boolean _printed = false;

    class SpansReporer extends TimerTask {

        private long delay = 0;

        public SpansReporer() {
            delay = config.getSpansCount() * 1000L; // delay in microseconds
        }

        @Override
        public void run() {
            if (!_printed) {
                _printed = true;
                logger.trace("Started sending spans, tracer:{}", name);
            }
            Map<String, Object> logs = new HashMap<>();
            logs.put("event", Tags.ERROR);
            logs.put("error.object", new RuntimeException());
            logs.put("class", this.getClass().getName());
            int count = 0;
            long startTime = System.currentTimeMillis();
            do {
                if (!config.isValid()) {
                    break;
                }
                count++;
                // emulate client spans
                Span span = tracer.buildSpan(name)
                        .withTag(Tags.COMPONENT.getKey(), "perf-test")
                        .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT)
                        .withTag(Tags.HTTP_METHOD.getKey(), "get")
                        .withTag(Tags.HTTP_STATUS.getKey(), 200)
                        .withTag(Tags.HTTP_URL.getKey(), "http://www.example.com/foo/bar?q=bar")
                        .start();
                span.log(logs);
                span.finish();
                try {
                    TimeUnit.MICROSECONDS.sleep(delay);
                } catch (InterruptedException ex) {
                    logger.error("exception, ", ex);
                }
            } while (count < config.getSpansCount());

            logger.trace("Reporting spans done, duration:{}ms, Tracer:{}",
                    System.currentTimeMillis() - startTime, name);
        }
    }

    private JaegerTracer tracer;
    private String name;
    private ReporterConfig config;
    private boolean close;

    public CreateSpansRunnable(JaegerTracer tracer, String name, ReporterConfig config, boolean close) {
        this.tracer = tracer;
        this.name = name;
        this.config = config;
        this.close = close;
    }

    @Override
    public void run() {
        logger.debug("Sending spans triggered for the tracer: {}", name);
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new SpansReporer(), new Date(config.getStartTime()), 1000L);
        try {
            while (config.isValid()) {
                TimeUnit.MILLISECONDS.sleep(500L);
            }
            timer.cancel();
        } catch (Exception ex) {
            logger.error("Exception,", ex);
        }

        if (close) {
            tracer.close();
        }
    }

}
