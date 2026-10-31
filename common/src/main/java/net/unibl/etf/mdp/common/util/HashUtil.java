package net.unibl.etf.mdp.common.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class HashUtil {

    public static final int SHA256_LENGTH = 32;

    private HashUtil() {
    }

    public static byte[] sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 nije dostupan", e);
        }
    }

    public static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static boolean matches(byte[] data, byte[] expectedHash) {
    	//isEqual uvijek prodje kroz cijeli niz u konstantnom vremenu
        return MessageDigest.isEqual(sha256(data), expectedHash);
    }
}