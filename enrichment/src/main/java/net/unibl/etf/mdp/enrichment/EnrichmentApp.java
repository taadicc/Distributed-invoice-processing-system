package net.unibl.etf.mdp.enrichment;

import net.unibl.etf.mdp.enrichment.mq.RetryListener;
import net.unibl.etf.mdp.enrichment.mq.ValidatedListener;
import net.unibl.etf.mdp.enrichment.redis.RedisClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnrichmentApp {

    private static final Logger log = LoggerFactory.getLogger(EnrichmentApp.class);

    public static void main(String[] args) {

        log.info("Pokretanje Enrichment servisa...");

        if (!RedisClient.provjeri()) {
            log.error("Redis nije dostupan, gasim se");
            System.exit(1);
        }

        ValidatedListener validatedListener = new ValidatedListener();
        RetryListener retryListener = new RetryListener();

        try {
            validatedListener.start();
            retryListener.start();
        } catch (Exception e) {
            log.error("Ne mogu se pretplatiti na MQ topike", e);
            System.exit(1);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Gasenje Enrichment servisa...");
            validatedListener.close();
            retryListener.close();
        }));

        log.info("Enrichment servis je spreman (validated + retry listener).");
    }
}