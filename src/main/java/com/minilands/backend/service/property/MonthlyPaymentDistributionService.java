package com.minilands.backend.service.property;

import com.minilands.backend.dto.property.MonthlyPaymentDistributionResponse;
import com.minilands.backend.dto.property.MonthlyPaymentExecuteRequest;
import com.minilands.backend.dto.property.MonthlyPaymentHistoryItem;
import com.minilands.backend.dto.property.MonthlyPaymentPreviewResponse;

import java.util.List;

public interface MonthlyPaymentDistributionService {

    MonthlyPaymentPreviewResponse preview(String propertyId);

    MonthlyPaymentDistributionResponse distribute(String propertyId, MonthlyPaymentExecuteRequest request);

    List<MonthlyPaymentHistoryItem> history(String propertyId);
}
