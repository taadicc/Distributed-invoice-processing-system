package net.unibl.etf.mdp.aggregator.udp;

import net.unibl.etf.mdp.common.config.AppConfig;
import net.unibl.etf.mdp.common.model.Invoice;
import net.unibl.etf.mdp.common.util.JsonUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.net.MulticastSocket;
import java.net.NetworkInterface;

public class MulticastNotifier implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(MulticastNotifier.class);

    private final InetAddress grupa;
    private final int port;
    private final MulticastSocket socket;

    public MulticastNotifier() throws Exception {
        String adresa = AppConfig.get("multicast.group", "239.0.0.1");
        this.port = AppConfig.getInt("multicast.port", 4446);
        this.grupa = InetAddress.getByName(adresa);

        MulticastSocket ms = new MulticastSocket();

        String imeInterfejsa = AppConfig.get("multicast.interface", null);
        if (imeInterfejsa != null && !imeInterfejsa.isBlank()) {
            NetworkInterface ni = NetworkInterface.getByName(imeInterfejsa);
            if (ni != null) {
                ms.setNetworkInterface(ni);
                log.info("Multicast salje kroz interfejs {} ({})",
                        ni.getName(), ni.getDisplayName());
            } else {
                log.warn("Interfejs {} nije pronadjen, koristim podrazumijevani", imeInterfejsa);
            }
        }

        ms.setTimeToLive(1);
        this.socket = ms;

        log.info("Multicast notifier spreman za grupu {}:{}", adresa, port);
    }

    public void posalji(Invoice invoice) {
        try {
            Map<String, Object> notifikacija = new LinkedHashMap<>();
            notifikacija.put("invoiceId", invoice.getId());
            notifikacija.put("clientName",
                    invoice.getClient() != null ? invoice.getClient().getName() : "—");
            notifikacija.put("totalBAM", invoice.getTotalBAM());
            notifikacija.put("status", invoice.getStatus());
            notifikacija.put("timestamp", LocalDateTime.now().toString());

            String json = JsonUtil.toJson(notifikacija);
            byte[] podaci = json.getBytes(StandardCharsets.UTF_8);

            DatagramPacket paket = new DatagramPacket(podaci, podaci.length, grupa, port);
            socket.send(paket);

            log.info("UDP multicast poslan za fakturu {}: {}", invoice.getId(), json);

        } catch (Exception e) {
            log.error("Greska pri slanju UDP notifikacije za fakturu {}",
                    invoice.getId(), e);
        }
    }

    @Override
    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
            log.info("Multicast socket zatvoren");
        }
    }
}