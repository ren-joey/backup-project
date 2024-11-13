package com.delta.dms.community.enums;

public enum DrcSyncType {
    BATCH_UPSERT("batch_upsert"),
    BATCH_DELETE("batch_delete"),
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete");

    private String value;

    DrcSyncType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    public static DrcSyncType fromValue(String text) {
        for (DrcSyncType b : DrcSyncType.values()) {
            if (String.valueOf(b.value).equals(text)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + text + "'");
    }
}
