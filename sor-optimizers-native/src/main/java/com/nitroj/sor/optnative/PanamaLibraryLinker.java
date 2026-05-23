package com.nitroj.sor.optnative;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/** Base class for Java 25 FFM-style native library symbol resolution. */
public abstract class PanamaLibraryLinker {
    private final String libraryName;
    private final Path libraryPath;
    private final Set<String> symbols;

    protected PanamaLibraryLinker(final String libraryName, final String fileName, final Set<String> symbols) {
        this.libraryName = libraryName;
        final Path dir = Path.of(System.getProperty("sor.native.lib.dir", "sor-core/build/native"));
        this.libraryPath = dir.resolve(fileName);
        this.symbols = Set.copyOf(symbols);
        if (!Files.exists(libraryPath)) {
            throw new IllegalStateException("missing native library " + fileName + " at " + libraryPath);
        }
    }

    public String libraryName() {
        return libraryName;
    }

    public Path libraryPath() {
        return libraryPath;
    }

    public boolean resolves(final String symbol) {
        return symbols.contains(symbol);
    }

    public MemorySegment allocate(final long bytes) {
        return Arena.ofShared().allocate(bytes);
    }
}
