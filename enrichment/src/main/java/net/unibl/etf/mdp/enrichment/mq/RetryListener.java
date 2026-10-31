package net.unibl.etf.mdp.enrichment.mq;

import net.unibl.etf.mdp.common.config.AppConfig;
import net.unibl.etf.mdp.common.config.Status;
import net.unibl.etf.mdp.common.config.Topics;
import net.unibl.etf.mdp.common.model.Invoice;
import net.unibl.etf.mdp.common.util.JsonUtil;
import net.unibl.etf.mdp.enrichment.service.EnrichmentService;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RetryListener implements MessageListener, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(RetryListener.class);

    private static final String CLIENT_ID = "enrichment-retry";
    private static final String SUBSCRIPTION_NAME = "enrichment-retry-sub";

    private final long odgodaSekundi;
    private final int maxPokusaja;

    private final ScheduledExecutorService scheduler;

    private Connection connection;
    private Session session;
    private TopicSubscriber subscriber;

    public RetryListener() {
        this.odgodaSekundi = AppConfig.getInt("retry.delay.seconds", 30);
        this.maxPokusaja = AppConfig.getInt("retry.max.attempts", 3);

        int niti = AppConfig.getInt("retry.scheduler.threads", 2);
        this.scheduler = Executors.newScheduledThreadPool(niti);
    }

    public void start() throws JMSException {

        String url = AppConfig.get("mq.url", "tcp://localhost:61616");

        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(url);
        factory.setTrustAllPackages(true);

        connection = factory.createConnection();
        connection.setClientID(CLIENT_ID);

        session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

        Topic topic = session.createTopic(Topics.RETRY);
        subscriber = session.createDurableSubscriber(topic, SUBSCRIPTION_NAME);
        subscriber.setMessageListener(this);

        connection.start();

        log.info("Retry listener pretplacen na topic {} (odgoda {} s, max pokusaja {})",
                Topics.RETRY, odgodaSekundi, maxPokusaja);
    }

    @Override
    public void onMessage(Message message) {
        try {
            if (!(message instanceof TextMessage)) {
                message.acknowledge();
                return;
            }

            String json = ((TextMessage) message).getText();
            Invoice invoice = JsonUtil.fromJson(json, Invoice.class);

            int pokusaj = invoice.getRetryCount() == null ? 1 : invoice.getRetryCount();

            log.info("Faktura {} zakazana za ponovni pokusaj {} za {} sekundi",
                    invoice.getId(), pokusaj, odgodaSekundi);

            scheduler.schedule(
                    () -> pokusajPonovo(invoice),
                    odgodaSekundi,
                    TimeUnit.SECONDS);

            message.acknowledge();

        } catch (Exception e) {
            log.error("Greska pri obradi retry poruke", e);
        }
    }

    private void pokusajPonovo(Invoice invoice) {

        int pokusaj = invoice.getRetryCount() == null ? 1 : invoice.getRetryCount();

        log.info("Ponovni pokusaj {} za fakturu {}", pokusaj, invoice.getId());

        boolean uspjeh;
        try {
            uspjeh = EnrichmentService.obogati(invoice);
        } catch (Exception e) {
            log.error("Neocekivana greska pri ponovnom pokusaju za fakturu {}",
                    invoice.getId(), e);
            uspjeh = false;
        }

        if (uspjeh) {
            log.info("Faktura {} uspjesno obogacena iz {}. pokusaja", invoice.getId(), pokusaj);
            return;
        }

        if (pokusaj >= maxPokusaja) {
            log.error("Faktura {} odustaje nakon {} pokusaja, saljem na {}",
                    invoice.getId(), pokusaj, Topics.FAILED);

            invoice.setStatus(Status.FAILED);
            invoice.setRejectionReason(Status.REASON_RATE_UNAVAILABLE);
            InvoicePublisher.posalji(Topics.FAILED, invoice);
            return;
        }

        int sljedeci = pokusaj + 1;
        invoice.setRetryCount(sljedeci);

        log.warn("Faktura {} nije obogacena, vracam na {} sa brojacem {}",
                invoice.getId(), Topics.RETRY, sljedeci);

        InvoicePublisher.posalji(Topics.RETRY, invoice);
    }

    @Override
    public void close() {
        try {
            if (subscriber != null) subscriber.close();
            if (session != null) session.close();
            if (connection != null) connection.close();
        } catch (JMSException e) {
            log.debug("Greska pri zatvaranju retry MQ konekcije", e);
        }

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
        }

        log.info("Retry listener zaustavljen");
    }
}