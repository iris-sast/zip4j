/*
 * Decompiled with CFR 0.153-SNAPSHOT (3d1d0f4).
 */
package net.lingala.zip4j.model;

public class ArchiveExtraDataRecord {
    private int signature;
    private int extraFieldLength;
    private String extraFieldData;

    public int getSignature() {
        return this.signature;
    }

    public void setSignature(int signature) {
        this.signature = signature;
    }

    public int getExtraFieldLength() {
        return this.extraFieldLength;
    }

    public void setExtraFieldLength(int extraFieldLength) {
        this.extraFieldLength = extraFieldLength;
    }

    public String getExtraFieldData() {
        return this.extraFieldData;
    }

    public void setExtraFieldData(String extraFieldData) {
        this.extraFieldData = extraFieldData;
    }
}

