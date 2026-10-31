package net.unibl.etf.mdp.parser.rmi;

import net.unibl.etf.mdp.common.config.AppConfig;
import net.unibl.etf.mdp.common.model.Invoice;
import net.unibl.etf.mdp.common.model.ValidationResult;
import net.unibl.etf.mdp.common.rmi.ValidatorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public final class ValidatorClient {

    private static final Logger log = LoggerFactory.getLogger(ValidatorClient.class);

    private static final String HOST = AppConfig.get("rmi.host", "localhost");
    private static final int PORT = AppConfig.getInt("rmi.port", 1099);

    private static volatile ValidatorService cache;

    private ValidatorClient() {
    }

    public static ValidationResult validate(Invoice invoice) throws RemoteException {
        try {
            return dohvati().validate(invoice);
        } catch (RemoteException e) {
            log.warn("RMI poziv nije uspio, pokusavam ponovo pronaci servis: {}", e.getMessage());
            cache = null;
            return dohvati().validate(invoice);
        }
    }

    private static ValidatorService dohvati() throws RemoteException {
        ValidatorService lokalna = cache;
        if (lokalna != null) {
            return lokalna;
        }

        synchronized (ValidatorClient.class) {
            if (cache == null) {
                try {
                    Registry registry = LocateRegistry.getRegistry(HOST, PORT);
                    cache = (ValidatorService) registry.lookup(ValidatorService.SERVICE_NAME);
                    log.info("Pronadjen Validator servis na {}:{}", HOST, PORT);
                } catch (Exception e) {
                    throw new RemoteException("Ne mogu pronaci Validator servis", e);
                }
            }
            return cache;
        }
    }
}