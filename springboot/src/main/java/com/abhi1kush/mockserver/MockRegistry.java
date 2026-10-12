package com.abhi1kush.mockserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MockRegistry {
    private static final Logger log = LoggerFactory.getLogger(MockRegistry.class);
    private final Map<String, MockDefinition> mocks = new LinkedHashMap<>();

    public MockRegistry(ObjectMapper mapper, @Value("${mocks.dir:../mocks}") String mocksDir) throws IOException {
        Path dir = Paths.get(mocksDir);
        if (!Files.isDirectory(dir)) {
            log.warn("Mocks directory {} not found; no mocks loaded", dir.toAbsolutePath());
            return;
        }
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : files.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
                MockDefinition m = mapper.readValue(f.toFile(), MockDefinition.class);
                if (m.path() == null) { log.warn("Skipping {}: no path", f); continue; }
                mocks.put(key(m.normalizedMethod(), m.path()), m);
                log.info("Loaded mock {} {} from {}", m.normalizedMethod(), m.path(), f.getFileName());
            }
        }
    }

    private static String key(String method, String path) { return method.toUpperCase() + " " + path; }

    public Optional<MockDefinition> find(String method, String path) {
        return Optional.ofNullable(mocks.get(key(method, path)));
    }

    public int size() { return mocks.size(); }
}
