/*
 * Decompiled with CFR 0.153-SNAPSHOT (3d1d0f4).
 */
package net.lingala.zip4j.crypto;

import net.lingala.zip4j.exception.ZipException;

public interface IDecrypter {
    public int decryptData(byte[] var1, int var2, int var3) throws ZipException;

    public int decryptData(byte[] var1) throws ZipException;
}

