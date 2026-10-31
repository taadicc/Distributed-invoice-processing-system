package net.unibl.etf.mdp.validator;

import net.unibl.etf.mdp.common.model.Invoice;
import net.unibl.etf.mdp.common.model.InvoiceItem;
import net.unibl.etf.mdp.common.model.ValidationResult;
import net.unibl.etf.mdp.common.rmi.ValidatorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class ValidatorServiceImpl extends UnicastRemoteObject implements ValidatorService {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ValidatorServiceImpl.class);

    private static final Pattern JIB_PATTERN = Pattern.compile("\\d{13}");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Set<String> VALID_CURRENCIES = Set.of("BAM", "EUR", "USD", "CHF", "GBP");

    private static final BigDecimal MAX_TOTAL = new BigDecimal("1000000");
    private static final int MAX_AGE_DAYS = 365;

    public ValidatorServiceImpl() throws RemoteException {
        super();
    }

    @Override
    public ValidationResult validate(Invoice invoice) throws RemoteException {

        log.info("Primljen zahtjev za validaciju fakture: {}",
                invoice != null ? invoice.getId() : "null");

        List<String> errors = new ArrayList<>();

        if (invoice == null) {
            errors.add("Faktura je prazna");
            return new ValidationResult(false, errors, LocalDateTime.now().toString());
        }

        validateJib(invoice, errors);
        validateEmail(invoice, errors);
        validateDate(invoice, errors);
        validateItems(invoice, errors);
        validateCurrency(invoice, errors);
        validateTotal(invoice, errors);

        boolean valid = errors.isEmpty();

        if (valid) {
            log.info("Faktura {} je ISPRAVNA", invoice.getId());
        } else {
            log.warn("Faktura {} je NEISPRAVNA, gresaka: {} -> {}",
                    invoice.getId(), errors.size(), errors);
        }

        return new ValidationResult(valid, errors, LocalDateTime.now().toString());
    }

    private void validateJib(Invoice invoice, List<String> errors) {
        if (invoice.getClient() == null) {
            errors.add("Podaci o klijentu nedostaju");
            return;
        }
        String jib = invoice.getClient().getJib();
        if (jib == null || !JIB_PATTERN.matcher(jib).matches()) {
            errors.add("JIB mora imati tacno 13 cifara, dobijeno: " + jib);
        }
    }

    private void validateEmail(Invoice invoice, List<String> errors) {
        if (invoice.getClient() == null) {
            return;
        }
        String email = invoice.getClient().getEmail();
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            errors.add("Email nije u ispravnom formatu: " + email);
        }
    }

    private void validateDate(Invoice invoice, List<String> errors) {
        LocalDate date = invoice.getDate();
        if (date == null) {
            errors.add("Datum fakture nedostaje");
            return;
        }
        LocalDate today = LocalDate.now();
        if (date.isAfter(today)) {
            errors.add("Datum ne smije biti u buducnosti: " + date);
        }
        if (date.isBefore(today.minusDays(MAX_AGE_DAYS))) {
            errors.add("Datum je stariji od " + MAX_AGE_DAYS + " dana: " + date);
        }
    }

    private void validateItems(Invoice invoice, List<String> errors) {
        List<InvoiceItem> items = invoice.getItems();
        if (items == null || items.isEmpty()) {
            errors.add("Faktura mora imati barem jednu stavku");
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            InvoiceItem item = items.get(i);
            int redniBroj = i + 1;

            if (item.getQuantity() <= 0) {
                errors.add("Stavka " + redniBroj + ": kolicina mora biti pozitivna, dobijeno "
                        + item.getQuantity());
            }
            if (item.getUnitPrice() == null
                    || item.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("Stavka " + redniBroj + ": cijena mora biti pozitivna, dobijeno "
                        + item.getUnitPrice());
            }
        }
    }

    private void validateCurrency(Invoice invoice, List<String> errors) {
        String currency = invoice.getCurrency();
        if (currency == null || !VALID_CURRENCIES.contains(currency)) {
            errors.add("Valuta nije podrzana: " + currency + ", dozvoljene: " + VALID_CURRENCIES);
        }
    }

    private void validateTotal(Invoice invoice, List<String> errors) {
        BigDecimal total = invoice.calculateTotal();

        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Ukupan iznos mora biti veci od nule, dobijeno: " + total);
        }
        if (total.compareTo(MAX_TOTAL) >= 0) {
            errors.add("Ukupan iznos mora biti manji od 1.000.000, dobijeno: " + total);
        }
    }
}