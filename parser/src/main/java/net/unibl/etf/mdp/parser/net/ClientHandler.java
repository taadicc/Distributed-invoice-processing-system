package net.unibl.etf.mdp.parser.net;

import net.unibl.etf.mdp.common.config.Protocol;
import net.unibl.etf.mdp.common.util.HashUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import net.unibl.etf.mdp.common.model.Invoice;
import net.unibl.etf.mdp.parser.redis.DuplicateChecker;
import net.unibl.etf.mdp.parser.xml.InvoiceXmlParser;
import net.unibl.etf.mdp.common.config.Status;
import net.unibl.etf.mdp.common.model.ValidationResult;
import net.unibl.etf.mdp.parser.rmi.ValidatorClient;
import java.rmi.RemoteException;
import net.unibl.etf.mdp.common.config.Topics;
import net.unibl.etf.mdp.parser.mq.InvoicePublisher;

public class ClientHandler implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);

    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        String klijent = socket.getRemoteSocketAddress().toString();
        log.info("Nova konekcija sa {}", klijent);

        try (Socket s = socket;
             DataInputStream in = new DataInputStream(s.getInputStream());
             DataOutputStream out = new DataOutputStream(s.getOutputStream())) {

            while (true) {
                boolean nastavi = obradiPoruku(in, out);
                if (!nastavi) {
                    break;
                }
            }

        } catch (java.io.EOFException e) {
            log.info("Klijent {} je zatvorio konekciju", klijent);
        } catch (java.net.SocketException e) {
            log.info("Konekcija sa {} prekinuta: {}", klijent, e.getMessage());
        } catch (IOException e) {
            log.error("Greska u komunikaciji sa {}", klijent, e);
        }
    }

    private boolean obradiPoruku(DataInputStream in, DataOutputStream out) throws IOException {

        int duzinaImena = in.readInt();

        if (duzinaImena <= 0 || duzinaImena > Protocol.MAX_FILENAME_SIZE) {
            log.error("Neispravna duzina imena fajla: {}", duzinaImena);
            return false;
        }

        byte[] imeBajtovi = new byte[duzinaImena];
        in.readFully(imeBajtovi);
        String imeFajla = new String(imeBajtovi, StandardCharsets.UTF_8);

        int duzinaSadrzaja = in.readInt();

        if (duzinaSadrzaja <= 0 || duzinaSadrzaja > Protocol.MAX_PAYLOAD_SIZE) {
            log.error("Neispravna duzina sadrzaja za {}: {}", imeFajla, duzinaSadrzaja);
            posalji(out, Protocol.nak(imeFajla, "INVALID_LENGTH"));
            return false;
        }

        byte[] sadrzaj = new byte[duzinaSadrzaja];
        in.readFully(sadrzaj);

        byte[] primljeniHash = new byte[HashUtil.SHA256_LENGTH];
        in.readFully(primljeniHash);

        log.info("Primljen fajl {} ({} bajtova), hash {}",
                imeFajla, duzinaSadrzaja, HashUtil.toHex(primljeniHash));

        if (!HashUtil.matches(sadrzaj, primljeniHash)) {
            log.error("Integritet narusen za fajl {}. Ocekivano {}, izracunato {}",
                    imeFajla,
                    HashUtil.toHex(primljeniHash),
                    HashUtil.toHex(HashUtil.sha256(sadrzaj)));
            posalji(out, Protocol.nak(imeFajla, "HASH_MISMATCH"));
            return true;
        }

        posalji(out, Protocol.ack(imeFajla));
        log.info("Poslan ACK za fajl {}", imeFajla);

        obradiFakturu(imeFajla, sadrzaj);

        return true;
    }

    private void obradiFakturu(String imeFajla, byte[] sadrzaj) {

        Invoice invoice;
        try {
            invoice = InvoiceXmlParser.parse(sadrzaj);
        } catch (Exception e) {
            log.error("Neuspjelo parsiranje XML-a iz fajla {}: {}", imeFajla, e.getMessage());
            return;
        }

        invoice.setSourceFile(imeFajla);

        if (invoice.getId() == null || invoice.getId().isBlank()) {
            log.error("Faktura iz fajla {} nema <id>, odbacujem", imeFajla);
            return;
        }

        log.info("Parsirana faktura {} iz fajla {}, {} stavki, ukupno {} {}",
                invoice.getId(), imeFajla, invoice.getItems().size(),
                invoice.calculateTotal(), invoice.getCurrency());

        if (!DuplicateChecker.jeNovaFaktura(invoice.getId())) {
            log.warn("Faktura {} je duplikat, odbacena", invoice.getId());
            return;
        }

        log.info("Faktura {} prosla provjeru duplikata", invoice.getId());

        ValidationResult rezultat;
        try {
            rezultat = ValidatorClient.validate(invoice);
        } catch (RemoteException e) {
            log.error("Validator servis nije dostupan za fakturu {}", invoice.getId(), e);
            return;
        }

        if (rezultat.isValid()) {
            log.info("Faktura {} je VALIDNA (validirano u {})",
                    invoice.getId(), rezultat.getValidatorTimestamp());
            invoice.setStatus(Status.VALIDATED);
            InvoicePublisher.posalji(Topics.VALIDATED, invoice);
        } else {
            log.warn("Faktura {} NIJE VALIDNA, gresaka {}: {}",
                    invoice.getId(), rezultat.getErrors().size(), rezultat.getErrors());
            invoice.setStatus(Status.REJECTED);
            invoice.setErrors(rezultat.getErrors());
            InvoicePublisher.posalji(Topics.REJECTED, invoice);
        }
    }

    private void posalji(DataOutputStream out, String poruka) throws IOException {
        byte[] bajtovi = poruka.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bajtovi.length);
        out.write(bajtovi);
        out.flush();
    }
}