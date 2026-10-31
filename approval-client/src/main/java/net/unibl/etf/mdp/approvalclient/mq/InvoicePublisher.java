package net.unibl.etf.mdp.approvalclient.mq;

import net.unibl.etf.mdp.common.config.AppConfig;
import net.unibl.etf.mdp.common.model.Invoice;
import net.unibl.etf.mdp.common.util.JsonUtil;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

public final class InvoicePublisher {

    private static final Logger log = LoggerFactory.getLogger(InvoicePublisher.class);

    private static final ActiveMQConnectionFactory FACTORY;

    static {
        String url = AppConfig.get("mq.url", "tcp://localhost:61616");
        FACTORY = new ActiveMQConnectionFactory(url);
        FACTORY.setTrustAllPackages(true);
    }

    private InvoicePublisher() {
    }

    public static void posalji(String topicNaziv, Invoice invoice) {

        String json = JsonUtil.toJson(invoice);

        try (Connection connection = FACTORY.createConnection()) {
            connection.start();

            try (Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {

                Topic topic = session.createTopic(topicNaziv);

                try (MessageProducer producer = session.createProducer(topic)) {
                    producer.setDeliveryMode(DeliveryMode.PERSISTENT);

                    TextMessage message = session.createTextMessage(json);
                    message.setStringProperty("invoiceId", invoice.getId());
                    message.setStringProperty("status", invoice.getStatus());

                    producer.send(message);

                    log.info("Faktura {} poslana na topic {}", invoice.getId(), topicNaziv);
                }
            }
        } catch (JMSException e) {
            log.error("Greska pri slanju fakture {} na topic {}",
                    invoice.getId(), topicNaziv, e);
        }
    }
}