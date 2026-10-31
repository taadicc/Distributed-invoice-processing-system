package net.unibl.etf.mdp.parser.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

public final class DuplicateChecker {

    private static final Logger log = LoggerFactory.getLogger(DuplicateChecker.class);

    private static final String PREFIX = "seen:invoice:";
    private static final long TTL_SEKUNDI = 7 * 24 * 60 * 60;

    private DuplicateChecker() {
    }

    public static boolean jeNovaFaktura(String invoiceId) {
        String kljuc = PREFIX + invoiceId;

        try (Jedis jedis = RedisClient.get()) {
            long upisano = jedis.setnx(kljuc, String.valueOf(System.currentTimeMillis()));

            if (upisano == 1) {
                jedis.expire(kljuc, TTL_SEKUNDI);
                log.debug("Faktura {} je nova, kljuc upisan", invoiceId);
                return true;
            }

            log.warn("Faktura {} je DUPLIKAT, kljuc vec postoji u Redisu", invoiceId);
            return false;
        }
    }
}