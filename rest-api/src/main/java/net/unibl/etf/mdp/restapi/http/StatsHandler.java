package net.unibl.etf.mdp.restapi.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import net.unibl.etf.mdp.common.model.Invoice;
import net.unibl.etf.mdp.common.util.JsonUtil;
import net.unibl.etf.mdp.restapi.redis.RedisClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class StatsHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(StatsHandler.class);

    private static final String INVOICE_PREFIX = "invoice:";
    private static final String STATUS_PREFIX = "invoices:status:";
    private static final String SVE_FAKTURE = "invoices:all";

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        log.info("{} {}", exchange.getRequestMethod(), exchange.getRequestURI());

        try {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                Http.greska(exchange, 405, "Podrzana je samo GET metoda");
                return;
            }

            statistika(exchange);

        } catch (Exception e) {
            log.error("Greska pri obradi statistike", e);
            Http.greska(exchange, 500, "Interna greska: " + e.getMessage());
        }
    }

    private void statistika(HttpExchange exchange) throws IOException {

        try (Jedis jedis = RedisClient.get()) {

            Set<String> idevi = jedis.smembers(SVE_FAKTURE);

            int ukupnoFaktura = idevi.size();
            BigDecimal ukupnoBAM = BigDecimal.ZERO;
            int saIznosom = 0;

            for (String id : idevi) {
                String json = jedis.get(INVOICE_PREFIX + id);
                if (json == null) {
                    continue;
                }

                Invoice invoice = JsonUtil.fromJson(json, Invoice.class);

                if (invoice.getTotalBAM() != null) {
                    ukupnoBAM = ukupnoBAM.add(invoice.getTotalBAM());
                    saIznosom++;
                }
            }

            BigDecimal prosjek = saIznosom == 0
                    ? BigDecimal.ZERO
                    : ukupnoBAM.divide(BigDecimal.valueOf(saIznosom), 2, RoundingMode.HALF_UP);

            long brojPending = jedis.scard(STATUS_PREFIX + "pending");
            long brojApproved = jedis.scard(STATUS_PREFIX + "approved");
            long brojRejected = jedis.scard(STATUS_PREFIX + "rejected");
            long brojFailed = jedis.scard(STATUS_PREFIX + "failed");

            Map<String, Object> odgovor = new LinkedHashMap<>();
            odgovor.put("totalInvoices", ukupnoFaktura);
            odgovor.put("totalBAM", ukupnoBAM.setScale(2, RoundingMode.HALF_UP));
            odgovor.put("averageBAM", prosjek);
            odgovor.put("pendingCount", brojPending);
            odgovor.put("approvedCount", brojApproved);
            odgovor.put("rejectedCount", brojRejected);
            odgovor.put("failedCount", brojFailed);

            log.info("Statistika: {} faktura, ukupno {} BAM", ukupnoFaktura, ukupnoBAM);

            Http.posaljiJson(exchange, 200, odgovor);
        }
    }
}