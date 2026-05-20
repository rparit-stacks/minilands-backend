package com.minilands.backend.entity;

import com.minilands.backend.entity.enums.MarketplaceListingStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "share_listings")
public class ShareListing {

    @Id
    private String id;

    @Indexed
    private String sellerId;

    @Indexed
    private String propertyId;

    private String holdingId;

    private BigDecimal sharesListed;
    private BigDecimal pricePerShare;
    private BigDecimal totalAskPrice;

    private MarketplaceListingStatus status;

    private String buyerId;
    private Instant soldAt;

    private Instant createdAt;
    private Instant updatedAt;

    public ShareListing() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getPropertyId() { return propertyId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }

    public String getHoldingId() { return holdingId; }
    public void setHoldingId(String holdingId) { this.holdingId = holdingId; }

    public BigDecimal getSharesListed() { return sharesListed; }
    public void setSharesListed(BigDecimal sharesListed) { this.sharesListed = sharesListed; }

    public BigDecimal getPricePerShare() { return pricePerShare; }
    public void setPricePerShare(BigDecimal pricePerShare) { this.pricePerShare = pricePerShare; }

    public BigDecimal getTotalAskPrice() { return totalAskPrice; }
    public void setTotalAskPrice(BigDecimal totalAskPrice) { this.totalAskPrice = totalAskPrice; }

    public MarketplaceListingStatus getStatus() { return status; }
    public void setStatus(MarketplaceListingStatus status) { this.status = status; }

    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }

    public Instant getSoldAt() { return soldAt; }
    public void setSoldAt(Instant soldAt) { this.soldAt = soldAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
