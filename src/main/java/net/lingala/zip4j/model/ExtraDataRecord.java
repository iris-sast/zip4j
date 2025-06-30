/*
 * Decompiled with CFR 0.153-SNAPSHOT (3d1d0f4).
 */
package net.lingala.zip4j.model;

public class ExtraDataRecord {
    private long header;
    private int sizeOfData;
    private byte[] data;

    public long getHeader() {
        return this.header;
    }

    public void setHeader(long header) {
        this.header = header;
    }

    public int getSizeOfData() {
        return this.sizeOfData;
    }

    public void setSizeOfData(int sizeOfData) {
        this.sizeOfData = sizeOfData;
    }

    public byte[] getData() {
        return this.data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}

