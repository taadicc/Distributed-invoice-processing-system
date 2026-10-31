package net.unibl.etf.mdp.restapi.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import net.unibl.etf.mdp.restapi.redis.RedisClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class InvoiceHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(InvoiceHandler.class);

    private static final String INVOICE_PREFIX = "invoice:";
    private static final String STATUS_PREFIX = "invoices:status:";
    private static final String SVE_FAKTURE = "invoices:all";

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        String putanja = exchange.getRequestURI().getPath();
        String metoda = exchange.getRequestMethod();

        log.info("{} {}", metoda, exchange.getRequestURI());

        try {
            if (!"GET".equalsIgnoreCase(metoda)) {
                Http.greska(exchange, 405, "Podrzana je samo GET metoda");
                return;
            }

            String ostatak = putanja.substring("/api/invoices".length());

            if (ostatak.isEmpty() || "/".equals(ostatak)) {
                listaFaktura(exchange);
            } else {
                String id = ostatak.startsWith("/") ? ostatak.substring(1) : ostatak;
                jednaFaktura(exchange, id);
            }

        } catch (Exception e) {
            log.error("Greska pri obradi zahtjeva {}", putanja, e);
            Http.greska(exchange, 500, "Interna greska: " + e.getMessage());
        }
    }

    private void listaFaktura(HttpExchange exchange) throws IOException {

        Map<String, String> parametri = Http.parametri(exchange);
        String status = parametri.get("status");

        try (Jedis jedis = RedisClient.get()) {

            Set<String> idevi;

            if (status != null && !status.isBlank()) {
                idevi = jedis.smembers(STATUS_PREFIX + status.toLowerCase());
                log.info("Filtriranje po statusu '{}': {} faktura", status, idevi.size());
            } else {
                idevi = jedis.smembers(SVE_FAKTURE);
                log.info("Sve fakture: {}", idevi.size());
            }

            List<String> sortirani = new ArrayList<>(new TreeSet<>(idevi));

            StringBuilder sb = new StringBuilder("[");
            boolean prvi = true;

            for (String id : sortirani) {
                String json = jedis.get(INVOICE_PREFIX + id);
                if (json == null) {
                    continue;
                }
                if (!prvi) {
                    sb.append(",");
                }
                sb.append(json);
                prvi = false;
            }
            sb.append("]");

            Http.posaljiSirovJson(exchange, 200, sb.toString());
        }
    }

    private void jednaFaktura(HttpExchange exchange, String id) throws IOException {

        try (Jedis jedis = RedisClient.get()) {

            String json = jedis.get(INVOICE_PREFIX + id);

            if (json == null) {
                log.warn("Faktura {} nije pronadjena", id);
                Http.greska(exchange, 404, "Faktura " + id + " nije pronadjena");
                return;
            }

            Http.posaljiSirovJson(exchange, 200, json);
        }
    }
}