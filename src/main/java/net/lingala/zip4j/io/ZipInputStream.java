/*
 * Decompiled with CFR 0.153-SNAPSHOT (3d1d0f4).
 */
package net.lingala.zip4j.io;

import java.io.IOException;
import java.io.InputStream;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.BaseInputStream;

public class ZipInputStream
extends InputStream {
    private BaseInputStream is;

    public ZipInputStream(BaseInputStream is) {
        this.is = is;
    }

    public int read() throws IOException {
        int readByte = this.is.read();
        if (readByte != -1) {
            this.is.getUnzipEngine().updateCRC(readByte);
        }
        return readByte;
    }

    public int read(byte[] b) throws IOException {
        return this.read(b, 0, b.length);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int readLen = this.is.read(b, off, len);
        if (readLen > 0 && this.is.getUnzipEngine() != null) {
            this.is.getUnzipEngine().updateCRC(b, off, readLen);
        }
        return readLen;
    }

    public void close() throws IOException {
        this.close(false);
    }

    public void close(boolean skipCRCCheck) throws IOException {
        try {
            this.is.close();
            if (!skipCRCCheck && this.is.getUnzipEngine() != null) {
                this.is.getUnzipEngine().checkCRC();
            }
        } catch (ZipException e) {
            throw new IOException(e.getMessage());
        }
    }

    public int available() throws IOException {
        return this.is.available();
    }

    public long skip(long n) throws IOException {
        return this.is.skip(n);
    }
}

