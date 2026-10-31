package net.unibl.etf.mdp.validator;

import net.unibl.etf.mdp.common.model.Client;
import net.unibl.etf.mdp.common.model.Invoice;
import net.unibl.etf.mdp.common.model.InvoiceItem;
import net.unibl.etf.mdp.common.model.ValidationResult;
import net.unibl.etf.mdp.common.rmi.ValidatorService;

import java.math.BigDecimal;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalDate;

public class TestKlijent {

    public static void main(String[] args) throws Exception {

        Registry registry = LocateRegistry.getRegistry("localhost", 1099);
        ValidatorService validator =
                (ValidatorService) registry.lookup(ValidatorService.SERVICE_NAME);

        System.out.println("Povezan na Validator servis.\n");

        Invoice dobra = new Invoice();
        dobra.setId("INV-2025-001");
        dobra.setClient(new Client("ETF", "1234567891012", "fakture@etf.com"));
        dobra.setDate(LocalDate.now().minusDays(10));
        dobra.setCurrency("EUR");
        dobra.getItems().add(new InvoiceItem("Web development", 10, new BigDecimal("50.00")));
        dobra.getItems().add(new InvoiceItem("Hosting", 1, new BigDecimal("120.00")));

        ValidationResult r1 = validator.validate(dobra);
        System.out.println("Ispravna faktura -> " + r1.isValid());
        System.out.println("Greske: " + r1.getErrors());
        System.out.println("Vrijeme: " + r1.getValidatorTimestamp() + "\n");

        Invoice losa = new Invoice();
        losa.setId("INV-2025-999");
        losa.setClient(new Client("Firma", "123", "nijeemail"));
        losa.setDate(LocalDate.now().plusDays(5));
        losa.setCurrency("RSD");
        losa.getItems().add(new InvoiceItem("Nesto", -3, new BigDecimal("-10.00")));

        ValidationResult r2 = validator.validate(losa);
        System.out.println("Neispravna faktura -> " + r2.isValid());
        System.out.println("Greske:");
        for (String greska : r2.getErrors()) {
            System.out.println("  - " + greska);
        }
    }
}