/*
 * Decompiled with CFR 0.153-SNAPSHOT (3d1d0f4).
 */
package net.lingala.zip4j.model;

import java.util.ArrayList;
import net.lingala.zip4j.model.AESExtraDataRecord;
import net.lingala.zip4j.model.Zip64ExtendedInfo;

public class LocalFileHeader {
    private int signature;
    private int versionNeededToExtract;
    private byte[] generalPurposeFlag;
    private int compressionMethod;
    private int lastModFileTime;
    private long crc32 = 0L;
    private byte[] crcBuff;
    private long compressedSize;
    private long uncompressedSize = 0L;
    private int fileNameLength;
    private int extraFieldLength;
    private String fileName;
    private byte[] extraField;
    private long offsetStartOfData;
    private boolean isEncrypted;
    private int encryptionMethod = -1;
    private char[] password;
    private ArrayList extraDataRecords;
    private Zip64ExtendedInfo zip64ExtendedInfo;
    private AESExtraDataRecord aesExtraDataRecord;
    private boolean dataDescriptorExists;
    private boolean writeComprSizeInZip64ExtraRecord = false;
    private boolean fileNameUTF8Encoded;

    public int getSignature() {
        return this.signature;
    }

    public void setSignature(int signature) {
        this.signature = signature;
    }

    public int getVersionNeededToExtract() {
        return this.versionNeededToExtract;
    }

    public void setVersionNeededToExtract(int versionNeededToExtract) {
        this.versionNeededToExtract = versionNeededToExtract;
    }

    public byte[] getGeneralPurposeFlag() {
        return this.generalPurposeFlag;
    }

    public void setGeneralPurposeFlag(byte[] generalPurposeFlag) {
        this.generalPurposeFlag = generalPurposeFlag;
    }

    public int getCompressionMethod() {
        return this.compressionMethod;
    }

    public void setCompressionMethod(int compressionMethod) {
        this.compressionMethod = compressionMethod;
    }

    public int getLastModFileTime() {
        return this.lastModFileTime;
    }

    public void setLastModFileTime(int lastModFileTime) {
        this.lastModFileTime = lastModFileTime;
    }

    public long getCrc32() {
        return this.crc32;
    }

    public void setCrc32(long crc32) {
        this.crc32 = crc32;
    }

    public long getCompressedSize() {
        return this.compressedSize;
    }

    public void setCompressedSize(long compressedSize) {
        this.compressedSize = compressedSize;
    }

    public long getUncompressedSize() {
        return this.uncompressedSize;
    }

    public void setUncompressedSize(long uncompressedSize) {
        this.uncompressedSize = uncompressedSize;
    }

    public int getFileNameLength() {
        return this.fileNameLength;
    }

    public void setFileNameLength(int fileNameLength) {
        this.fileNameLength = fileNameLength;
    }

    public int getExtraFieldLength() {
        return this.extraFieldLength;
    }

    public void setExtraFieldLength(int extraFieldLength) {
        this.extraFieldLength = extraFieldLength;
    }

    public String getFileName() {
        return this.fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte[] getExtraField() {
        return this.extraField;
    }

    public void setExtraField(byte[] extraField) {
        this.extraField = extraField;
    }

    public long getOffsetStartOfData() {
        return this.offsetStartOfData;
    }

    public void setOffsetStartOfData(long offsetStartOfData) {
        this.offsetStartOfData = offsetStartOfData;
    }

    public boolean isEncrypted() {
        return this.isEncrypted;
    }

    public void setEncrypted(boolean isEncrypted) {
        this.isEncrypted = isEncrypted;
    }

    public int getEncryptionMethod() {
        return this.encryptionMethod;
    }

    public void setEncryptionMethod(int encryptionMethod) {
        this.encryptionMethod = encryptionMethod;
    }

    public byte[] getCrcBuff() {
        return this.crcBuff;
    }

    public void setCrcBuff(byte[] crcBuff) {
        this.crcBuff = crcBuff;
    }

    public char[] getPassword() {
        return this.password;
    }

    public void setPassword(char[] password) {
        this.password = password;
    }

    public ArrayList getExtraDataRecords() {
        return this.extraDataRecords;
    }

    public void setExtraDataRecords(ArrayList extraDataRecords) {
        this.extraDataRecords = extraDataRecords;
    }

    public boolean isDataDescriptorExists() {
        return this.dataDescriptorExists;
    }

    public void setDataDescriptorExists(boolean dataDescriptorExists) {
        this.dataDescriptorExists = dataDescriptorExists;
    }

    public Zip64ExtendedInfo getZip64ExtendedInfo() {
        return this.zip64ExtendedInfo;
    }

    public void setZip64ExtendedInfo(Zip64ExtendedInfo zip64ExtendedInfo) {
        this.zip64ExtendedInfo = zip64ExtendedInfo;
    }

    public AESExtraDataRecord getAesExtraDataRecord() {
        return this.aesExtraDataRecord;
    }

    public void setAesExtraDataRecord(AESExtraDataRecord aesExtraDataRecord) {
        this.aesExtraDataRecord = aesExtraDataRecord;
    }

    public boolean isWriteComprSizeInZip64ExtraRecord() {
        return this.writeComprSizeInZip64ExtraRecord;
    }

    public void setWriteComprSizeInZip64ExtraRecord(boolean writeComprSizeInZip64ExtraRecord) {
        this.writeComprSizeInZip64ExtraRecord = writeComprSizeInZip64ExtraRecord;
    }

    public boolean isFileNameUTF8Encoded() {
        return this.fileNameUTF8Encoded;
    }

    public void setFileNameUTF8Encoded(boolean fileNameUTF8Encoded) {
        this.fileNameUTF8Encoded = fileNameUTF8Encoded;
    }
}

