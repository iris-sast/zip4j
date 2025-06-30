/*
 * Decompiled with CFR 0.153-SNAPSHOT (3d1d0f4).
 */
package net.lingala.zip4j.io;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.CipherOutputStream;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.model.ZipParameters;

public class DeflaterOutputStream
extends CipherOutputStream {
    private byte[] buff;
    protected Deflater deflater = new Deflater();
    private boolean firstBytesRead = false;

    public DeflaterOutputStream(OutputStream outputStream, ZipModel zipModel) {
        super(outputStream, zipModel);
        this.buff = new byte[4096];
    }

    public void putNextEntry(File file, ZipParameters zipParameters) throws ZipException {
        super.putNextEntry(file, zipParameters);
        if (zipParameters.getCompressionMethod() == 8) {
            this.deflater.reset();
            if ((zipParameters.getCompressionLevel() < 0 || zipParameters.getCompressionLevel() > 9) && zipParameters.getCompressionLevel() != -1) {
                throw new ZipException("invalid compression level for deflater. compression level should be in the range of 0-9");
            }
            this.deflater.setLevel(zipParameters.getCompressionLevel());
        }
    }

    public void write(byte[] b) throws IOException {
        this.write(b, 0, b.length);
    }

    private void deflate() throws IOException {
        int len = this.deflater.deflate(this.buff, 0, this.buff.length);
        if (len > 0) {
            if (this.deflater.finished()) {
                if (len == 4) {
                    return;
                }
                if (len < 4) {
                    this.decrementCompressedFileSize(4 - len);
                    return;
                }
                len -= 4;
            }
            if (!this.firstBytesRead) {
                super.write(this.buff, 2, len - 2);
                this.firstBytesRead = true;
            } else {
                super.write(this.buff, 0, len);
            }
        }
    }

    public void write(int bval) throws IOException {
        byte[] b = new byte[]{(byte)bval};
        this.write(b, 0, 1);
    }

    public void write(byte[] buf, int off, int len) throws IOException {
        if (this.zipParameters.getCompressionMethod() != 8) {
            super.write(buf, off, len);
        } else {
            this.deflater.setInput(buf, off, len);
            while (!this.deflater.needsInput()) {
                this.deflate();
            }
        }
    }

    public void closeEntry() throws IOException, ZipException {
        if (this.zipParameters.getCompressionMethod() == 8) {
            if (!this.deflater.finished()) {
                this.deflater.finish();
                while (!this.deflater.finished()) {
                    this.deflate();
                }
            }
            this.firstBytesRead = false;
        }
        super.closeEntry();
    }

    public void finish() throws IOException, ZipException {
        super.finish();
    }
}

