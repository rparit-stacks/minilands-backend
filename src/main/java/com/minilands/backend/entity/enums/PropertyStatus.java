package com.minilands.backend.entity.enums;

public enum PropertyStatus {
    DRAFT,
    COMING_SOON,
    OPEN,
    FUNDED,
    ACTIVE,
    /** Admin has approved the sale proposal; proceeds not yet distributed to investors. */
    SOLD,
    /** Sale proceeds have been distributed pro-rata; all holdings are settled. */
    EXITED,
    CLOSED
}
