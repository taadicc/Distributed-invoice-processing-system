package net.unibl.etf.mdp.approvalclient.mq;

import net.unibl.etf.mdp.common.config.AppConfig;
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

import java.util.function.Consumer;

public class PendingConsumer implements MessageListener, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(PendingConsumer.class);

    private static final String CLIENT_ID = "approval-client";
    private static final String SUBSCRIPTION_NAME = "approval-client-pending";

    private final Consumer<Invoice> primalac;

    private Connection connection;
    private Session session;
    private TopicSubscriber subscriber;

    public PendingConsumer(Consumer<Invoice> primalac) {
        this.primalac = primalac;
    }

    public void start() throws JMSException {

        String url = AppConfig.get("mq.url", "tcp://localhost:61616");

        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(url);
        factory.setTrustAllPackages(true);

        connection = factory.createConnection();
        connection.setClientID(CLIENT_ID);

        session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

        Topic topic = session.createTopic(Topics.PENDING_APPROVAL);
        subscriber = session.createDurableSubscriber(topic, SUBSCRIPTION_NAME);
        subscriber.setMessageListener(this);

        connection.start();

        log.info("Approval klijent pretplacen na topic {}", Topics.PENDING_APPROVAL);
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

            log.info("Primljen zahtjev za odobrenje fakture {}", invoice.getId());

            primalac.accept(invoice);

            message.acknowledge();

        } catch (Exception e) {
            log.error("Greska pri obradi zahtjeva za odobrenje", e);
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
            log.debug("Greska pri zatvaranju", e);
        }
    }
}