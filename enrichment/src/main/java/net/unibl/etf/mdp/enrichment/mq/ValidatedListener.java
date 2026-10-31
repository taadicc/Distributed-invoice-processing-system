package net.unibl.etf.mdp.enrichment.mq;

import net.unibl.etf.mdp.common.config.AppConfig;
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

public class ValidatedListener implements MessageListener, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ValidatedListener.class);

    private static final String CLIENT_ID = "enrichment-service";
    private static final String SUBSCRIPTION_NAME = "enrichment-validated";

    private Connection connection;
    private Session session;
    private TopicSubscriber subscriber;

    public void start() throws JMSException {

        String url = AppConfig.get("mq.url", "tcp://localhost:61616");

        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(url);
        factory.setTrustAllPackages(true);

        connection = factory.createConnection();
        connection.setClientID(CLIENT_ID);

        session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

        Topic topic = session.createTopic(Topics.VALIDATED);
        subscriber = session.createDurableSubscriber(topic, SUBSCRIPTION_NAME);
        subscriber.setMessageListener(this);

        connection.start();

        log.info("Pretplacen na topic {} (clientID={}, pretplata={})",
                Topics.VALIDATED, CLIENT_ID, SUBSCRIPTION_NAME);
    }

    @Override
    public void onMessage(Message message) {
        try {
            if (!(message instanceof TextMessage)) {
                log.warn("Primljena poruka koja nije TextMessage, preskacem");
                message.acknowledge();
                return;
            }

            String json = ((TextMessage) message).getText();
            Invoice invoice = JsonUtil.fromJson(json, Invoice.class);

            log.info("Primljena faktura {} ({} {})",
                    invoice.getId(), invoice.calculateTotal(), invoice.getCurrency());

            boolean uspjeh = EnrichmentService.obogati(invoice);

            if (!uspjeh) {
                log.warn("Faktura {} nije obogacena, saljem na retry", invoice.getId());
                invoice.setRetryCount(1);
                InvoicePublisher.posalji(Topics.RETRY, invoice);
            }

            message.acknowledge();

        } catch (Exception e) {
            log.error("Greska pri obradi poruke", e);
        }
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