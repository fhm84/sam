package de.halbmann.sam.cli.diagnostics;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ArgsHolder {

    private String[] args = new String[0];

    public void setArgs(String... args) {
        this.args = args == null ? new String[0] : args.clone();
    }

    public String[] getArgs() {
        return args.clone();
    }
}
