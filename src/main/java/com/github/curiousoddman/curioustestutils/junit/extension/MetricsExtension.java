package com.github.curiousoddman.curioustestutils.junit.extension;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.*;

@Slf4j
public class MetricsExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {
    private static final ThreadLocal<MeterRegistry> REGISTRY = new ThreadLocal<>();

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        REGISTRY.set(new SimpleMeterRegistry());
        Metrics.globalRegistry.add(REGISTRY.get());
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        Metrics.globalRegistry.clear();
        Metrics.globalRegistry.remove(REGISTRY.get());
        REGISTRY.get().clear();
        REGISTRY.remove();
    }

    @Override
    public ExtensionContextScope getTestInstantiationExtensionContextScope(ExtensionContext rootContext) {
        return ExtensionContextScope.TEST_METHOD;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType() == MeterRegistry.class;
    }

    @Override
    public MeterRegistry resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return REGISTRY.get();
    }
}
