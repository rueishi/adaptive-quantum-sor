package com.nitroj.sor.client;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SdkApiCompatibilityTest {
    @Test
    void publicMethodManifestMatchesCommittedSnapshot() throws Exception {
        final List<String> actual = Arrays.stream(new Class<?>[]{AeronSorClient.class, SorClientConfig.class})
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods())
                        .filter(method -> Modifier.isPublic(method.getModifiers()))
                        .map(method -> type.getName() + "#" + method.getName()
                                + "(" + Arrays.stream(method.getParameterTypes())
                                .map(Class::getName)
                                .reduce((left, right) -> left + "," + right)
                                .orElse("") + "):" + method.getReturnType().getName()))
                .sorted()
                .toList();
        final List<String> expected = Files.readAllLines(Path.of(
                "sor-client-java/src/test/resources/public-api-signatures.txt"))
                .stream()
                .sorted(Comparator.naturalOrder())
                .toList();
        assertEquals(expected, actual);
    }
}
