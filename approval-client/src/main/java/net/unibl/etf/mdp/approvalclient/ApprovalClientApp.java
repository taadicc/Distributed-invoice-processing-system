package net.unibl.etf.mdp.approvalclient;

import net.unibl.etf.mdp.approvalclient.mq.PendingConsumer;
import net.unibl.etf.mdp.approvalclient.ui.ApprovalFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class ApprovalClientApp {

    private static final Logger log = LoggerFactory.getLogger(ApprovalClientApp.class);

    public static void main(String[] args) {

        log.info("Pokretanje Approval klijenta...");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            log.debug("Ne mogu postaviti sistemski izgled", e);
        }

        SwingUtilities.invokeLater(() -> {

            ApprovalFrame frame = new ApprovalFrame();
            frame.setVisible(true);

            PendingConsumer consumer = new PendingConsumer(frame::dodajFakturu);

            try {
                consumer.start();
            } catch (Exception e) {
                log.error("Ne mogu se pretplatiti na MQ topic", e);
                JOptionPane.showMessageDialog(frame,
                        "Ne mogu se povezati na ActiveMQ:\n" + e.getMessage(),
                        "Greska",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Gasenje Approval klijenta...");
                consumer.close();
            }));

            log.info("Approval klijent je spreman.");
        });
    }
}