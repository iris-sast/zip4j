/*
 * Decompiled with CFR 0.153-SNAPSHOT (3d1d0f4).
 */
package net.lingala.zip4j.crypto.PBKDF2;

interface PRF {
    public void init(byte[] var1);

    public byte[] doFinal(byte[] var1);

    public int getHLen();
}

