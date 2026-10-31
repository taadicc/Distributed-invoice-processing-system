package net.unibl.etf.mdp.monitor;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;

public class TestPosiljalac {

    public static void main(String[] args) throws Exception {

        String json = "{\"invoiceId\":\"TEST-001\",\"clientName\":\"Test klijent\","
                + "\"totalBAM\":123.45,\"status\":\"approved\","
                + "\"timestamp\":\"2026-09-11T12:00:00\"}";

        byte[] podaci = json.getBytes(StandardCharsets.UTF_8);

        try (MulticastSocket socket = new MulticastSocket()) {
            NetworkInterface ni = NetworkInterface.getByName("loopback_0");
            if (ni != null) {
                socket.setNetworkInterface(ni);
            }
            socket.setTimeToLive(1);

            DatagramPacket paket = new DatagramPacket(
                    podaci, podaci.length,
                    InetAddress.getByName("239.0.0.1"), 4446);
            socket.send(paket);

            System.out.println("Poslano preko interfejsa: "
                    + (ni == null ? "podrazumijevani" : ni.getName()));
        }
    }
}