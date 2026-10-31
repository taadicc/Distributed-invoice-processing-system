package net.unibl.etf.mdp.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Invoice implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private Client client;
    private LocalDate date;
    private String currency;
    private List<InvoiceItem> items = new ArrayList<>();
    
//dodaju se usput dok faktura putuje
    private BigDecimal totalBAM;
    private String rateSource;
    private Integer retryCount;
    private String status;
    private String rejectionReason;
    private List<String> errors;
    private String sourceFile;

    public Invoice() {
    }

    public BigDecimal calculateTotal() {
        BigDecimal sum = BigDecimal.ZERO;
        if (items != null) {
            for (InvoiceItem item : items) {
                sum = sum.add(item.lineTotal());
            }
        }
        return sum;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public List<InvoiceItem> getItems() { return items; }
    public void setItems(List<InvoiceItem> items) { this.items = items; }

    public BigDecimal getTotalBAM() { return totalBAM; }
    public void setTotalBAM(BigDecimal totalBAM) { this.totalBAM = totalBAM; }

    public String getRateSource() { return rateSource; }
    public void setRateSource(String rateSource) { this.rateSource = rateSource; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public String getSourceFile() { return sourceFile; }
    public void setSourceFile(String sourceFile) { this.sourceFile = sourceFile; }

    @Override
    public String toString() {
        return "Invoice[" + id + ", " + client + ", " + calculateTotal() + " " + currency + "]";
    }
}