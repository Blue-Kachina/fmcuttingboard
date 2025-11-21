package dev.fmcuttingboard.util;

import com.intellij.openapi.diagnostic.Logger;

/**
 * Simple diagnostics helper to enable extra verbose logging via a system property.
 * Enable by starting IDE with: -Dfmcuttingboard.verbose=true
 */
public final class Diagnostics {
    private static final String FLAG = "fmcuttingboard.verbose";

    private Diagnostics() {}

    public static boolean isVerbose() {
        try {
            String v = System.getProperty(FLAG, System.getenv("FMCUTTINGBOARD_VERBOSE"));
            return v != null && v.equalsIgnoreCase("true");
        } catch (Throwable t) {
            return false;
        }
    }

    public static void vInfo(Logger log, String msg) {
        if (isVerbose()) log.info("[VERBOSE] " + msg);
    }

    public static void vDebug(Logger log, String msg) {
        if (isVerbose()) log.debug("[VERBOSE] " + msg);
    }
}
