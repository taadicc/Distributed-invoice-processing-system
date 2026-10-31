package net.unibl.etf.mdp.approval;

import net.unibl.etf.mdp.approval.mq.EnrichedListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApprovalApp {

    private static final Logger log = LoggerFactory.getLogger(ApprovalApp.class);

    public static void main(String[] args) {

        log.info("Pokretanje Approval servisa...");

        EnrichedListener listener = new EnrichedListener();

        try {
            listener.start();
        } catch (Exception e) {
            log.error("Ne mogu se pretplatiti na MQ topic", e);
            System.exit(1);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Gasenje Approval servisa...");
            listener.close();
        }));

        log.info("Approval servis je spreman i ceka fakture.");
    }
}