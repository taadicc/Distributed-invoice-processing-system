package net.unibl.etf.mdp.watcher.fs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Stream;

public class InboxWatcher implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(InboxWatcher.class);

    private final Path inboxDir;
    private final BlockingQueue<Path> red;
    private final Set<String> uObradi;

    public InboxWatcher(Path inboxDir, BlockingQueue<Path> red, Set<String> uObradi) {
        this.inboxDir = inboxDir;
        this.red = red;
        this.uObradi = uObradi;
    }

    @Override
    public void run() {
        try {
            skenirajPostojece();
            pratiPromjene();
        } catch (IOException e) {
            log.error("Greska u radu Watcher niti", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("Watcher nit prekinuta");
        }
    }

    private void skenirajPostojece() throws IOException, InterruptedException {
        log.info("Skeniram postojece fajlove u {}", inboxDir);

        List<Path> fajlovi;
        try (Stream<Path> tok = Files.list(inboxDir)) {
            fajlovi = tok.filter(Files::isRegularFile)
                         .filter(p -> p.toString().toLowerCase().endsWith(".xml"))
                         .toList();
        }

        for (Path fajl : fajlovi) {
            ubaciURed(fajl);
        }

        log.info("Skeniranje zavrseno, ubaceno {} fajlova", fajlovi.size());
    }

    private void pratiPromjene() throws IOException, InterruptedException {

        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {

            inboxDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
            log.info("Pracenje foldera {} je aktivno", inboxDir);

            while (true) {
                WatchKey key = watchService.take();

                for (WatchEvent<?> event : key.pollEvents()) {

                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        log.warn("OVERFLOW! Neki dogadjaji su izgubljeni, radim ponovno skeniranje");
                        skenirajPostojece();
                        continue;
                    }

                    Path imeFajla = (Path) event.context();
                    Path punaPutanja = inboxDir.resolve(imeFajla);

                    if (!imeFajla.toString().toLowerCase().endsWith(".xml")) {
                        log.debug("Preskacem fajl koji nije XML: {}", imeFajla);
                        continue;
                    }

                    ubaciURed(punaPutanja);
                }

                boolean validan = key.reset();
                if (!validan) {
                    log.error("Folder {} vise nije dostupan, prekidam pracenje", inboxDir);
                    break;
                }
            }
        }
    }

    private void ubaciURed(Path fajl) throws InterruptedException {
        if (!uObradi.add(fajl.toString())) {
            log.debug("Fajl {} je vec u redu, preskacem duplikat", fajl.getFileName());
            return;
        }
        log.info("Detektovan fajl: {}", fajl.getFileName());
        red.put(fajl);
    }
}