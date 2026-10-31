package net.unibl.etf.mdp.watcher;

import net.unibl.etf.mdp.common.config.AppConfig;
import net.unibl.etf.mdp.watcher.fs.InboxWatcher;
import net.unibl.etf.mdp.watcher.send.FileSender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WatcherApp {

    private static final Logger log = LoggerFactory.getLogger(WatcherApp.class);

    public static void main(String[] args) throws IOException {

        Path inboxDir = Path.of(AppConfig.get("inbox.dir", "./data/invoices/inbox"));
        Path processedDir = Path.of(AppConfig.get("processed.dir", "./data/invoices/processed"));

        int brojNiti = AppConfig.getInt("watcher.threads", 4);
        int kapacitet = AppConfig.getInt("watcher.queue.capacity", 1000);

        Files.createDirectories(inboxDir);
        Files.createDirectories(processedDir);

        log.info("Pokretanje Watcher servisa...");
        log.info("Inbox: {}", inboxDir.toAbsolutePath());
        log.info("Processed: {}", processedDir.toAbsolutePath());
        log.info("Sender niti: {}, kapacitet reda: {}", brojNiti, kapacitet);

        BlockingQueue<Path> red = new ArrayBlockingQueue<>(kapacitet);
        Set<String> uObradi = ConcurrentHashMap.newKeySet();
        
        ExecutorService senderPool = Executors.newFixedThreadPool(brojNiti);
        for (int i = 0; i < brojNiti; i++) {
            senderPool.submit(new FileSender(red, processedDir, uObradi));
        }

        Thread watcherNit = new Thread(new InboxWatcher(inboxDir, red, uObradi), "inbox-watcher");
        watcherNit.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Gasenje Watcher servisa...");
            watcherNit.interrupt();

            for (int i = 0; i < brojNiti; i++) {
                try {
                    red.put(FileSender.OTROVNA_PILULA);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            senderPool.shutdown();
            try {
                senderPool.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            log.info("Watcher servis zaustavljen");
        }));

        log.info("Watcher servis je spreman.");
    }
}