package me.redst.casualMode.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AsyncFileWriter implements AutoCloseable {

    private final Logger logger;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "CasualMode file writer");
        thread.setDaemon(true);
        return thread;
    });

    public AsyncFileWriter(Logger logger) {
        this.logger = logger;
    }

    public void write(Path target, String content) {
        try {
            executor.execute(() -> writeLogged(target, content));
        } catch (RejectedExecutionException e) {
            writeLogged(target, content);
        }
    }

    public void flush() {
        try {
            executor.submit(() -> { }).get(10, TimeUnit.SECONDS);
        } catch (RejectedExecutionException ignored) {
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Timed out waiting for CasualMode files to be saved.", e);
        }
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warning("Some CasualMode files may not have been saved.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void writeLogged(Path target, String content) {
        try {
            writeNow(target, content);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not save " + target.getFileName() + ": " + e.getMessage(), e);
        }
    }

    public static void writeNow(Path target, String content) throws IOException {
        Files.createDirectories(target.toAbsolutePath().getParent());
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(temporary, content, StandardCharsets.UTF_8);
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Files.writeString(target, content, StandardCharsets.UTF_8);
            Files.deleteIfExists(temporary);
        }
    }
}
