package io.jaegertracing.qe.spansreporter;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.jaegertracing.internal.reporters.RemoteReporter;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.jaegertracing.spi.Sender;
import io.jaegertracing.thrift.internal.senders.HttpSender;
import io.jaegertracing.thrift.internal.senders.UdpSender;
import lombok.extern.slf4j.Slf4j;
import io.jaegertracing.internal.JaegerTracer;

@Slf4j
public class Reporter implements Runnable {

    private static final AtomicBoolean _RUNNING = new AtomicBoolean(false);

    private ReporterConfig config;

    public Reporter(ReporterConfig config) {
        this.config = config;
    }

    private void startReporter() {

        logger.debug("{}", config);
        if (_RUNNING.get()) {
            logger.warn("Reporter busy. Try after some time");
            return;
        }

        try {
            _RUNNING.set(true);
            logger.info("Loading reportig spans configurations, Sending spans will start @ {}",
                    new Date(config.getStartTime()));
            long startTime = System.currentTimeMillis();
            ExecutorService executor = Executors.newFixedThreadPool(config.getTracersCount());
            List<Future<?>> futures = new ArrayList<>(config.getTracersCount());
            Set<String> serviceNames = new LinkedHashSet<>();
            String hostname = Utils.getHostname();
            for (int tracerNumber = 1; tracerNumber <= config.getTracersCount(); tracerNumber++) {
                String name = null;
                if (config.getUseHostname()) {
                    name = String.format("%s_%s_%d", config.getTracerName(), hostname, tracerNumber);
                } else {
                    name = String.format("%s_%d", config.getTracerName(), tracerNumber);
                }

                String serviceName = String.format("%s-%s", name, config.getServiceName());

                JaegerTracer tracer = createJaegerTracer(serviceName);
                serviceNames.add(tracer.getServiceName());
                Runnable worker = new CreateSpansRunnable(tracer, name, config, true);
                futures.add(executor.submit(worker));
            }

            for (Future<?> future : futures) {
                future.get();
            }
            executor.shutdownNow();
            executor.awaitTermination(3, TimeUnit.SECONDS);
            logger.info("reporting spans completed in {} ms", System.currentTimeMillis() - startTime);
        } catch (Exception ex) {
            logger.error("Exception, unable to start reporter,", ex);
        } finally {
            _RUNNING.set(false);
            // send status
        }
    }

    private JaegerTracer createJaegerTracer(String serviceName) {
        Sender sender;
        if (config.getSender().equalsIgnoreCase("udp")) {
            sender = new UdpSender(
                    config.getJaegerAgentHost(),
                    config.getJaegerAgentPort(),
                    config.getJaegerMaxPocketSize());
            logger.info("Using UDP sender, sending to: {}:{}",
                    config.getJaegerAgentHost(), config.getJaegerAgentPort());
        } else {
            // use the collector
            String httpEndpoint = "http://" + config.getJaegerCollectorHost() + ":" + config.getJaegerCollectorPort()
                    + "/api/traces";
            logger.info("Using HTTP sender, sending to endpoint: {}", httpEndpoint);
            sender = new HttpSender.Builder(httpEndpoint).build();
        }

        logger.info("Flush interval {}, queue size {}", config.getJaegerFlushInterval(),
                config.getJaegerMaxQueueSize());
        RemoteReporter reporter = new RemoteReporter.Builder()
                .withSender(sender)
                .withMaxQueueSize(config.getJaegerMaxQueueSize())
                .withFlushInterval(config.getJaegerFlushInterval())
                .build();

        return new JaegerTracer.Builder(serviceName)
                .withReporter(reporter)
                .withSampler(new ConstSampler(true))
                .build();
    }

    @Override
    public void run() {
        this.startReporter();
    }

}
