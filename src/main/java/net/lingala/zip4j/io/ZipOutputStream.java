/*
 * Decompiled with CFR 0.153-SNAPSHOT (3d1d0f4).
 */
package net.lingala.zip4j.io;

import java.io.IOException;
import java.io.OutputStream;
import net.lingala.zip4j.io.DeflaterOutputStream;
import net.lingala.zip4j.model.ZipModel;

public class ZipOutputStream
extends DeflaterOutputStream {
    public ZipOutputStream(OutputStream outputStream) {
        this(outputStream, null);
    }

    public ZipOutputStream(OutputStream outputStream, ZipModel zipModel) {
        super(outputStream, zipModel);
    }

    @Override
    public void write(int bval) throws IOException {
        byte[] b = new byte[]{(byte)bval};
        this.write(b, 0, 1);
    }

    @Override
    public void write(byte[] b) throws IOException {
        this.write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        this.crc.update(b, off, len);
        this.updateTotalBytesRead(len);
        super.write(b, off, len);
    }
}

