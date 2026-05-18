package com.minilands.backend.service.investment;

import com.minilands.backend.dto.investment.HoldingResponse;
import com.minilands.backend.dto.investment.InvestRequest;

import java.util.List;

/**
 * Buy shares and manage investor holdings (SRP).
 */
public interface PropertyInvestmentService {

    HoldingResponse invest(String userId, InvestRequest request);

    List<HoldingResponse> getHoldings(String userId);

    HoldingResponse getHolding(String userId, String holdingId);
}
