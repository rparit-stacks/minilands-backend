package com.minilands.backend.service.exit;

import com.minilands.backend.dto.exit.ExitResponse;

/**
 * Investor exit after property sale approval (SRP).
 */
public interface PropertyExitService {

    ExitResponse exit(String userId, String holdingId);
}
