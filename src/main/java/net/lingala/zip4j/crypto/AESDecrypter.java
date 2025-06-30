/*
 * Decompiled with CFR 0.153-SNAPSHOT (3d1d0f4).
 */
package net.lingala.zip4j.crypto;

import java.util.Arrays;
import net.lingala.zip4j.crypto.IDecrypter;
import net.lingala.zip4j.crypto.PBKDF2.MacBasedPRF;
import net.lingala.zip4j.crypto.PBKDF2.PBKDF2Engine;
import net.lingala.zip4j.crypto.PBKDF2.PBKDF2Parameters;
import net.lingala.zip4j.crypto.engine.AESEngine;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.AESExtraDataRecord;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.util.Raw;

public class AESDecrypter
implements IDecrypter {
    private LocalFileHeader localFileHeader;
    private AESEngine aesEngine;
    private MacBasedPRF mac;
    private final int PASSWORD_VERIFIER_LENGTH = 2;
    private int KEY_LENGTH;
    private int MAC_LENGTH;
    private int SALT_LENGTH;
    private byte[] aesKey;
    private byte[] macKey;
    private byte[] derivedPasswordVerifier;
    private byte[] storedMac;
    private int nonce = 1;
    private byte[] iv;
    private byte[] counterBlock;
    private int loopCount = 0;

    public AESDecrypter(LocalFileHeader localFileHeader, byte[] salt, byte[] passwordVerifier) throws ZipException {
        if (localFileHeader == null) {
            throw new ZipException("one of the input parameters is null in AESDecryptor Constructor");
        }
        this.localFileHeader = localFileHeader;
        this.storedMac = null;
        this.iv = new byte[16];
        this.counterBlock = new byte[16];
        this.init(salt, passwordVerifier);
    }

    private void init(byte[] salt, byte[] passwordVerifier) throws ZipException {
        if (this.localFileHeader == null) {
            throw new ZipException("invalid file header in init method of AESDecryptor");
        }
        AESExtraDataRecord aesExtraDataRecord = this.localFileHeader.getAesExtraDataRecord();
        if (aesExtraDataRecord == null) {
            throw new ZipException("invalid aes extra data record - in init method of AESDecryptor");
        }
        switch (aesExtraDataRecord.getAesStrength()) {
            case 1: {
                this.KEY_LENGTH = 16;
                this.MAC_LENGTH = 16;
                this.SALT_LENGTH = 8;
                break;
            }
            case 2: {
                this.KEY_LENGTH = 24;
                this.MAC_LENGTH = 24;
                this.SALT_LENGTH = 12;
                break;
            }
            case 3: {
                this.KEY_LENGTH = 32;
                this.MAC_LENGTH = 32;
                this.SALT_LENGTH = 16;
                break;
            }
            default: {
                throw new ZipException("invalid aes key strength for file: " + this.localFileHeader.getFileName());
            }
        }
        if (this.localFileHeader.getPassword() == null || this.localFileHeader.getPassword().length <= 0) {
            throw new ZipException("empty or null password provided for AES Decryptor");
        }
        byte[] derivedKey = this.deriveKey(salt, this.localFileHeader.getPassword());
        if (derivedKey == null || derivedKey.length != this.KEY_LENGTH + this.MAC_LENGTH + 2) {
            throw new ZipException("invalid derived key");
        }
        this.aesKey = new byte[this.KEY_LENGTH];
        this.macKey = new byte[this.MAC_LENGTH];
        this.derivedPasswordVerifier = new byte[2];
        System.arraycopy(derivedKey, 0, this.aesKey, 0, this.KEY_LENGTH);
        System.arraycopy(derivedKey, this.KEY_LENGTH, this.macKey, 0, this.MAC_LENGTH);
        System.arraycopy(derivedKey, this.KEY_LENGTH + this.MAC_LENGTH, this.derivedPasswordVerifier, 0, 2);
        if (this.derivedPasswordVerifier == null) {
            throw new ZipException("invalid derived password verifier for AES");
        }
        if (!Arrays.equals(passwordVerifier, this.derivedPasswordVerifier)) {
            throw new ZipException("Wrong Password for file: " + this.localFileHeader.getFileName(), 5);
        }
        this.aesEngine = new AESEngine(this.aesKey);
        this.mac = new MacBasedPRF("HmacSHA1");
        this.mac.init(this.macKey);
    }

    @Override
    public int decryptData(byte[] buff, int start, int len) throws ZipException {
        if (this.aesEngine == null) {
            throw new ZipException("AES not initialized properly");
        }
        try {
            for (int j = start; j < start + len; j += 16) {
                this.loopCount = j + 16 <= start + len ? 16 : start + len - j;
                this.mac.update(buff, j, this.loopCount);
                Raw.prepareBuffAESIVBytes(this.iv, this.nonce, 16);
                this.aesEngine.processBlock(this.iv, this.counterBlock);
                for (int k = 0; k < this.loopCount; ++k) {
                    buff[j + k] = (byte)(buff[j + k] ^ this.counterBlock[k]);
                }
                ++this.nonce;
            }
            return len;
        } catch (ZipException e) {
            throw e;
        } catch (Exception e) {
            throw new ZipException(e);
        }
    }

    @Override
    public int decryptData(byte[] buff) throws ZipException {
        return this.decryptData(buff, 0, buff.length);
    }

    private byte[] deriveKey(byte[] salt, char[] password) throws ZipException {
        try {
            PBKDF2Parameters p = new PBKDF2Parameters("HmacSHA1", "ISO-8859-1", salt, 1000);
            PBKDF2Engine e = new PBKDF2Engine(p);
            byte[] derivedKey = e.deriveKey(password, this.KEY_LENGTH + this.MAC_LENGTH + 2);
            return derivedKey;
        } catch (Exception e) {
            throw new ZipException(e);
        }
    }

    public int getPasswordVerifierLength() {
        return 2;
    }

    public int getSaltLength() {
        return this.SALT_LENGTH;
    }

    public byte[] getCalculatedAuthenticationBytes() {
        return this.mac.doFinal();
    }

    public void setStoredMac(byte[] storedMac) {
        this.storedMac = storedMac;
    }

    public byte[] getStoredMac() {
        return this.storedMac;
    }
}

