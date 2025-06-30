/*
 * Decompiled with CFR 0.153-SNAPSHOT (3d1d0f4).
 */
package net.lingala.zip4j.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import net.lingala.zip4j.io.PartInputStream;
import net.lingala.zip4j.unzip.UnzipEngine;

public class InflaterInputStream
extends PartInputStream {
    private Inflater inflater;
    private byte[] buff;
    private byte[] oneByteBuff = new byte[1];
    private UnzipEngine unzipEngine;
    private long bytesWritten;
    private long uncompressedSize;

    public InflaterInputStream(RandomAccessFile raf, long start, long len, UnzipEngine unzipEngine) {
        super(raf, start, len, unzipEngine);
        this.inflater = new Inflater(true);
        this.buff = new byte[4096];
        this.unzipEngine = unzipEngine;
        this.bytesWritten = 0L;
        this.uncompressedSize = unzipEngine.getFileHeader().getUncompressedSize();
    }

    @Override
    public int read() throws IOException {
        return this.read(this.oneByteBuff, 0, 1) == -1 ? -1 : this.oneByteBuff[0] & 0xFF;
    }

    @Override
    public int read(byte[] b) throws IOException {
        if (b == null) {
            throw new NullPointerException("input buffer is null");
        }
        return this.read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException("input buffer is null");
        }
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }
        try {
            int n;
            if (this.bytesWritten >= this.uncompressedSize) {
                this.finishInflating();
                return -1;
            }
            while ((n = this.inflater.inflate(b, off, len)) == 0) {
                if (this.inflater.finished() || this.inflater.needsDictionary()) {
                    this.finishInflating();
                    return -1;
                }
                if (!this.inflater.needsInput()) continue;
                this.fill();
            }
            this.bytesWritten += (long)n;
            return n;
        } catch (DataFormatException e) {
            String s = "Invalid ZLIB data format";
            if (e.getMessage() != null) {
                s = e.getMessage();
            }
            if (this.unzipEngine != null && this.unzipEngine.getLocalFileHeader().isEncrypted() && this.unzipEngine.getLocalFileHeader().getEncryptionMethod() == 0) {
                s = s + " - Wrong Password?";
            }
            throw new IOException(s);
        }
    }

    private void finishInflating() throws IOException {
        byte[] b = new byte[1024];
        while (super.read(b, 0, 1024) != -1) {
        }
        this.checkAndReadAESMacBytes();
    }

    private void fill() throws IOException {
        int len = super.read(this.buff, 0, this.buff.length);
        if (len == -1) {
            throw new EOFException("Unexpected end of ZLIB input stream");
        }
        this.inflater.setInput(this.buff, 0, len);
    }

    @Override
    public long skip(long n) throws IOException {
        int total;
        int len;
        if (n < 0L) {
            throw new IllegalArgumentException("negative skip length");
        }
        int max = (int)Math.min(n, Integer.MAX_VALUE);
        byte[] b = new byte[512];
        for (total = 0; total < max; total += len) {
            len = max - total;
            if (len > b.length) {
                len = b.length;
            }
            if ((len = this.read(b, 0, len)) == -1) break;
        }
        return total;
    }

    @Override
    public void seek(long pos) throws IOException {
        super.seek(pos);
    }

    @Override
    public int available() {
        return this.inflater.finished() ? 0 : 1;
    }

    @Override
    public void close() throws IOException {
        this.inflater.end();
        super.close();
    }

    @Override
    public UnzipEngine getUnzipEngine() {
        return super.getUnzipEngine();
    }
}

