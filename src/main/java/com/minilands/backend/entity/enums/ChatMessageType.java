package com.minilands.backend.entity.enums;

public enum ChatMessageType {
    TEXT,
    IMAGE,
    /** File / document attachment (PDF, doc, etc.). */
    FILE,
    /** Voice note (reserved for a later phase). */
    VOICE,
    /** System-generated line (e.g. "X joined the group"). */
    SYSTEM
}
