/*
 * Decompiled with CFR 0.153-SNAPSHOT (3d1d0f4).
 */
package net.lingala.zip4j.crypto;

import java.util.Random;
import net.lingala.zip4j.crypto.IEncrypter;
import net.lingala.zip4j.crypto.engine.ZipCryptoEngine;
import net.lingala.zip4j.exception.ZipException;

public class StandardEncrypter
implements IEncrypter {
    private ZipCryptoEngine zipCryptoEngine;
    private byte[] headerBytes;

    public StandardEncrypter(char[] password, int crc) throws ZipException {
        if (password == null || password.length <= 0) {
            throw new ZipException("input password is null or empty in standard encrpyter constructor");
        }
        this.zipCryptoEngine = new ZipCryptoEngine();
        this.headerBytes = new byte[12];
        this.init(password, crc);
    }

    private void init(char[] password, int crc) throws ZipException {
        if (password == null || password.length <= 0) {
            throw new ZipException("input password is null or empty, cannot initialize standard encrypter");
        }
        this.zipCryptoEngine.initKeys(password);
        this.headerBytes = this.generateRandomBytes(12);
        this.zipCryptoEngine.initKeys(password);
        this.headerBytes[11] = (byte)(crc >>> 24);
        this.headerBytes[10] = (byte)(crc >>> 16);
        if (this.headerBytes.length < 12) {
            throw new ZipException("invalid header bytes generated, cannot perform standard encryption");
        }
        this.encryptData(this.headerBytes);
    }

    @Override
    public int encryptData(byte[] buff) throws ZipException {
        if (buff == null) {
            throw new NullPointerException();
        }
        return this.encryptData(buff, 0, buff.length);
    }

    @Override
    public int encryptData(byte[] buff, int start, int len) throws ZipException {
        if (len < 0) {
            throw new ZipException("invalid length specified to decrpyt data");
        }
        try {
            for (int i = start; i < start + len; ++i) {
                buff[i] = this.encryptByte(buff[i]);
            }
            return len;
        } catch (Exception e) {
            throw new ZipException(e);
        }
    }

    protected byte encryptByte(byte val) {
        byte temp_val = (byte)(val ^ this.zipCryptoEngine.decryptByte() & 0xFF);
        this.zipCryptoEngine.updateKeys(val);
        return temp_val;
    }

    protected byte[] generateRandomBytes(int size) throws ZipException {
        if (size <= 0) {
            throw new ZipException("size is either 0 or less than 0, cannot generate header for standard encryptor");
        }
        byte[] buff = new byte[size];
        Random rand = new Random();
        for (int i = 0; i < buff.length; ++i) {
            buff[i] = this.encryptByte((byte)rand.nextInt(256));
        }
        return buff;
    }

    public byte[] getHeaderBytes() {
        return this.headerBytes;
    }
}

