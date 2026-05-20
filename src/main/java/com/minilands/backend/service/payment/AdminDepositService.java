package com.minilands.backend.service.payment;

import com.minilands.backend.dto.payment.AdminDepositResponse;
import com.minilands.backend.entity.enums.DepositStatus;

import java.util.List;

public interface AdminDepositService {

    List<AdminDepositResponse> listDeposits(DepositStatus status);

    AdminDepositResponse getById(String depositId);

    AdminDepositResponse manuallyComplete(String depositId);
}
