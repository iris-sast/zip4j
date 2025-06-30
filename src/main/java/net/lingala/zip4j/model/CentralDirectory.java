/*
 * Decompiled with CFR 0.153-SNAPSHOT (3d1d0f4).
 */
package net.lingala.zip4j.model;

import java.util.ArrayList;
import net.lingala.zip4j.model.DigitalSignature;

public class CentralDirectory {
    private ArrayList fileHeaders;
    private DigitalSignature digitalSignature;

    public ArrayList getFileHeaders() {
        return this.fileHeaders;
    }

    public void setFileHeaders(ArrayList fileHeaders) {
        this.fileHeaders = fileHeaders;
    }

    public DigitalSignature getDigitalSignature() {
        return this.digitalSignature;
    }

    public void setDigitalSignature(DigitalSignature digitalSignature) {
        this.digitalSignature = digitalSignature;
    }
}

