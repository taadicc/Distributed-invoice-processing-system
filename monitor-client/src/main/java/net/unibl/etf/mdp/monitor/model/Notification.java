package net.unibl.etf.mdp.monitor.model;

import java.math.BigDecimal;

public class Notification {

    private String invoiceId;
    private String clientName;
    private BigDecimal totalBAM;
    private String status;
    private String timestamp;

    public Notification() {
    }

    public String getInvoiceId() { return invoiceId; }
    public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public BigDecimal getTotalBAM() { return totalBAM; }
    public void setTotalBAM(BigDecimal totalBAM) { this.totalBAM = totalBAM; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return invoiceId + " / " + clientName + " / " + totalBAM + " BAM";
    }
}