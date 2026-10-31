package net.unibl.etf.mdp.common.rmi;

import net.unibl.etf.mdp.common.model.Invoice;
import net.unibl.etf.mdp.common.model.ValidationResult;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ValidatorService extends Remote {

    String SERVICE_NAME = "ValidatorService";

    ValidationResult validate(Invoice invoice) throws RemoteException;
}