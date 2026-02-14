package de.halbmann.sam.cli.diagnostics;

import java.util.concurrent.atomic.AtomicReference;

public final class StaticArgsBridge {

    private static final AtomicReference<String[]> ARGS = new AtomicReference<>(new String[0]);

    private StaticArgsBridge() {}

    public static void setArgs(String... args) {
        ARGS.set(args == null ? new String[0] : args.clone());
    }

    public static String[] getArgs() {
        return ARGS.get().clone();
    }

    public static void clear() {
        ARGS.set(new String[0]);
    }
}
