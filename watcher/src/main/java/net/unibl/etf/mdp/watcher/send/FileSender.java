package net.unibl.etf.mdp.watcher.send;

import net.unibl.etf.mdp.common.config.AppConfig;
import net.unibl.etf.mdp.watcher.net.ParserClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.nio.file.NoSuchFileException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.BlockingQueue;

public class FileSender implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(FileSender.class);

    public static final Path OTROVNA_PILULA = Path.of("__KRAJ__");

    private final BlockingQueue<Path> red;
    private final Path processedDir;
    private final Set<String> uObradi;
    private final String host;
    private final int port;
    private final long stabilityWaitMs;

    private ParserClient klijent;

    public FileSender(BlockingQueue<Path> red, Path processedDir, Set<String> uObradi) {
        this.red = red;
        this.processedDir = processedDir;
        this.uObradi = uObradi;
        this.host = AppConfig.get("parser.host", "localhost");
        this.port = AppConfig.getInt("parser.port", 9000);
        this.stabilityWaitMs = AppConfig.getInt("watcher.stability.wait.ms", 200);
    }

    @Override
    public void run() {
        log.info("Sender nit pokrenuta");

        try {
            while (true) {
                Path fajl = red.take();

                if (fajl.equals(OTROVNA_PILULA)) {
                    log.info("Sender nit prima signal za gasenje");
                    break;
                }

                obradi(fajl);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("Sender nit prekinuta");
        } finally {
            zatvoriKlijenta();
        }
    }

    private void obradi(Path fajl) {
        try {
            if (!Files.exists(fajl)) {
                log.debug("Fajl {} vise ne postoji, preskacem", fajl.getFileName());
                return;
            }

            if (!sacekajStabilnost(fajl)) {
                log.warn("Fajl {} se nije stabilizovao, preskacem", fajl.getFileName());
                return;
            }

            byte[] sadrzaj = Files.readAllBytes(fajl);

            if (sadrzaj.length == 0) {
                log.warn("Fajl {} je prazan, preskacem", fajl.getFileName());
                return;
            }

            boolean ack = posaljiSaPonovnimPokusajem(fajl.getFileName().toString(), sadrzaj);

            if (ack) {
                premjesti(fajl);
            } else {
                log.warn("Nema ACK za fajl {}, ostaje u inbox folderu", fajl.getFileName());
            }

        } catch (NoSuchFileException e) {
            log.debug("Fajl {} je u medjuvremenu nestao", fajl.getFileName());
        } catch (IOException e) {
            log.error("Greska pri obradi fajla {}", fajl.getFileName(), e);
        } finally {
            uObradi.remove(fajl.toString());
        }
    }

    private boolean sacekajStabilnost(Path fajl) {
        try {
            long prethodnaVelicina = -1;

            for (int pokusaj = 0; pokusaj < 10; pokusaj++) {
                long velicina = Files.size(fajl);

                if (velicina > 0 && velicina == prethodnaVelicina) {
                    return true;
                }

                prethodnaVelicina = velicina;
                Thread.sleep(stabilityWaitMs);
            }
            return false;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (IOException e) {
            log.error("Greska pri provjeri velicine fajla {}", fajl.getFileName(), e);
            return false;
        }
    }

    private boolean posaljiSaPonovnimPokusajem(String imeFajla, byte[] sadrzaj) {
        for (int pokusaj = 1; pokusaj <= 2; pokusaj++) {
            try {
                if (klijent == null) {
                    klijent = new ParserClient(host, port);
                }
                return klijent.posalji(imeFajla, sadrzaj);

            } catch (IOException e) {
                log.warn("Pokusaj {} slanja fajla {} nije uspio: {}",
                        pokusaj, imeFajla, e.getMessage());
                zatvoriKlijenta();
            }
        }
        log.error("Slanje fajla {} nije uspjelo ni nakon ponovnog pokusaja", imeFajla);
        return false;
    }

    private void premjesti(Path fajl) throws IOException {
        Path odrediste = processedDir.resolve(fajl.getFileName());

        if (Files.exists(odrediste)) {
            String ime = fajl.getFileName().toString();
            odrediste = processedDir.resolve(System.currentTimeMillis() + "_" + ime);
        }

        try {
            Files.move(fajl, odrediste, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(fajl, odrediste, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("Fajl {} premjesten u processed kao {}",
                fajl.getFileName(), odrediste.getFileName());
    }

    private void zatvoriKlijenta() {
        if (klijent != null) {
            try {
                klijent.close();
            } catch (IOException e) {
                log.debug("Greska pri zatvaranju klijenta", e);
            }
            klijent = null;
        }
    }
}