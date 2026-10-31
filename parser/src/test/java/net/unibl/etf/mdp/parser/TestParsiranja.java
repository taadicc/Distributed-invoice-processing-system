package net.unibl.etf.mdp.parser;

import net.unibl.etf.mdp.common.model.Invoice;
import net.unibl.etf.mdp.common.util.JsonUtil;
import net.unibl.etf.mdp.parser.xml.InvoiceXmlParser;

import java.nio.file.Files;
import java.nio.file.Path;

public class TestParsiranja {

    public static void main(String[] args) throws Exception {

        Path fajl = Path.of("data/invoices/processed/faktura-001.xml");
        byte[] bajtovi = Files.readAllBytes(fajl);

        Invoice inv = InvoiceXmlParser.parse(bajtovi);

        System.out.println("ID: " + inv.getId());
        System.out.println("Klijent: " + inv.getClient());
        System.out.println("Email: " + inv.getClient().getEmail());
        System.out.println("Datum: " + inv.getDate());
        System.out.println("Valuta: " + inv.getCurrency());
        System.out.println("Stavki: " + inv.getItems().size());
        System.out.println("Ukupno: " + inv.calculateTotal());
        System.out.println("\nJSON:\n" + JsonUtil.toJson(inv));
    }
}