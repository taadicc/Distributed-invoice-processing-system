package net.unibl.etf.mdp.approval.mq;

import net.unibl.etf.mdp.common.config.AppConfig;
import net.unibl.etf.mdp.common.config.Status;
import net.unibl.etf.mdp.common.config.Topics;
import net.unibl.etf.mdp.common.model.Invoice;
import net.unibl.etf.mdp.common.util.JsonUtil;

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

import java.math.BigDecimal;

public class EnrichedListener implements MessageListener, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(EnrichedListener.class);

    private static final String CLIENT_ID = "approval-service";
    private static final String SUBSCRIPTION_NAME = "approval-enriched";

    private final BigDecimal prag;

    private Connection connection;
    private Session session;
    private TopicSubscriber subscriber;

    public EnrichedListener() {
        String pragText = AppConfig.get("approval.threshold.bam", "100");
        this.prag = new BigDecimal(pragText);
    }

    public void start() throws JMSException {

        String url = AppConfig.get("mq.url", "tcp://localhost:61616");

        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(url);
        factory.setTrustAllPackages(true);

        connection = factory.createConnection();
        connection.setClientID(CLIENT_ID);

        session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

        Topic topic = session.createTopic(Topics.ENRICHED);
        subscriber = session.createDurableSubscriber(topic, SUBSCRIPTION_NAME);
        subscriber.setMessageListener(this);

        connection.start();

        log.info("Pretplacen na topic {} (prag za automatsko odobrenje: {} BAM)",
                Topics.ENRICHED, prag);
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

            BigDecimal totalBAM = invoice.getTotalBAM();

            if (totalBAM == null) {
                log.error("Faktura {} nema totalBAM, saljem na rucno odobrenje",
                        invoice.getId());
                posaljiNaCekanje(invoice);
                message.acknowledge();
                return;
            }

            log.info("Primljena faktura {} sa iznosom {} BAM", invoice.getId(), totalBAM);

            if (totalBAM.compareTo(prag) <= 0) {
                log.info("Faktura {} ({} BAM <= {} BAM) — AUTOMATSKI ODOBRENA",
                        invoice.getId(), totalBAM, prag);

                invoice.setStatus(Status.APPROVED);
                InvoicePublisher.posalji(Topics.APPROVED, invoice);

            } else {
                log.info("Faktura {} ({} BAM > {} BAM) — ceka rucno odobrenje",
                        invoice.getId(), totalBAM, prag);

                posaljiNaCekanje(invoice);
            }

            message.acknowledge();

        } catch (Exception e) {
            log.error("Greska pri obradi poruke", e);
        }
    }

    private void posaljiNaCekanje(Invoice invoice) {
        invoice.setStatus(Status.PENDING);
        InvoicePublisher.posalji(Topics.PENDING_APPROVAL, invoice);
    }

    @Override
    public void close() {
        try {
            if (subscriber != null) subscriber.close();
            if (session != null) session.close();
            if (connection != null) connection.close();
            log.info("MQ konekcija zatvorena");
        } catch (JMSException e) {
            log.debug("Greska pri zatvaranju MQ konekcije", e);
        }
    }
}