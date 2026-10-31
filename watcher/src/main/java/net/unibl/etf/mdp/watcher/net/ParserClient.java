package net.unibl.etf.mdp.watcher.net;

import net.unibl.etf.mdp.common.config.Protocol;
import net.unibl.etf.mdp.common.util.HashUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ParserClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ParserClient.class);

    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;

    public ParserClient(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        log.info("Povezan na Parser servis {}:{}", host, port);
    }

    public boolean posalji(String imeFajla, byte[] sadrzaj) throws IOException {

        byte[] hash = HashUtil.sha256(sadrzaj);
        byte[] imeBajtovi = imeFajla.getBytes(StandardCharsets.UTF_8);

        out.writeInt(imeBajtovi.length);
        out.write(imeBajtovi);

        out.writeInt(sadrzaj.length);
        out.write(sadrzaj);

        out.write(hash);
        out.flush();

        log.info("Poslan fajl {} ({} bajtova), hash {}",
                imeFajla, sadrzaj.length, HashUtil.toHex(hash));

        String odgovor = primiOdgovor();
        log.info("Odgovor Parsera: {}", odgovor);

        return Protocol.isAck(odgovor);
    }

    private String primiOdgovor() throws IOException {
        int duzina = in.readInt();

        if (duzina <= 0 || duzina > Protocol.MAX_FILENAME_SIZE * 2) {
            throw new IOException("Neispravna duzina odgovora: " + duzina);
        }

        byte[] bajtovi = new byte[duzina];
        in.readFully(bajtovi);
        return new String(bajtovi, StandardCharsets.UTF_8);
    }

    @Override
    public void close() throws IOException {
        socket.close();
        log.info("Konekcija sa Parserom zatvorena");
    }
}