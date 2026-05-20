package com.minilands.backend.service.investment;

import com.minilands.backend.dto.investment.BuySharesRequest;
import com.minilands.backend.dto.investment.HoldingDetailResponse;
import com.minilands.backend.dto.investment.PortfolioSummaryResponse;
import com.minilands.backend.dto.investment.RoiEarningResponse;
import com.minilands.backend.dto.investment.SellSharesRequest;
import com.minilands.backend.dto.investment.SharePriceResponse;

import java.util.List;

public interface PropertyInvestmentService {

    HoldingDetailResponse buyShares(String userId, BuySharesRequest request);

    HoldingDetailResponse sellShares(String userId, SellSharesRequest request);

    List<HoldingDetailResponse> getHoldings(String userId);

    HoldingDetailResponse getHolding(String userId, String holdingId);

    SharePriceResponse getSharePrice(String propertyId);

    List<RoiEarningResponse> getRoiHistory(String userId, String holdingId);

    PortfolioSummaryResponse getPortfolioSummary(String userId);
}
