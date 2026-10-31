package net.unibl.etf.mdp.validator;

import net.unibl.etf.mdp.common.config.AppConfig;
import net.unibl.etf.mdp.common.rmi.ValidatorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ValidatorApp {

    private static final Logger log = LoggerFactory.getLogger(ValidatorApp.class);

    public static void main(String[] args) {
    	String rmiHost = AppConfig.get("rmi.host", "localhost");
        System.setProperty("java.rmi.server.hostname", rmiHost);
        int port = AppConfig.getInt("rmi.port", 1099);
        log.info("RMI hostname postavljen na {}", rmiHost);
        

        try {
            log.info("Pokretanje Validator servisa...");

            Registry registry = LocateRegistry.createRegistry(port);
            log.info("RMI registry podignut na portu {}", port);

            ValidatorService service = new ValidatorServiceImpl();

            registry.rebind(ValidatorService.SERVICE_NAME, service);
            log.info("Servis registrovan pod imenom '{}'", ValidatorService.SERVICE_NAME);

            log.info("Validator servis je spreman i ceka zahtjeve.");

        } catch (Exception e) {
            log.error("Greska pri pokretanju Validator servisa", e);
            System.exit(1);
        }
    }
}