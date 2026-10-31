package net.unibl.etf.mdp.monitor.udp;

import net.unibl.etf.mdp.common.config.AppConfig;
import net.unibl.etf.mdp.common.util.JsonUtil;
import net.unibl.etf.mdp.monitor.model.Notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class MulticastReceiver implements Runnable, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(MulticastReceiver.class);

    private static final int BUFFER_SIZE = 8192;

    private final Consumer<Notification> primalac;
    private final String adresa;
    private final int port;

    private MulticastSocket socket;
    private volatile boolean radi = true;

    public MulticastReceiver(Consumer<Notification> primalac) {
        this.primalac = primalac;
        this.adresa = AppConfig.get("multicast.group", "239.0.0.1");
        this.port = AppConfig.getInt("multicast.port", 4446);
    }

    public void start() throws Exception {

        socket = new MulticastSocket(null);
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(port));
        //socket.setLoopbackMode(false);
        socket.setOption(java.net.StandardSocketOptions.IP_MULTICAST_LOOP, true);

        InetAddress grupa = InetAddress.getByName(adresa);
        InetSocketAddress grupaAdresa = new InetSocketAddress(grupa, port);

        NetworkInterface interfejs = pronadjiInterfejs();
        socket.joinGroup(grupaAdresa, interfejs);

        log.info("Pridruzen multicast grupi {}:{} preko interfejsa {}",
                adresa, port, interfejs == null ? "podrazumijevani" : interfejs.getName());

        Thread nit = new Thread(this, "multicast-receiver");
        nit.setDaemon(true);
        nit.start();
    }

    @Override
    public void run() {
        byte[] buffer = new byte[BUFFER_SIZE];

        while (radi) {
            try {
                DatagramPacket paket = new DatagramPacket(buffer, buffer.length);
                socket.receive(paket);

                String json = new String(paket.getData(), paket.getOffset(),
                        paket.getLength(), StandardCharsets.UTF_8);

                log.info("Primljena notifikacija: {}", json);

                Notification notifikacija = JsonUtil.fromJson(json, Notification.class);
                primalac.accept(notifikacija);

            } catch (Exception e) {
                if (radi) {
                    log.error("Greska pri prijemu UDP paketa", e);
                }
            }
        }

        log.info("Prijem multicast poruka zaustavljen");
    }

    private NetworkInterface pronadjiInterfejs() throws Exception {
        String ime = AppConfig.get("multicast.interface", null);

        if (ime != null && !ime.isBlank()) {
            NetworkInterface ni = NetworkInterface.getByName(ime);
            if (ni != null) {
                return ni;
            }
            log.warn("Mrezni interfejs {} nije pronadjen, koristim automatski izbor", ime);
        }

        java.util.Enumeration<NetworkInterface> sve = NetworkInterface.getNetworkInterfaces();
        while (sve.hasMoreElements()) {
            NetworkInterface ni = sve.nextElement();

            if (!ni.isUp() || !ni.supportsMulticast() || ni.isLoopback()) {
                continue;
            }
            if (!imaIPv4(ni)) {
                continue;
            }

            log.info("Izabran mrezni interfejs: {} ({})", ni.getName(), ni.getDisplayName());
            return ni;
        }

        java.util.Enumeration<NetworkInterface> opet = NetworkInterface.getNetworkInterfaces();
        while (opet.hasMoreElements()) {
            NetworkInterface ni = opet.nextElement();
            if (ni.isUp() && ni.supportsMulticast() && ni.isLoopback() && imaIPv4(ni)) {
                log.warn("Koristim loopback interfejs {} — multicast radi samo lokalno",
                        ni.getName());
                return ni;
            }
        }

        throw new IllegalStateException("Nije pronadjen mrezni interfejs sa IPv4 podrskom");
    }

    private boolean imaIPv4(NetworkInterface ni) {
        java.util.Enumeration<java.net.InetAddress> adrese = ni.getInetAddresses();
        while (adrese.hasMoreElements()) {
            if (adrese.nextElement() instanceof java.net.Inet4Address) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void close() {
        radi = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}