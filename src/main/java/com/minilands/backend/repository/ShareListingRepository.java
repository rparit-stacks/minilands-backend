package com.minilands.backend.repository;

import com.minilands.backend.entity.ShareListing;
import com.minilands.backend.entity.enums.MarketplaceListingStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ShareListingRepository extends MongoRepository<ShareListing, String> {

    List<ShareListing> findByPropertyIdAndStatus(String propertyId, MarketplaceListingStatus status);

    List<ShareListing> findBySellerIdAndStatus(String sellerId, MarketplaceListingStatus status);

    List<ShareListing> findBySellerId(String sellerId);

    List<ShareListing> findByStatus(MarketplaceListingStatus status);
}
