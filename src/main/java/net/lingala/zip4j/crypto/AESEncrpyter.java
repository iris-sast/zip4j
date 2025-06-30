/*
 * Decompiled with CFR 0.153-SNAPSHOT (3d1d0f4).
 */
package net.lingala.zip4j.crypto;

import java.util.Random;
import net.lingala.zip4j.crypto.IEncrypter;
import net.lingala.zip4j.crypto.PBKDF2.MacBasedPRF;
import net.lingala.zip4j.crypto.PBKDF2.PBKDF2Engine;
import net.lingala.zip4j.crypto.PBKDF2.PBKDF2Parameters;
import net.lingala.zip4j.crypto.engine.AESEngine;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.util.Raw;

public class AESEncrpyter
implements IEncrypter {
    private char[] password;
    private int keyStrength;
    private AESEngine aesEngine;
    private MacBasedPRF mac;
    private int KEY_LENGTH;
    private int MAC_LENGTH;
    private int SALT_LENGTH;
    private final int PASSWORD_VERIFIER_LENGTH = 2;
    private byte[] aesKey;
    private byte[] macKey;
    private byte[] derivedPasswordVerifier;
    private byte[] saltBytes;
    private boolean finished;
    private int nonce = 1;
    private int loopCount = 0;
    private byte[] iv;
    private byte[] counterBlock;

    public AESEncrpyter(char[] password, int keyStrength) throws ZipException {
        if (password == null || password.length == 0) {
            throw new ZipException("input password is empty or null in AES encrypter constructor");
        }
        if (keyStrength != 1 && keyStrength != 3) {
            throw new ZipException("Invalid key strength in AES encrypter constructor");
        }
        this.password = password;
        this.keyStrength = keyStrength;
        this.finished = false;
        this.counterBlock = new byte[16];
        this.iv = new byte[16];
        this.init();
    }

    private void init() throws ZipException {
        switch (this.keyStrength) {
            case 1: {
                this.KEY_LENGTH = 16;
                this.MAC_LENGTH = 16;
                this.SALT_LENGTH = 8;
                break;
            }
            case 3: {
                this.KEY_LENGTH = 32;
                this.MAC_LENGTH = 32;
                this.SALT_LENGTH = 16;
                break;
            }
            default: {
                throw new ZipException("invalid aes key strength, cannot determine key sizes");
            }
        }
        this.saltBytes = AESEncrpyter.generateSalt(this.SALT_LENGTH);
        byte[] keyBytes = this.deriveKey(this.saltBytes, this.password);
        if (keyBytes == null || keyBytes.length != this.KEY_LENGTH + this.MAC_LENGTH + 2) {
            throw new ZipException("invalid key generated, cannot decrypt file");
        }
        this.aesKey = new byte[this.KEY_LENGTH];
        this.macKey = new byte[this.MAC_LENGTH];
        this.derivedPasswordVerifier = new byte[2];
        System.arraycopy(keyBytes, 0, this.aesKey, 0, this.KEY_LENGTH);
        System.arraycopy(keyBytes, this.KEY_LENGTH, this.macKey, 0, this.MAC_LENGTH);
        System.arraycopy(keyBytes, this.KEY_LENGTH + this.MAC_LENGTH, this.derivedPasswordVerifier, 0, 2);
        this.aesEngine = new AESEngine(this.aesKey);
        this.mac = new MacBasedPRF("HmacSHA1");
        this.mac.init(this.macKey);
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

    @Override
    public int encryptData(byte[] buff) throws ZipException {
        if (buff == null) {
            throw new ZipException("input bytes are null, cannot perform AES encrpytion");
        }
        return this.encryptData(buff, 0, buff.length);
    }

    @Override
    public int encryptData(byte[] buff, int start, int len) throws ZipException {
        if (this.finished) {
            throw new ZipException("AES Encrypter is in finished state (A non 16 byte block has already been passed to encrypter)");
        }
        if (len % 16 != 0) {
            this.finished = true;
        }
        for (int j = start; j < start + len; j += 16) {
            this.loopCount = j + 16 <= start + len ? 16 : start + len - j;
            Raw.prepareBuffAESIVBytes(this.iv, this.nonce, 16);
            this.aesEngine.processBlock(this.iv, this.counterBlock);
            for (int k = 0; k < this.loopCount; ++k) {
                buff[j + k] = (byte)(buff[j + k] ^ this.counterBlock[k]);
            }
            this.mac.update(buff, j, this.loopCount);
            ++this.nonce;
        }
        return len;
    }

    private static byte[] generateSalt(int size) throws ZipException {
        if (size != 8 && size != 16) {
            throw new ZipException("invalid salt size, cannot generate salt");
        }
        int rounds = 0;
        if (size == 8) {
            rounds = 2;
        }
        if (size == 16) {
            rounds = 4;
        }
        byte[] salt = new byte[size];
        for (int j = 0; j < rounds; ++j) {
            Random rand = new Random();
            int i = rand.nextInt();
            salt[0 + j * 4] = (byte)(i >> 24);
            salt[1 + j * 4] = (byte)(i >> 16);
            salt[2 + j * 4] = (byte)(i >> 8);
            salt[3 + j * 4] = (byte)i;
        }
        return salt;
    }

    public byte[] getFinalMac() {
        byte[] rawMacBytes = this.mac.doFinal();
        byte[] macBytes = new byte[10];
        System.arraycopy(rawMacBytes, 0, macBytes, 0, 10);
        return macBytes;
    }

    public byte[] getDerivedPasswordVerifier() {
        return this.derivedPasswordVerifier;
    }

    public void setDerivedPasswordVerifier(byte[] derivedPasswordVerifier) {
        this.derivedPasswordVerifier = derivedPasswordVerifier;
    }

    public byte[] getSaltBytes() {
        return this.saltBytes;
    }

    public void setSaltBytes(byte[] saltBytes) {
        this.saltBytes = saltBytes;
    }

    public int getSaltLength() {
        return this.SALT_LENGTH;
    }

    public int getPasswordVeriifierLength() {
        return 2;
    }
}

