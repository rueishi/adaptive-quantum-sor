package com.nitroj.sor.transport.aeron;

import com.nitroj.sor.api.ParentOrderRequest;
import com.nitroj.sor.api.SorEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Aeron-shaped server wrapper around a SOR engine. */
public final class AeronSorServer implements AutoCloseable {
    private final AeronChannel channel;
    private final SorEngine engine;
    private final Path mediaDriverDir;
    private final boolean embeddedDriver;
    private final AtomicBoolean running = new AtomicBoolean();

    public AeronSorServer(final String channelUri, final SorEngine engine) {
        this(channelUri, engine, null);
    }

    public AeronSorServer(final String channelUri, final SorEngine engine, final Path externalDriverDir) {
        this.channel = new AeronChannel(channelUri);
        this.engine = Objects.requireNonNull(engine, "engine must not be null");
        this.embeddedDriver = externalDriverDir == null;
        try {
            this.mediaDriverDir = embeddedDriver
                    ? Files.createTempDirectory("aeron-embedded-")
                    : externalDriverDir;
            Files.createDirectories(mediaDriverDir);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("could not create media driver directory", ex);
        }
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            InProcessAeronBroker.register(channel.uri(), this);
        }
    }

    public boolean running() {
        return running.get();
    }

    public Path mediaDriverDir() {
        return mediaDriverDir;
    }

    public boolean embeddedDriver() {
        return embeddedDriver;
    }

    long submit(final ParentOrderRequest request) {
        return engine.submitParentOrder(request);
    }

    SorEngine engine() {
        return engine;
    }

    @Override
    public void close() {
        if (running.compareAndSet(true, false)) {
            InProcessAeronBroker.unregister(channel.uri(), this);
        }
        if (embeddedDriver) {
            try {
                Files.deleteIfExists(mediaDriverDir);
            } catch (java.io.IOException ignored) {
                // best-effort cleanup for test media driver directory
            }
        }
    }
}
