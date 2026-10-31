package net.unibl.etf.mdp.common.config;

public final class Protocol {

    public static final String ACK_PREFIX = "ACK";
    public static final String NAK_PREFIX = "NAK";
    public static final String SEPARATOR = ":";

    public static final int MAX_PAYLOAD_SIZE = 10 * 1024 * 1024;
    public static final int MAX_FILENAME_SIZE = 512;

    private Protocol() {
    }

    public static String ack(String fileName) {
        return ACK_PREFIX + SEPARATOR + fileName;
    }

    public static String nak(String fileName, String reason) {
        return NAK_PREFIX + SEPARATOR + fileName + SEPARATOR + reason;
    }

    public static boolean isAck(String response) {
        return response != null && response.startsWith(ACK_PREFIX);
    }
}