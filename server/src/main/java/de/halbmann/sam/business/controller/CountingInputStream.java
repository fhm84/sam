package de.halbmann.sam.business.controller;

import de.halbmann.sam.business.entity.AttachmentEntity;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.input.ProxyInputStream;

import java.io.IOException;
import java.io.InputStream;

/**
 * InputStream proxy for getting the streams "size" (number of bytes read) at the end to be able to set/update the
 * document size.
 */
public class CountingInputStream extends ProxyInputStream {

    private long count;

    @Getter
    @Setter
    AttachmentEntity attachment;

    public CountingInputStream(final AttachmentEntity attachment, final InputStream in) {
        super(in);
        this.attachment = attachment;
    }

    protected synchronized void afterRead(int n) throws IOException {
        if (n != -1) {
            this.count += (long) n;
        }

        super.afterRead(n);
    }

    public synchronized long getByteCount() {
        return this.count;
    }

    public synchronized long resetByteCount() {
        long tmp = this.count;
        this.count = 0L;
        return tmp;
    }

    public synchronized long skip(long length) throws IOException {
        long skip = super.skip(length);
        this.count += skip;
        return skip;
    }

    @Override
    public int read() throws IOException {
        final int read = super.read();
        if (read == -1) {
            attachment.setFileSize(getByteCount());
        }
        return read;
    }

    @Override
    public int read(final byte[] bts) throws IOException {
        final int read = super.read(bts);
        if (read == -1) {
            attachment.setFileSize(getByteCount());
        }
        return read;
    }

    @Override
    public int read(final byte[] bts, final int off, final int len) throws IOException {
        final int read = super.read(bts, off, len);
        if (read == -1) {
            attachment.setFileSize(getByteCount());
        }
        return read;
    }

}
