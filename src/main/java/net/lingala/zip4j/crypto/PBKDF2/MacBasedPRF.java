/*
 * Decompiled with CFR 0.153-SNAPSHOT (3d1d0f4).
 */
package net.lingala.zip4j.crypto.PBKDF2;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import net.lingala.zip4j.crypto.PBKDF2.PRF;

public class MacBasedPRF
implements PRF {
    protected Mac mac;
    protected int hLen;
    protected String macAlgorithm;

    public MacBasedPRF(String macAlgorithm) {
        this.macAlgorithm = macAlgorithm;
        try {
            this.mac = Mac.getInstance(macAlgorithm);
            this.hLen = this.mac.getMacLength();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public MacBasedPRF(String macAlgorithm, String provider) {
        this.macAlgorithm = macAlgorithm;
        try {
            this.mac = Mac.getInstance(macAlgorithm, provider);
            this.hLen = this.mac.getMacLength();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] doFinal(byte[] M) {
        byte[] r = this.mac.doFinal(M);
        return r;
    }

    public byte[] doFinal() {
        byte[] r = this.mac.doFinal();
        return r;
    }

    @Override
    public int getHLen() {
        return this.hLen;
    }

    @Override
    public void init(byte[] P) {
        try {
            this.mac.init(new SecretKeySpec(P, this.macAlgorithm));
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public void update(byte[] U) {
        try {
            this.mac.update(U);
        } catch (IllegalStateException e) {
            throw new RuntimeException(e);
        }
    }

    public void update(byte[] U, int start, int len) {
        try {
            this.mac.update(U, start, len);
        } catch (IllegalStateException e) {
            throw new RuntimeException(e);
        }
    }
}

