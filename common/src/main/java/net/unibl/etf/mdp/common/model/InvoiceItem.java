package net.unibl.etf.mdp.common.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class InvoiceItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private String description;
    private int quantity;
    private BigDecimal unitPrice;

    public InvoiceItem() {
    }

    public InvoiceItem(String description, int quantity, BigDecimal unitPrice) {
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public BigDecimal lineTotal() {
        if (unitPrice == null) {
            return BigDecimal.ZERO;
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    @Override
    public String toString() {
        return description + " x" + quantity + " @ " + unitPrice;
    }
}