package net.unibl.etf.mdp.enrichment.cache;

import net.unibl.etf.mdp.enrichment.redis.RedisClient;
import net.unibl.etf.mdp.enrichment.rest.ExchangeRateApi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public final class RateCache {

    private static final Logger log = LoggerFactory.getLogger(RateCache.class);

    private static final String PREFIX = "rate:";
    private static final long TTL_SEKUNDI = 60 * 60;

    public static final String IZVOR_CACHE = "CACHE";
    public static final String IZVOR_API = "API";

    private RateCache() {
    }

    public static Rezultat dohvatiKurs(String valuta) throws Exception {

        String kljuc = kljucZa(valuta);

        try (Jedis jedis = RedisClient.get()) {
            String keširano = jedis.get(kljuc);

            if (keširano != null) {
                log.info("Kurs za {} pronadjen u cache-u: {}", valuta, keširano);
                return new Rezultat(new BigDecimal(keširano), IZVOR_CACHE);
            }
        }

        log.info("Kurs za {} nije u cache-u, pozivam REST API", valuta);

        Map<String, BigDecimal> kursevi = ExchangeRateApi.dohvatiKurseve();

        upisiUCache(kursevi);

        BigDecimal kurs = kursevi.get(valuta);
        if (kurs == null) {
            throw new IllegalStateException("REST API ne poznaje valutu " + valuta);
        }

        return new Rezultat(kurs, IZVOR_API);
    }

    private static void upisiUCache(Map<String, BigDecimal> kursevi) {
        try (Jedis jedis = RedisClient.get()) {
            for (Map.Entry<String, BigDecimal> unos : kursevi.entrySet()) {
                String kljuc = kljucZa(unos.getKey());
                jedis.setex(kljuc, TTL_SEKUNDI, unos.getValue().toPlainString());
            }
            log.info("Upisano {} kurseva u cache (TTL {} s)", kursevi.size(), TTL_SEKUNDI);
        }
    }

    private static String kljucZa(String valuta) {
        return PREFIX + valuta + ":" + LocalDate.now();
    }

    public static class Rezultat {
        private final BigDecimal kurs;
        private final String izvor;

        public Rezultat(BigDecimal kurs, String izvor) {
            this.kurs = kurs;
            this.izvor = izvor;
        }

        public BigDecimal getKurs() {
            return kurs;
        }

        public String getIzvor() {
            return izvor;
        }
    }
}