package com.minilands.backend.controller.investor;

import com.minilands.backend.dto.UserPrincipal;
import com.minilands.backend.dto.exit.ExitResponse;
import com.minilands.backend.service.exit.PropertyExitService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exit")
public class ExitController {

    private final PropertyExitService propertyExitService;

    public ExitController(PropertyExitService propertyExitService) {
        this.propertyExitService = propertyExitService;
    }

    @PostMapping("/holdings/{holdingId}")
    public ResponseEntity<ExitResponse> exit(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String holdingId) {
        return ResponseEntity.ok(propertyExitService.exit(principal.getUserId(), holdingId));
    }
}
