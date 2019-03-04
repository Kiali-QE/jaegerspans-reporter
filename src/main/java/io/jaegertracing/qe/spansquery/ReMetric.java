package io.jaegertracing.qe.spansquery;

import java.util.HashMap;
import java.util.Map;

import lombok.Builder.Default;

import lombok.Getter;
import lombok.ToString;
import lombok.Builder;

@Builder
@ToString
@Getter
public class ReMetric {
    private String suiteId;
    private String name;
    @Default
    private Map<String, Object> data = new HashMap<>();
}
