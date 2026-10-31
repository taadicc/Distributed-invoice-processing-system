package net.unibl.etf.mdp.aggregator.store;

import net.unibl.etf.mdp.aggregator.redis.RedisClient;
import net.unibl.etf.mdp.common.model.Invoice;
import net.unibl.etf.mdp.common.util.JsonUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class InvoiceStore {

    private static final Logger log = LoggerFactory.getLogger(InvoiceStore.class);

    private static final String INVOICE_PREFIX = "invoice:";
    private static final String CLIENT_PREFIX = "client:";
    private static final String STATUS_PREFIX = "invoices:status:";
    private static final String SVE_FAKTURE = "invoices:all";

    private InvoiceStore() {
    }

    public static void sacuvaj(Invoice invoice) {

        String id = invoice.getId();
        String json = JsonUtil.toJson(invoice);

        try (Jedis jedis = RedisClient.get()) {

            jedis.set(INVOICE_PREFIX + id, json);
            jedis.sadd(SVE_FAKTURE, id);

            if (invoice.getStatus() != null) {
                obrisiIzStarihStatusa(jedis, id);
                jedis.sadd(STATUS_PREFIX + invoice.getStatus(), id);
            }

            log.info("Faktura {} sacuvana u Redis (status: {})", id, invoice.getStatus());
        }
    }

    private static void obrisiIzStarihStatusa(Jedis jedis, String id) {
        for (String status : new String[] {
                "validated", "rejected", "enriched", "approved", "pending", "failed" }) {
            jedis.srem(STATUS_PREFIX + status, id);
        }
    }

    public static void azurirajKlijenta(Invoice invoice) {

        if (invoice.getClient() == null || invoice.getClient().getJib() == null) {
            log.warn("Faktura {} nema JIB klijenta, preskacem azuriranje", invoice.getId());
            return;
        }

        String jib = invoice.getClient().getJib();
        String kljuc = CLIENT_PREFIX + jib;

        BigDecimal iznos = invoice.getTotalBAM() == null
                ? BigDecimal.ZERO
                : invoice.getTotalBAM();

        try (Jedis jedis = RedisClient.get()) {

            String postojeci = jedis.hget(kljuc, "totalBAM");

            BigDecimal noviUkupno = postojeci == null
                    ? iznos
                    : new BigDecimal(postojeci).add(iznos);

            jedis.hset(kljuc, "totalBAM", noviUkupno.toPlainString());
            jedis.hincrBy(kljuc, "count", 1);
            jedis.hset(kljuc, "lastInvoiceId", invoice.getId());
            jedis.hset(kljuc, "lastUpdated", LocalDateTime.now().toString());

            if (invoice.getClient().getName() != null) {
                jedis.hset(kljuc, "clientName", invoice.getClient().getName());
            }

            log.info("Klijent {} azuriran: ukupno {} BAM, faktura {}",
                    jib, noviUkupno, invoice.getId());
        }
    }
}