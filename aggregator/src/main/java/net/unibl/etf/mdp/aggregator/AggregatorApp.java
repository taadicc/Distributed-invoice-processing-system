package net.unibl.etf.mdp.aggregator;

import net.unibl.etf.mdp.aggregator.mq.AggregatorListener;
import net.unibl.etf.mdp.aggregator.redis.RedisClient;
import net.unibl.etf.mdp.aggregator.udp.MulticastNotifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AggregatorApp {

    private static final Logger log = LoggerFactory.getLogger(AggregatorApp.class);

    public static void main(String[] args) {

        log.info("Pokretanje Aggregator servisa...");

        if (!RedisClient.provjeri()) {
            log.error("Redis nije dostupan, gasim se");
            System.exit(1);
        }

        MulticastNotifier notifier;
        try {
            notifier = new MulticastNotifier();
        } catch (Exception e) {
            log.error("Ne mogu inicijalizovati UDP multicast", e);
            System.exit(1);
            return;
        }

        AggregatorListener listener = new AggregatorListener(notifier);

        try {
            listener.start();
        } catch (Exception e) {
            log.error("Ne mogu se pretplatiti na MQ topike", e);
            System.exit(1);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Gasenje Aggregator servisa...");
            listener.close();
            notifier.close();
        }));

        log.info("Aggregator servis je spreman.");
    }
}