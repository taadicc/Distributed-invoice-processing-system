package net.unibl.etf.mdp.watcher;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GeneratorFaktura {

    private static final Path IZLAZ = Path.of("data/invoices/generated");

    private static final int BROJ_FAKTURA = 120;
    private static final int POSTO_NEISPRAVNIH = 15;

    private static final String[] KLIJENTI = {
            "ETF Banja Luka", "Telekom Srpske", "Nova Banka", "M-tel",
            "Bingo doo", "Lanaco", "Prointer ITSS", "Sberbank",
            "Elektrokrajina", "Vodovod ad", "Autoprevoz", "Bosnalijek",
            "Comtrade", "Mikrografija", "IT Studio", "Digital Cube"
    };

    private static final String[] VALUTE = { "BAM", "EUR", "USD", "CHF", "GBP" };

    private static final String[] OPISI = {
            "Web development", "Hosting", "Konsultacije", "Licenca za softver",
            "Odrzavanje sistema", "Obuka zaposlenih", "Dizajn korisnickog interfejsa",
            "Migracija baze podataka", "Sigurnosna revizija", "Tehnicka podrska",
            "Instalacija opreme", "Izrada dokumentacije", "Testiranje aplikacije",
            "Analiza zahtjeva", "Optimizacija performansi"
    };

    private static final Random RND = new Random(42);

    public static void main(String[] args) throws IOException {

        Files.createDirectories(IZLAZ);

        int ocisceno = ocistiFolder();
        if (ocisceno > 0) {
            System.out.println("Obrisano " + ocisceno + " starih fajlova.");
        }

        int neispravnih = 0;
        int malih = 0;

        for (int i = 1; i <= BROJ_FAKTURA; i++) {

            boolean neispravna = RND.nextInt(100) < POSTO_NEISPRAVNIH;
            boolean mala = !neispravna && RND.nextInt(100) < 25;

            String id = String.format("INV-2026-%04d", i);
            String xml = napraviXml(id, neispravna, mala);

            Path fajl = IZLAZ.resolve(String.format("faktura-%04d.xml", i));
            Files.writeString(fajl, xml, StandardCharsets.UTF_8);

            if (neispravna) neispravnih++;
            if (mala) malih++;
        }

        System.out.println();
        System.out.println("Generisano " + BROJ_FAKTURA + " faktura u " + IZLAZ.toAbsolutePath());
        System.out.println("  ispravnih velikih (rucno odobrenje): "
                + (BROJ_FAKTURA - neispravnih - malih));
        System.out.println("  ispravnih malih (automatsko odobrenje): " + malih);
        System.out.println("  neispravnih (idu na rejected): " + neispravnih);
    }

    private static int ocistiFolder() throws IOException {
        int brojac = 0;
        try (var tok = Files.list(IZLAZ)) {
            for (Path p : tok.toList()) {
                if (Files.isRegularFile(p) && p.toString().endsWith(".xml")) {
                    Files.delete(p);
                    brojac++;
                }
            }
        }
        return brojac;
    }

    private static String napraviXml(String id, boolean neispravna, boolean mala) {

        String klijent = KLIJENTI[RND.nextInt(KLIJENTI.length)];
        String jib = generisiJib(neispravna);
        String email = generisiEmail(klijent, neispravna);
        String datum = generisiDatum(neispravna);
        String valuta = generisiValutu(neispravna);

        List<String> stavke = generisiStavke(neispravna, mala);

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<invoice>\n");
        sb.append("    <id>").append(id).append("</id>\n");
        sb.append("    <client>\n");
        sb.append("        <name>").append(klijent).append("</name>\n");
        sb.append("        <jib>").append(jib).append("</jib>\n");
        sb.append("        <email>").append(email).append("</email>\n");
        sb.append("    </client>\n");
        sb.append("    <date>").append(datum).append("</date>\n");
        sb.append("    <currency>").append(valuta).append("</currency>\n");
        sb.append("    <items>\n");

        for (String stavka : stavke) {
            sb.append(stavka);
        }

        sb.append("    </items>\n");
        sb.append("</invoice>\n");

        return sb.toString();
    }

    private static String generisiJib(boolean neispravna) {
        if (neispravna && RND.nextInt(100) < 40) {
            return String.valueOf(100 + RND.nextInt(900));
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 13; i++) {
            sb.append(RND.nextInt(10));
        }
        return sb.toString();
    }

    private static String generisiEmail(String klijent, boolean neispravna) {
        if (neispravna && RND.nextInt(100) < 40) {
            return "neispravan-email";
        }
        String domen = klijent.toLowerCase()
                .replace(" ", "")
                .replaceAll("[^a-z]", "");
        return "fakture@" + domen + ".ba";
    }

    private static String generisiDatum(boolean neispravna) {
        LocalDate danas = LocalDate.now();

        if (neispravna && RND.nextInt(100) < 35) {
            return RND.nextBoolean()
                    ? danas.plusDays(10 + RND.nextInt(60)).toString()
                    : danas.minusDays(400 + RND.nextInt(300)).toString();
        }
        return danas.minusDays(RND.nextInt(300)).toString();
    }

    private static String generisiValutu(boolean neispravna) {
        if (neispravna && RND.nextInt(100) < 35) {
            return RND.nextBoolean() ? "RSD" : "HRK";
        }
        return VALUTE[RND.nextInt(VALUTE.length)];
    }

    private static List<String> generisiStavke(boolean neispravna, boolean mala) {

        List<String> stavke = new ArrayList<>();
        int broj = 1 + RND.nextInt(4);

        for (int i = 0; i < broj; i++) {

            String opis = OPISI[RND.nextInt(OPISI.length)];
            int kolicina;
            BigDecimal cijena;

            if (neispravna && i == 0 && RND.nextInt(100) < 30) {
                kolicina = -(1 + RND.nextInt(5));
                cijena = BigDecimal.valueOf(-(10 + RND.nextInt(100)));
            } else if (mala) {
                kolicina = 1;
                cijena = BigDecimal.valueOf(5 + RND.nextInt(30))
                        .setScale(2, RoundingMode.HALF_UP);
            } else {
                kolicina = 1 + RND.nextInt(20);
                cijena = BigDecimal.valueOf(20 + RND.nextInt(500))
                        .add(BigDecimal.valueOf(RND.nextInt(100), 2))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            stavke.add("        <item>\n"
                    + "            <description>" + opis + "</description>\n"
                    + "            <quantity>" + kolicina + "</quantity>\n"
                    + "            <unitPrice>" + cijena + "</unitPrice>\n"
                    + "        </item>\n");

            if (mala) {
                break;
            }
        }

        return stavke;
    }
}