/*
 * Decompiled with CFR 0.153-SNAPSHOT (3d1d0f4).
 */
package net.lingala.zip4j.model;

public class AESExtraDataRecord {
    private long signature = -1L;
    private int dataSize = -1;
    private int versionNumber = -1;
    private String vendorID = null;
    private int aesStrength = -1;
    private int compressionMethod = -1;

    public long getSignature() {
        return this.signature;
    }

    public void setSignature(long signature) {
        this.signature = signature;
    }

    public int getDataSize() {
        return this.dataSize;
    }

    public void setDataSize(int dataSize) {
        this.dataSize = dataSize;
    }

    public int getVersionNumber() {
        return this.versionNumber;
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getVendorID() {
        return this.vendorID;
    }

    public void setVendorID(String vendorID) {
        this.vendorID = vendorID;
    }

    public int getAesStrength() {
        return this.aesStrength;
    }

    public void setAesStrength(int aesStrength) {
        this.aesStrength = aesStrength;
    }

    public int getCompressionMethod() {
        return this.compressionMethod;
    }

    public void setCompressionMethod(int compressionMethod) {
        this.compressionMethod = compressionMethod;
    }
}

