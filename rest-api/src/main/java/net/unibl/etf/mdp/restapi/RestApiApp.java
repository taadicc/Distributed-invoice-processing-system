package net.unibl.etf.mdp.restapi;

import com.sun.net.httpserver.HttpServer;

import net.unibl.etf.mdp.common.config.AppConfig;
import net.unibl.etf.mdp.restapi.http.ClientHandler;
import net.unibl.etf.mdp.restapi.http.InvoiceHandler;
import net.unibl.etf.mdp.restapi.http.StatsHandler;
import net.unibl.etf.mdp.restapi.redis.RedisClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RestApiApp {

    private static final Logger log = LoggerFactory.getLogger(RestApiApp.class);

    public static void main(String[] args) throws Exception {

        int port = AppConfig.getInt("rest.port", 8080);
        int niti = AppConfig.getInt("rest.threads", 10);

        log.info("Pokretanje REST API servisa...");

        if (!RedisClient.provjeri()) {
            log.error("Redis nije dostupan, gasim se");
            System.exit(1);
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/invoices", new InvoiceHandler());
        server.createContext("/api/clients", new ClientHandler());
        server.createContext("/api/stats", new StatsHandler());

        ExecutorService pool = Executors.newFixedThreadPool(niti);
        server.setExecutor(pool);

        server.start();

        log.info("REST API servis slusa na http://localhost:{}", port);
        log.info("Dostupni endpointi:");
        log.info("  GET http://localhost:{}/api/invoices", port);
        log.info("  GET http://localhost:{}/api/invoices/{{id}}", port);
        log.info("  GET http://localhost:{}/api/invoices?status=approved", port);
        log.info("  GET http://localhost:{}/api/clients/{{jib}}/summary", port);
        log.info("  GET http://localhost:{}/api/stats", port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Gasenje REST API servisa...");
            server.stop(2);
            pool.shutdown();
        }));
    }
}