package net.unibl.etf.mdp.enrichment.rest;

import com.fasterxml.jackson.databind.JsonNode;

import net.unibl.etf.mdp.common.config.AppConfig;
import net.unibl.etf.mdp.common.util.JsonUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class ExchangeRateApi {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateApi.class);

    private static final String URL = AppConfig.get("rate.api.url",
            "https://open.er-api.com/v6/latest/BAM");

    private static final int TIMEOUT_SEKUNDI = AppConfig.getInt("rate.api.timeout.seconds", 10);

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(TIMEOUT_SEKUNDI))
            .build();

    private ExchangeRateApi() {
    }

    public static Map<String, BigDecimal> dohvatiKurseve() throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .timeout(Duration.ofSeconds(TIMEOUT_SEKUNDI))
                .header("Accept", "application/json")
                .GET()
                .build();

        log.info("Pozivam REST API: {}", URL);

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IllegalStateException("REST API vratio status " + response.statusCode());
        }

        JsonNode root = JsonUtil.mapper().readTree(response.body());

        String rezultat = root.path("result").asText();
        if (!"success".equals(rezultat)) {
            throw new IllegalStateException("REST API vratio result=" + rezultat);
        }

        JsonNode rates = root.path("rates");
        if (rates.isMissingNode() || !rates.isObject()) {
            throw new IllegalStateException("REST API odgovor nema polje 'rates'");
        }

        Map<String, BigDecimal> kursevi = new HashMap<>();

        Iterator<Map.Entry<String, JsonNode>> polja = rates.fields();
        while (polja.hasNext()) {
            Map.Entry<String, JsonNode> polje = polja.next();
            kursevi.put(polje.getKey(), new BigDecimal(polje.getValue().asText()));
        }

        log.info("Dohvaceno {} kurseva sa REST API-ja", kursevi.size());
        return kursevi;
    }
}