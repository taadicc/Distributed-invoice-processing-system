package net.unibl.etf.mdp.aggregator.mq;

import net.unibl.etf.mdp.aggregator.store.InvoiceStore;
import net.unibl.etf.mdp.aggregator.udp.MulticastNotifier;
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

import java.util.ArrayList;
import java.util.List;

public class AggregatorListener implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(AggregatorListener.class);

    private static final String CLIENT_ID = "aggregator-service";

    private static final String[] TOPICI = {
            Topics.VALIDATED,
            Topics.REJECTED,
            Topics.ENRICHED,
            Topics.APPROVED,
            Topics.PENDING_APPROVAL,
            Topics.FAILED
    };

    private final MulticastNotifier notifier;

    private Connection connection;
    private Session session;
    private final List<TopicSubscriber> pretplate = new ArrayList<>();

    public AggregatorListener(MulticastNotifier notifier) {
        this.notifier = notifier;
    }

    public void start() throws JMSException {

        String url = AppConfig.get("mq.url", "tcp://localhost:61616");

        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(url);
        factory.setTrustAllPackages(true);

        connection = factory.createConnection();
        connection.setClientID(CLIENT_ID);

        session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

        for (String topicNaziv : TOPICI) {
            Topic topic = session.createTopic(topicNaziv);
            String pretplataIme = "aggregator-" + topicNaziv.replace('.', '-');

            TopicSubscriber subscriber = session.createDurableSubscriber(topic, pretplataIme);
            subscriber.setMessageListener(new TopicHandler(topicNaziv));
            pretplate.add(subscriber);

            log.info("Pretplacen na topic {}", topicNaziv);
        }

        connection.start();
        log.info("Aggregator prati {} topica", TOPICI.length);
    }

    private class TopicHandler implements MessageListener {

        private final String topicNaziv;

        TopicHandler(String topicNaziv) {
            this.topicNaziv = topicNaziv;
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

                log.info("[{}] primljena faktura {}", topicNaziv, invoice.getId());

                InvoiceStore.sacuvaj(invoice);

                if (Topics.APPROVED.equals(topicNaziv)) {
                    InvoiceStore.azurirajKlijenta(invoice);
                    notifier.posalji(invoice);
                }

                message.acknowledge();

            } catch (Exception e) {
                log.error("Greska pri obradi poruke sa topica {}", topicNaziv, e);
            }
        }
    }

    @Override
    public void close() {
        try {
            for (TopicSubscriber subscriber : pretplate) {
                subscriber.close();
            }
            if (session != null) session.close();
            if (connection != null) connection.close();
            log.info("MQ konekcije zatvorene");
        } catch (JMSException e) {
            log.debug("Greska pri zatvaranju", e);
        }
    }
}