package com.minilands.backend.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "property_valuation_logs")
public class PropertyValuationLog {

    @Id
    private String id;

    @Indexed
    private String propertyId;

    private BigDecimal previousTotalValue;
    private BigDecimal newTotalValue;
    private BigDecimal previousSharePrice;
    private BigDecimal newSharePrice;
    private String updatedByAdminId;
    private String note;
    private Instant valuedAt;

    public PropertyValuationLog() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPropertyId() { return propertyId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }

    public BigDecimal getPreviousTotalValue() { return previousTotalValue; }
    public void setPreviousTotalValue(BigDecimal previousTotalValue) { this.previousTotalValue = previousTotalValue; }

    public BigDecimal getNewTotalValue() { return newTotalValue; }
    public void setNewTotalValue(BigDecimal newTotalValue) { this.newTotalValue = newTotalValue; }

    public BigDecimal getPreviousSharePrice() { return previousSharePrice; }
    public void setPreviousSharePrice(BigDecimal previousSharePrice) { this.previousSharePrice = previousSharePrice; }

    public BigDecimal getNewSharePrice() { return newSharePrice; }
    public void setNewSharePrice(BigDecimal newSharePrice) { this.newSharePrice = newSharePrice; }

    public String getUpdatedByAdminId() { return updatedByAdminId; }
    public void setUpdatedByAdminId(String updatedByAdminId) { this.updatedByAdminId = updatedByAdminId; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Instant getValuedAt() { return valuedAt; }
    public void setValuedAt(Instant valuedAt) { this.valuedAt = valuedAt; }
}
