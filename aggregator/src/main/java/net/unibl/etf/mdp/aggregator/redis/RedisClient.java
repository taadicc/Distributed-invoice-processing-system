package net.unibl.etf.mdp.aggregator.redis;

import net.unibl.etf.mdp.common.config.AppConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public final class RedisClient {

    private static final Logger log = LoggerFactory.getLogger(RedisClient.class);

    private static final JedisPool POOL;

    static {
        String host = AppConfig.get("redis.host", "localhost");
        int port = AppConfig.getInt("redis.port", 6379);

        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(20);
        config.setMaxIdle(10);
        config.setMinIdle(2);
        config.setTestOnBorrow(true);

        POOL = new JedisPool(config, host, port);
        log.info("Redis pool inicijalizovan za {}:{}", host, port);
    }

    private RedisClient() {
    }

    public static Jedis get() {
        return POOL.getResource();
    }

    public static boolean provjeri() {
        try (Jedis jedis = get()) {
            String odgovor = jedis.ping();
            log.info("Redis odgovorio: {}", odgovor);
            return "PONG".equals(odgovor);
        } catch (Exception e) {
            log.error("Redis nije dostupan", e);
            return false;
        }
    }
}