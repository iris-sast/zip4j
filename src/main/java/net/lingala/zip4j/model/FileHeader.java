/*
 * Decompiled with CFR 0.153-SNAPSHOT (3d1d0f4).
 */
package net.lingala.zip4j.model;

import java.util.ArrayList;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.AESExtraDataRecord;
import net.lingala.zip4j.model.UnzipParameters;
import net.lingala.zip4j.model.Zip64ExtendedInfo;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.unzip.Unzip;
import net.lingala.zip4j.util.Zip4jUtil;

public class FileHeader {
    private int signature;
    private int versionMadeBy;
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
    private int fileCommentLength;
    private int diskNumberStart;
    private byte[] internalFileAttr;
    private byte[] externalFileAttr;
    private long offsetLocalHeader;
    private String fileName;
    private String fileComment;
    private boolean isDirectory;
    private boolean isEncrypted;
    private int encryptionMethod = -1;
    private char[] password;
    private boolean dataDescriptorExists;
    private Zip64ExtendedInfo zip64ExtendedInfo;
    private AESExtraDataRecord aesExtraDataRecord;
    private ArrayList extraDataRecords;
    private boolean fileNameUTF8Encoded;

    public int getSignature() {
        return this.signature;
    }

    public void setSignature(int signature) {
        this.signature = signature;
    }

    public int getVersionMadeBy() {
        return this.versionMadeBy;
    }

    public void setVersionMadeBy(int versionMadeBy) {
        this.versionMadeBy = versionMadeBy;
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
        return this.crc32 & 0xFFFFFFFFL;
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

    public int getFileCommentLength() {
        return this.fileCommentLength;
    }

    public void setFileCommentLength(int fileCommentLength) {
        this.fileCommentLength = fileCommentLength;
    }

    public int getDiskNumberStart() {
        return this.diskNumberStart;
    }

    public void setDiskNumberStart(int diskNumberStart) {
        this.diskNumberStart = diskNumberStart;
    }

    public byte[] getInternalFileAttr() {
        return this.internalFileAttr;
    }

    public void setInternalFileAttr(byte[] internalFileAttr) {
        this.internalFileAttr = internalFileAttr;
    }

    public byte[] getExternalFileAttr() {
        return this.externalFileAttr;
    }

    public void setExternalFileAttr(byte[] externalFileAttr) {
        this.externalFileAttr = externalFileAttr;
    }

    public long getOffsetLocalHeader() {
        return this.offsetLocalHeader;
    }

    public void setOffsetLocalHeader(long offsetLocalHeader) {
        this.offsetLocalHeader = offsetLocalHeader;
    }

    public String getFileName() {
        return this.fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileComment() {
        return this.fileComment;
    }

    public void setFileComment(String fileComment) {
        this.fileComment = fileComment;
    }

    public boolean isDirectory() {
        return this.isDirectory;
    }

    public void setDirectory(boolean isDirectory) {
        this.isDirectory = isDirectory;
    }

    public void extractFile(ZipModel zipModel, String outPath, ProgressMonitor progressMonitor, boolean runInThread) throws ZipException {
        this.extractFile(zipModel, outPath, null, progressMonitor, runInThread);
    }

    public void extractFile(ZipModel zipModel, String outPath, UnzipParameters unzipParameters, ProgressMonitor progressMonitor, boolean runInThread) throws ZipException {
        this.extractFile(zipModel, outPath, unzipParameters, null, progressMonitor, runInThread);
    }

    public void extractFile(ZipModel zipModel, String outPath, UnzipParameters unzipParameters, String newFileName, ProgressMonitor progressMonitor, boolean runInThread) throws ZipException {
        if (zipModel == null) {
            throw new ZipException("input zipModel is null");
        }
        if (!Zip4jUtil.checkOutputFolder(outPath)) {
            throw new ZipException("Invalid output path");
        }
        if (this == null) {
            throw new ZipException("invalid file header");
        }
        Unzip unzip = new Unzip(zipModel);
        unzip.extractFile(this, outPath, unzipParameters, newFileName, progressMonitor, runInThread);
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

    public char[] getPassword() {
        return this.password;
    }

    public void setPassword(char[] password) {
        this.password = password;
    }

    public byte[] getCrcBuff() {
        return this.crcBuff;
    }

    public void setCrcBuff(byte[] crcBuff) {
        this.crcBuff = crcBuff;
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

    public boolean isFileNameUTF8Encoded() {
        return this.fileNameUTF8Encoded;
    }

    public void setFileNameUTF8Encoded(boolean fileNameUTF8Encoded) {
        this.fileNameUTF8Encoded = fileNameUTF8Encoded;
    }
}

