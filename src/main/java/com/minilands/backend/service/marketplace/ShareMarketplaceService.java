package com.minilands.backend.service.marketplace;

import com.minilands.backend.dto.marketplace.BuyListedSharesRequest;
import com.minilands.backend.dto.marketplace.ListSharesRequest;
import com.minilands.backend.dto.marketplace.MarketplaceListingResponse;

import java.util.List;

/**
 * Option 1 exit path: investor lists shares on the waiting-list marketplace.
 * Settlement is peer-to-peer — money moves only when a buyer purchases the listing.
 */
public interface ShareMarketplaceService {

    MarketplaceListingResponse listShares(String sellerId, ListSharesRequest request);

    MarketplaceListingResponse buyListing(String buyerId, BuyListedSharesRequest request);

    MarketplaceListingResponse cancelListing(String sellerId, String listingId);

    List<MarketplaceListingResponse> getListingsForProperty(String propertyId);

    List<MarketplaceListingResponse> getMyListings(String sellerId);
}
