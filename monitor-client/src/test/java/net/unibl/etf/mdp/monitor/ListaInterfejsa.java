package net.unibl.etf.mdp.monitor;

import java.net.NetworkInterface;
import java.util.Enumeration;

public class ListaInterfejsa {
    public static void main(String[] args) throws Exception {
        Enumeration<NetworkInterface> sve = NetworkInterface.getNetworkInterfaces();
        while (sve.hasMoreElements()) {
            NetworkInterface ni = sve.nextElement();
            System.out.println(ni.getName() + "  |  " + ni.getDisplayName()
                    + "  |  up=" + ni.isUp()
                    + " multicast=" + ni.supportsMulticast()
                    + " loopback=" + ni.isLoopback());
            ni.getInetAddresses().asIterator()
                    .forEachRemaining(a -> System.out.println("      " + a));
        }
    }
}