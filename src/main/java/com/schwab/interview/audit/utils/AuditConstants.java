package com.schwab.interview.audit.utils;

public final class AuditConstants {
    public static final String API_ROOT = "/audit";
    public static final String EVENTS_PATH = "/events";
    public static final String VERIFY_PATH = "/verify";
    public static final String EXPORT_PATH = "/export";
    public static final String CLIENT_ACCOUNT_ACCESS_REPORT_PATH = "/reports/client-account-access";
    public static final String CLIENT_ACCOUNT_RESOURCE_TYPE = "CLIENT_ACCOUNT";
    public static final String CLIENT_DATA_READ_EVENT_TYPE = "CLIENT_DATA_READ";
    public static final String SHA_256 = "SHA-256";
    public static final String HASH_VERSION = "v1";
    public static final String GENESIS_HASH = "0".repeat(64);
    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String REDACTED_VALUE = "[REDACTED]";
    private AuditConstants() { }
}
