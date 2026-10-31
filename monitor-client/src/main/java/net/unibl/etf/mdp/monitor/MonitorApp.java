package net.unibl.etf.mdp.monitor;

import net.unibl.etf.mdp.common.config.AppConfig;
import net.unibl.etf.mdp.monitor.udp.MulticastReceiver;
import net.unibl.etf.mdp.monitor.ui.MonitorFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class MonitorApp {

    private static final Logger log = LoggerFactory.getLogger(MonitorApp.class);

    public static void main(String[] args) {

        String ime = AppConfig.get("instance.name", "Monitor-1");
        String grupa = AppConfig.get("multicast.group", "239.0.0.1");
        int port = AppConfig.getInt("multicast.port", 4446);

        log.info("Pokretanje Monitor klijenta '{}'...", ime);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            log.debug("Ne mogu postaviti sistemski izgled", e);
        }

        SwingUtilities.invokeLater(() -> {

            MonitorFrame frame = new MonitorFrame(ime, grupa, port);
            frame.setVisible(true);

            MulticastReceiver receiver = new MulticastReceiver(frame::dodajNotifikaciju);

            try {
                receiver.start();
            } catch (Exception e) {
                log.error("Ne mogu se pridruziti multicast grupi", e);
                JOptionPane.showMessageDialog(frame,
                        "Ne mogu se pridružiti multicast grupi:\n" + e.getMessage(),
                        "Greška",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Gasenje Monitor klijenta...");
                receiver.close();
            }));

            log.info("Monitor klijent '{}' je spreman.", ime);
        });
    }
}