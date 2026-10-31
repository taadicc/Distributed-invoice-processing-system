package net.unibl.etf.mdp.restapi.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import net.unibl.etf.mdp.restapi.redis.RedisClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ClientHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);

    private static final String CLIENT_PREFIX = "client:";

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        String putanja = exchange.getRequestURI().getPath();
        log.info("{} {}", exchange.getRequestMethod(), exchange.getRequestURI());

        try {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                Http.greska(exchange, 405, "Podrzana je samo GET metoda");
                return;
            }

            String ostatak = putanja.substring("/api/clients".length());

            if (!ostatak.endsWith("/summary")) {
                Http.greska(exchange, 400,
                        "Ocekivana putanja: /api/clients/{jib}/summary");
                return;
            }

            String jib = ostatak
                    .substring(0, ostatak.length() - "/summary".length())
                    .replace("/", "");

            if (jib.isBlank()) {
                Http.greska(exchange, 400, "JIB nije naveden");
                return;
            }

            sazetak(exchange, jib);

        } catch (Exception e) {
            log.error("Greska pri obradi zahtjeva {}", putanja, e);
            Http.greska(exchange, 500, "Interna greska: " + e.getMessage());
        }
    }

    private void sazetak(HttpExchange exchange, String jib) throws IOException {

        try (Jedis jedis = RedisClient.get()) {

            Map<String, String> podaci = jedis.hgetAll(CLIENT_PREFIX + jib);

            if (podaci == null || podaci.isEmpty()) {
                log.warn("Klijent {} nije pronadjen", jib);
                Http.greska(exchange, 404, "Klijent " + jib + " nije pronadjen");
                return;
            }

            Map<String, Object> odgovor = new LinkedHashMap<>();
            odgovor.put("jib", jib);
            odgovor.put("clientName", podaci.getOrDefault("clientName", null));
            odgovor.put("totalBAM", podaci.getOrDefault("totalBAM", "0"));
            odgovor.put("count", Integer.parseInt(podaci.getOrDefault("count", "0")));
            odgovor.put("lastInvoiceId", podaci.get("lastInvoiceId"));
            odgovor.put("lastUpdated", podaci.get("lastUpdated"));

            Http.posaljiJson(exchange, 200, odgovor);
        }
    }
}