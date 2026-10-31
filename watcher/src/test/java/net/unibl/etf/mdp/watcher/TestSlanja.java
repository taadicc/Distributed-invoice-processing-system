package net.unibl.etf.mdp.watcher;

import net.unibl.etf.mdp.common.config.AppConfig;
import net.unibl.etf.mdp.watcher.net.ParserClient;

import java.nio.file.Files;
import java.nio.file.Path;

public class TestSlanja {

    public static void main(String[] args) throws Exception {

        String host = AppConfig.get("parser.host", "localhost");
        int port = AppConfig.getInt("parser.port", 9000);

        Path fajl = Path.of("data/invoices/inbox/faktura-001.xml");

        System.out.println("Citam fajl: " + fajl.toAbsolutePath());

        byte[] sadrzaj = Files.readAllBytes(fajl);
        System.out.println("Procitano bajtova: " + sadrzaj.length);

        try (ParserClient klijent = new ParserClient(host, port)) {

            boolean ok = klijent.posalji(fajl.getFileName().toString(), sadrzaj);
            System.out.println("\nPrvi fajl, ACK primljen: " + ok);

            boolean ok2 = klijent.posalji("faktura-002.xml", sadrzaj);
            System.out.println("Drugi fajl kroz istu konekciju, ACK: " + ok2);
/*
            byte[] pokvaren = sadrzaj.clone();
            pokvaren[0] = (byte) 'X';
            boolean ok3 = klijent.posalji("pokvarena.xml", pokvaren);
            System.out.println("Pokvarena faktura, ACK: " + ok3);
        */
        }
    }
}