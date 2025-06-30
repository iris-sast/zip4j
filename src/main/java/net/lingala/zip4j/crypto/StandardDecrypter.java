/*
 * Decompiled with CFR 0.153-SNAPSHOT (3d1d0f4).
 */
package net.lingala.zip4j.crypto;

import net.lingala.zip4j.crypto.IDecrypter;
import net.lingala.zip4j.crypto.engine.ZipCryptoEngine;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;

public class StandardDecrypter
implements IDecrypter {
    private FileHeader fileHeader;
    private byte[] crc = new byte[4];
    private ZipCryptoEngine zipCryptoEngine;

    public StandardDecrypter(FileHeader fileHeader, byte[] headerBytes) throws ZipException {
        if (fileHeader == null) {
            throw new ZipException("one of more of the input parameters were null in StandardDecryptor");
        }
        this.fileHeader = fileHeader;
        this.zipCryptoEngine = new ZipCryptoEngine();
        this.init(headerBytes);
    }

    public int decryptData(byte[] buff) throws ZipException {
        return this.decryptData(buff, 0, buff.length);
    }

    public int decryptData(byte[] buff, int start, int len) throws ZipException {
        if (start < 0 || len < 0) {
            throw new ZipException("one of the input parameters were null in standard decrpyt data");
        }
        try {
            for (int i = start; i < start + len; ++i) {
                int val = buff[i] & 0xFF;
                val = (val ^ this.zipCryptoEngine.decryptByte()) & 0xFF;
                this.zipCryptoEngine.updateKeys((byte)val);
                buff[i] = (byte)val;
            }
            return len;
        } catch (Exception e) {
            throw new ZipException(e);
        }
    }

    public void init(byte[] headerBytes) throws ZipException {
        byte[] crcBuff = this.fileHeader.getCrcBuff();
        this.crc[3] = (byte)(crcBuff[3] & 0xFF);
        this.crc[2] = (byte)(crcBuff[3] >> 8 & 0xFF);
        this.crc[1] = (byte)(crcBuff[3] >> 16 & 0xFF);
        this.crc[0] = (byte)(crcBuff[3] >> 24 & 0xFF);
        if (this.crc[2] > 0 || this.crc[1] > 0 || this.crc[0] > 0) {
            throw new IllegalStateException("Invalid CRC in File Header");
        }
        if (this.fileHeader.getPassword() == null || this.fileHeader.getPassword().length <= 0) {
            throw new ZipException("Wrong password!", 5);
        }
        this.zipCryptoEngine.initKeys(this.fileHeader.getPassword());
        try {
            byte result = headerBytes[0];
            for (int i = 0; i < 12; ++i) {
                this.zipCryptoEngine.updateKeys((byte)(result ^ this.zipCryptoEngine.decryptByte()));
                if (i + 1 == 12) continue;
                result = headerBytes[i + 1];
            }
        } catch (Exception e) {
            throw new ZipException(e);
        }
    }
}

