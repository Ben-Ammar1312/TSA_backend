package org.example.tas_backend.services;

public final class RevisionUtil {
    private RevisionUtil() {}

    public static boolean isInitialRevision(Number rev) {
        return rev != null && rev.longValue() == 1L;
    }
}
