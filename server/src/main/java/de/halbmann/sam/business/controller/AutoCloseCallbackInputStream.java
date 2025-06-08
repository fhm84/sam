package de.halbmann.sam.business.controller;

import org.apache.commons.io.input.ProxyInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

/**
 * Another proxyInputStream implementation to be able to "keep" a callback executed after read. This is used for
 * automatically closing FileObjects opened for providing document data to e.g. remove temporary files automatically
 * created by vfs-s3 library again. This is to not "spam" our tmp directory with lots of copies of loaded documents from
 * remote (like e.g. s3).
 */
public class AutoCloseCallbackInputStream extends ProxyInputStream {

    Consumer<InputStream> func;

    public AutoCloseCallbackInputStream(final InputStream in, final Consumer<InputStream> func) {
        super(in);
        this.func = func;
    }

    @Override
    public void close() throws IOException {
        this.in.close();
        if (func != null) {
            func.accept(this);
        }
    }

    @Override
    protected void afterRead(final int n) throws IOException {
        if (n == -1) {
            this.close();
        }
    }

}
