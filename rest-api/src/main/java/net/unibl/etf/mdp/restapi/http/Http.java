package net.unibl.etf.mdp.restapi.http;

import com.sun.net.httpserver.HttpExchange;

import net.unibl.etf.mdp.common.util.JsonUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Http {

    private Http() {
    }

    public static void posaljiJson(HttpExchange exchange, int status, Object tijelo)
            throws IOException {

        String json = JsonUtil.toJson(tijelo);
        byte[] bajtovi = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

        exchange.sendResponseHeaders(status, bajtovi.length);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bajtovi);
        }
    }

    public static void posaljiSirovJson(HttpExchange exchange, int status, String json)
            throws IOException {

        byte[] bajtovi = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

        exchange.sendResponseHeaders(status, bajtovi.length);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bajtovi);
        }
    }

    public static void greska(HttpExchange exchange, int status, String poruka)
            throws IOException {

        Map<String, Object> tijelo = new LinkedHashMap<>();
        tijelo.put("error", poruka);
        tijelo.put("status", status);

        posaljiJson(exchange, status, tijelo);
    }

    public static Map<String, String> parametri(HttpExchange exchange) {
        Map<String, String> mapa = new HashMap<>();

        String query = exchange.getRequestURI().getQuery();
        if (query == null || query.isBlank()) {
            return mapa;
        }

        for (String par : query.split("&")) {
            int znak = par.indexOf('=');
            if (znak > 0) {
                String kljuc = URLDecoder.decode(par.substring(0, znak), StandardCharsets.UTF_8);
                String vrijednost = URLDecoder.decode(par.substring(znak + 1), StandardCharsets.UTF_8);
                mapa.put(kljuc, vrijednost);
            }
        }
        return mapa;
    }
}