package net.unibl.etf.mdp.parser;

import net.unibl.etf.mdp.common.config.AppConfig;
import net.unibl.etf.mdp.parser.net.TcpServer;
import net.unibl.etf.mdp.parser.redis.RedisClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.unibl.etf.mdp.parser.mq.InvoicePublisher;
public class ParserApp {

    private static final Logger log = LoggerFactory.getLogger(ParserApp.class);

    public static void main(String[] args) {

        int port = AppConfig.getInt("parser.port", 9000);
        int brojNiti = AppConfig.getInt("parser.threads", 10);

        log.info("Pokretanje Parser servisa...");

        if (!RedisClient.provjeri()) {
            log.error("Redis nije dostupan, gasim se");
            System.exit(1);
        }

        if (!InvoicePublisher.provjeri()) {
            log.error("ActiveMQ nije dostupan, gasim se");
            System.exit(1);
        }

        TcpServer server = new TcpServer(port, brojNiti);
        server.start();
    }
}