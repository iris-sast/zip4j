/*
 * Decompiled with CFR 0.153-SNAPSHOT (3d1d0f4).
 */
package net.lingala.zip4j.core;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.SplitOutputStream;
import net.lingala.zip4j.model.AESExtraDataRecord;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.Zip64EndCentralDirLocator;
import net.lingala.zip4j.model.Zip64EndCentralDirRecord;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.util.Raw;
import net.lingala.zip4j.util.Zip4jUtil;

public class HeaderWriter {
    private final int ZIP64_EXTRA_BUF = 50;

    public int writeLocalFileHeader(ZipModel zipModel, LocalFileHeader localFileHeader, OutputStream outputStream) throws ZipException {
        if (localFileHeader == null) {
            throw new ZipException("input parameters are null, cannot write local file header");
        }
        try {
            ArrayList byteArrayList = new ArrayList();
            byte[] shortByte = new byte[2];
            byte[] intByte = new byte[4];
            byte[] longByte = new byte[8];
            byte[] emptyLongByte = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
            Raw.writeIntLittleEndian(intByte, 0, localFileHeader.getSignature());
            this.copyByteArrayToArrayList(intByte, byteArrayList);
            Raw.writeShortLittleEndian(shortByte, 0, (short)localFileHeader.getVersionNeededToExtract());
            this.copyByteArrayToArrayList(shortByte, byteArrayList);
            this.copyByteArrayToArrayList(localFileHeader.getGeneralPurposeFlag(), byteArrayList);
            Raw.writeShortLittleEndian(shortByte, 0, (short)localFileHeader.getCompressionMethod());
            this.copyByteArrayToArrayList(shortByte, byteArrayList);
            int dateTime = localFileHeader.getLastModFileTime();
            Raw.writeIntLittleEndian(intByte, 0, dateTime);
            this.copyByteArrayToArrayList(intByte, byteArrayList);
            Raw.writeIntLittleEndian(intByte, 0, (int)localFileHeader.getCrc32());
            this.copyByteArrayToArrayList(intByte, byteArrayList);
            boolean writingZip64Rec = false;
            long uncompressedSize = localFileHeader.getUncompressedSize();
            if (uncompressedSize + 50L >= 0xFFFFFFFFL) {
                Raw.writeLongLittleEndian(longByte, 0, 0xFFFFFFFFL);
                System.arraycopy(longByte, 0, intByte, 0, 4);
                this.copyByteArrayToArrayList(intByte, byteArrayList);
                this.copyByteArrayToArrayList(intByte, byteArrayList);
                zipModel.setZip64Format(true);
                writingZip64Rec = true;
                localFileHeader.setWriteComprSizeInZip64ExtraRecord(true);
            } else {
                Raw.writeLongLittleEndian(longByte, 0, localFileHeader.getCompressedSize());
                System.arraycopy(longByte, 0, intByte, 0, 4);
                this.copyByteArrayToArrayList(intByte, byteArrayList);
                Raw.writeLongLittleEndian(longByte, 0, localFileHeader.getUncompressedSize());
                System.arraycopy(longByte, 0, intByte, 0, 4);
                this.copyByteArrayToArrayList(intByte, byteArrayList);
                localFileHeader.setWriteComprSizeInZip64ExtraRecord(false);
            }
            Raw.writeShortLittleEndian(shortByte, 0, (short)localFileHeader.getFileNameLength());
            this.copyByteArrayToArrayList(shortByte, byteArrayList);
            int extraFieldLength = 0;
            if (writingZip64Rec) {
                extraFieldLength += 20;
            }
            if (localFileHeader.getAesExtraDataRecord() != null) {
                extraFieldLength += 11;
            }
            Raw.writeShortLittleEndian(shortByte, 0, (short)extraFieldLength);
            this.copyByteArrayToArrayList(shortByte, byteArrayList);
            if (Zip4jUtil.isStringNotNullAndNotEmpty(zipModel.getFileNameCharset())) {
                byte[] fileNameBytes = localFileHeader.getFileName().getBytes(zipModel.getFileNameCharset());
                this.copyByteArrayToArrayList(fileNameBytes, byteArrayList);
            } else {
                this.copyByteArrayToArrayList(Zip4jUtil.convertCharset(localFileHeader.getFileName()), byteArrayList);
            }
            if (writingZip64Rec) {
                Raw.writeShortLittleEndian(shortByte, 0, (short)1);
                this.copyByteArrayToArrayList(shortByte, byteArrayList);
                Raw.writeShortLittleEndian(shortByte, 0, (short)16);
                this.copyByteArrayToArrayList(shortByte, byteArrayList);
                Raw.writeLongLittleEndian(longByte, 0, localFileHeader.getUncompressedSize());
                this.copyByteArrayToArrayList(longByte, byteArrayList);
                this.copyByteArrayToArrayList(emptyLongByte, byteArrayList);
            }
            if (localFileHeader.getAesExtraDataRecord() != null) {
                AESExtraDataRecord aesExtraDataRecord = localFileHeader.getAesExtraDataRecord();
                Raw.writeShortLittleEndian(shortByte, 0, (short)aesExtraDataRecord.getSignature());
                this.copyByteArrayToArrayList(shortByte, byteArrayList);
                Raw.writeShortLittleEndian(shortByte, 0, (short)aesExtraDataRecord.getDataSize());
                this.copyByteArrayToArrayList(shortByte, byteArrayList);
                Raw.writeShortLittleEndian(shortByte, 0, (short)aesExtraDataRecord.getVersionNumber());
                this.copyByteArrayToArrayList(shortByte, byteArrayList);
                this.copyByteArrayToArrayList(aesExtraDataRecord.getVendorID().getBytes(), byteArrayList);
                byte[] aesStrengthBytes = new byte[]{(byte)aesExtraDataRecord.getAesStrength()};
                this.copyByteArrayToArrayList(aesStrengthBytes, byteArrayList);
                Raw.writeShortLittleEndian(shortByte, 0, (short)aesExtraDataRecord.getCompressionMethod());
                this.copyByteArrayToArrayList(shortByte, byteArrayList);
            }
            byte[] lhBytes = this.byteArrayListToByteArray(byteArrayList);
            outputStream.write(lhBytes);
            return lhBytes.length;
        } catch (ZipException e) {
            throw e;
        } catch (Exception e) {
            throw new ZipException(e);
        }
    }

    public int writeExtendedLocalHeader(LocalFileHeader localFileHeader, OutputStream outputStream) throws ZipException, IOException {
        if (localFileHeader == null || outputStream == null) {
            throw new ZipException("input parameters is null, cannot write extended local header");
        }
        ArrayList byteArrayList = new ArrayList();
        byte[] intByte = new byte[4];
        Raw.writeIntLittleEndian(intByte, 0, 134695760);
        this.copyByteArrayToArrayList(intByte, byteArrayList);
        Raw.writeIntLittleEndian(intByte, 0, (int)localFileHeader.getCrc32());
        this.copyByteArrayToArrayList(intByte, byteArrayList);
        long compressedSize = localFileHeader.getCompressedSize();
        if (compressedSize >= Integer.MAX_VALUE) {
            compressedSize = Integer.MAX_VALUE;
        }
        Raw.writeIntLittleEndian(intByte, 0, (int)compressedSize);
        this.copyByteArrayToArrayList(intByte, byteArrayList);
        long uncompressedSize = localFileHeader.getUncompressedSize();
        if (uncompressedSize >= Integer.MAX_VALUE) {
            uncompressedSize = Integer.MAX_VALUE;
        }
        Raw.writeIntLittleEndian(intByte, 0, (int)uncompressedSize);
        this.copyByteArrayToArrayList(intByte, byteArrayList);
        byte[] extLocHdrBytes = this.byteArrayListToByteArray(byteArrayList);
        outputStream.write(extLocHdrBytes);
        return extLocHdrBytes.length;
    }

    public void finalizeZipFile(ZipModel zipModel, OutputStream outputStream) throws ZipException {
        if (zipModel == null || outputStream == null) {
            throw new ZipException("input parameters is null, cannot finalize zip file");
        }
        try {
            this.processHeaderData(zipModel, outputStream);
            long offsetCentralDir = zipModel.getEndCentralDirRecord().getOffsetOfStartOfCentralDir();
            ArrayList headerBytesList = new ArrayList();
            int sizeOfCentralDir = this.writeCentralDirectory(zipModel, outputStream, headerBytesList);
            if (zipModel.isZip64Format()) {
                if (zipModel.getZip64EndCentralDirRecord() == null) {
                    zipModel.setZip64EndCentralDirRecord(new Zip64EndCentralDirRecord());
                }
                if (zipModel.getZip64EndCentralDirLocator() == null) {
                    zipModel.setZip64EndCentralDirLocator(new Zip64EndCentralDirLocator());
                }
                zipModel.getZip64EndCentralDirLocator().setOffsetZip64EndOfCentralDirRec(offsetCentralDir + (long)sizeOfCentralDir);
                if (outputStream instanceof SplitOutputStream) {
                    zipModel.getZip64EndCentralDirLocator().setNoOfDiskStartOfZip64EndOfCentralDirRec(((SplitOutputStream)outputStream).getCurrSplitFileCounter());
                    zipModel.getZip64EndCentralDirLocator().setTotNumberOfDiscs(((SplitOutputStream)outputStream).getCurrSplitFileCounter() + 1);
                } else {
                    zipModel.getZip64EndCentralDirLocator().setNoOfDiskStartOfZip64EndOfCentralDirRec(0);
                    zipModel.getZip64EndCentralDirLocator().setTotNumberOfDiscs(1);
                }
                this.writeZip64EndOfCentralDirectoryRecord(zipModel, outputStream, sizeOfCentralDir, offsetCentralDir, headerBytesList);
                this.writeZip64EndOfCentralDirectoryLocator(zipModel, outputStream, headerBytesList);
            }
            this.writeEndOfCentralDirectoryRecord(zipModel, outputStream, sizeOfCentralDir, offsetCentralDir, headerBytesList);
            this.writeZipHeaderBytes(zipModel, outputStream, this.byteArrayListToByteArray(headerBytesList));
        } catch (ZipException e) {
            throw e;
        } catch (Exception e) {
            throw new ZipException(e);
        }
    }

    public void finalizeZipFileWithoutValidations(ZipModel zipModel, OutputStream outputStream) throws ZipException {
        if (zipModel == null || outputStream == null) {
            throw new ZipException("input parameters is null, cannot finalize zip file without validations");
        }
        try {
            ArrayList headerBytesList = new ArrayList();
            long offsetCentralDir = zipModel.getEndCentralDirRecord().getOffsetOfStartOfCentralDir();
            int sizeOfCentralDir = this.writeCentralDirectory(zipModel, outputStream, headerBytesList);
            if (zipModel.isZip64Format()) {
                if (zipModel.getZip64EndCentralDirRecord() == null) {
                    zipModel.setZip64EndCentralDirRecord(new Zip64EndCentralDirRecord());
                }
                if (zipModel.getZip64EndCentralDirLocator() == null) {
                    zipModel.setZip64EndCentralDirLocator(new Zip64EndCentralDirLocator());
                }
                zipModel.getZip64EndCentralDirLocator().setOffsetZip64EndOfCentralDirRec(offsetCentralDir + (long)sizeOfCentralDir);
                this.writeZip64EndOfCentralDirectoryRecord(zipModel, outputStream, sizeOfCentralDir, offsetCentralDir, headerBytesList);
                this.writeZip64EndOfCentralDirectoryLocator(zipModel, outputStream, headerBytesList);
            }
            this.writeEndOfCentralDirectoryRecord(zipModel, outputStream, sizeOfCentralDir, offsetCentralDir, headerBytesList);
            this.writeZipHeaderBytes(zipModel, outputStream, this.byteArrayListToByteArray(headerBytesList));
        } catch (ZipException e) {
            throw e;
        } catch (Exception e) {
            throw new ZipException(e);
        }
    }

    private void writeZipHeaderBytes(ZipModel zipModel, OutputStream outputStream, byte[] buff) throws ZipException {
        if (buff == null) {
            throw new ZipException("invalid buff to write as zip headers");
        }
        try {
            if (outputStream instanceof SplitOutputStream && ((SplitOutputStream)outputStream).checkBuffSizeAndStartNextSplitFile(buff.length)) {
                this.finalizeZipFile(zipModel, outputStream);
                return;
            }
            outputStream.write(buff);
        } catch (IOException e) {
            throw new ZipException(e);
        }
    }

    private void processHeaderData(ZipModel zipModel, OutputStream outputStream) throws ZipException {
        try {
            int currSplitFileCounter = 0;
            if (outputStream instanceof SplitOutputStream) {
                zipModel.getEndCentralDirRecord().setOffsetOfStartOfCentralDir(((SplitOutputStream)outputStream).getFilePointer());
                currSplitFileCounter = ((SplitOutputStream)outputStream).getCurrSplitFileCounter();
            }
            if (zipModel.isZip64Format()) {
                if (zipModel.getZip64EndCentralDirRecord() == null) {
                    zipModel.setZip64EndCentralDirRecord(new Zip64EndCentralDirRecord());
                }
                if (zipModel.getZip64EndCentralDirLocator() == null) {
                    zipModel.setZip64EndCentralDirLocator(new Zip64EndCentralDirLocator());
                }
                zipModel.getZip64EndCentralDirLocator().setNoOfDiskStartOfZip64EndOfCentralDirRec(currSplitFileCounter);
                zipModel.getZip64EndCentralDirLocator().setTotNumberOfDiscs(currSplitFileCounter + 1);
            }
            zipModel.getEndCentralDirRecord().setNoOfThisDisk(currSplitFileCounter);
            zipModel.getEndCentralDirRecord().setNoOfThisDiskStartOfCentralDir(currSplitFileCounter);
        } catch (IOException e) {
            throw new ZipException(e);
        }
    }

    private int writeCentralDirectory(ZipModel zipModel, OutputStream outputStream, List headerBytesList) throws ZipException {
        if (zipModel == null || outputStream == null) {
            throw new ZipException("input parameters is null, cannot write central directory");
        }
        if (zipModel.getCentralDirectory() == null || zipModel.getCentralDirectory().getFileHeaders() == null || zipModel.getCentralDirectory().getFileHeaders().size() <= 0) {
            return 0;
        }
        int sizeOfCentralDir = 0;
        for (int i = 0; i < zipModel.getCentralDirectory().getFileHeaders().size(); ++i) {
            FileHeader fileHeader = (FileHeader)zipModel.getCentralDirectory().getFileHeaders().get(i);
            int sizeOfFileHeader = this.writeFileHeader(zipModel, fileHeader, outputStream, headerBytesList);
            sizeOfCentralDir += sizeOfFileHeader;
        }
        return sizeOfCentralDir;
    }

    private int writeFileHeader(ZipModel zipModel, FileHeader fileHeader, OutputStream outputStream, List headerBytesList) throws ZipException {
        if (fileHeader == null || outputStream == null) {
            throw new ZipException("input parameters is null, cannot write local file header");
        }
        try {
            int sizeOfFileHeader = 0;
            byte[] shortByte = new byte[2];
            byte[] intByte = new byte[4];
            byte[] longByte = new byte[8];
            byte[] emptyShortByte = new byte[]{0, 0};
            byte[] emptyIntByte = new byte[]{0, 0, 0, 0};
            boolean writeZip64FileSize = false;
            boolean writeZip64OffsetLocalHeader = false;
            Raw.writeIntLittleEndian(intByte, 0, fileHeader.getSignature());
            this.copyByteArrayToArrayList(intByte, headerBytesList);
            sizeOfFileHeader += 4;
            Raw.writeShortLittleEndian(shortByte, 0, (short)fileHeader.getVersionMadeBy());
            this.copyByteArrayToArrayList(shortByte, headerBytesList);
            sizeOfFileHeader += 2;
            Raw.writeShortLittleEndian(shortByte, 0, (short)fileHeader.getVersionNeededToExtract());
            this.copyByteArrayToArrayList(shortByte, headerBytesList);
            sizeOfFileHeader += 2;
            this.copyByteArrayToArrayList(fileHeader.getGeneralPurposeFlag(), headerBytesList);
            sizeOfFileHeader += 2;
            Raw.writeShortLittleEndian(shortByte, 0, (short)fileHeader.getCompressionMethod());
            this.copyByteArrayToArrayList(shortByte, headerBytesList);
            sizeOfFileHeader += 2;
            int dateTime = fileHeader.getLastModFileTime();
            Raw.writeIntLittleEndian(intByte, 0, dateTime);
            this.copyByteArrayToArrayList(intByte, headerBytesList);
            sizeOfFileHeader += 4;
            Raw.writeIntLittleEndian(intByte, 0, (int)fileHeader.getCrc32());
            this.copyByteArrayToArrayList(intByte, headerBytesList);
            sizeOfFileHeader += 4;
            if (fileHeader.getCompressedSize() >= 0xFFFFFFFFL || fileHeader.getUncompressedSize() + 50L >= 0xFFFFFFFFL) {
                Raw.writeLongLittleEndian(longByte, 0, 0xFFFFFFFFL);
                System.arraycopy(longByte, 0, intByte, 0, 4);
                this.copyByteArrayToArrayList(intByte, headerBytesList);
                sizeOfFileHeader += 4;
                this.copyByteArrayToArrayList(intByte, headerBytesList);
                sizeOfFileHeader += 4;
                writeZip64FileSize = true;
            } else {
                Raw.writeLongLittleEndian(longByte, 0, fileHeader.getCompressedSize());
                System.arraycopy(longByte, 0, intByte, 0, 4);
                this.copyByteArrayToArrayList(intByte, headerBytesList);
                sizeOfFileHeader += 4;
                Raw.writeLongLittleEndian(longByte, 0, fileHeader.getUncompressedSize());
                System.arraycopy(longByte, 0, intByte, 0, 4);
                this.copyByteArrayToArrayList(intByte, headerBytesList);
                sizeOfFileHeader += 4;
            }
            Raw.writeShortLittleEndian(shortByte, 0, (short)fileHeader.getFileNameLength());
            this.copyByteArrayToArrayList(shortByte, headerBytesList);
            sizeOfFileHeader += 2;
            byte[] offsetLocalHeaderBytes = new byte[4];
            if (fileHeader.getOffsetLocalHeader() > 0xFFFFFFFFL) {
                Raw.writeLongLittleEndian(longByte, 0, 0xFFFFFFFFL);
                System.arraycopy(longByte, 0, offsetLocalHeaderBytes, 0, 4);
                writeZip64OffsetLocalHeader = true;
            } else {
                Raw.writeLongLittleEndian(longByte, 0, fileHeader.getOffsetLocalHeader());
                System.arraycopy(longByte, 0, offsetLocalHeaderBytes, 0, 4);
            }
            int extraFieldLength = 0;
            if (writeZip64FileSize || writeZip64OffsetLocalHeader) {
                extraFieldLength += 4;
                if (writeZip64FileSize) {
                    extraFieldLength += 16;
                }
                if (writeZip64OffsetLocalHeader) {
                    extraFieldLength += 8;
                }
            }
            if (fileHeader.getAesExtraDataRecord() != null) {
                extraFieldLength += 11;
            }
            Raw.writeShortLittleEndian(shortByte, 0, (short)extraFieldLength);
            this.copyByteArrayToArrayList(shortByte, headerBytesList);
            sizeOfFileHeader += 2;
            this.copyByteArrayToArrayList(emptyShortByte, headerBytesList);
            sizeOfFileHeader += 2;
            Raw.writeShortLittleEndian(shortByte, 0, (short)fileHeader.getDiskNumberStart());
            this.copyByteArrayToArrayList(shortByte, headerBytesList);
            sizeOfFileHeader += 2;
            this.copyByteArrayToArrayList(emptyShortByte, headerBytesList);
            sizeOfFileHeader += 2;
            if (fileHeader.getExternalFileAttr() != null) {
                this.copyByteArrayToArrayList(fileHeader.getExternalFileAttr(), headerBytesList);
            } else {
                this.copyByteArrayToArrayList(emptyIntByte, headerBytesList);
            }
            sizeOfFileHeader += 4;
            this.copyByteArrayToArrayList(offsetLocalHeaderBytes, headerBytesList);
            sizeOfFileHeader += 4;
            if (Zip4jUtil.isStringNotNullAndNotEmpty(zipModel.getFileNameCharset())) {
                byte[] fileNameBytes = fileHeader.getFileName().getBytes(zipModel.getFileNameCharset());
                this.copyByteArrayToArrayList(fileNameBytes, headerBytesList);
                sizeOfFileHeader += fileNameBytes.length;
            } else {
                this.copyByteArrayToArrayList(Zip4jUtil.convertCharset(fileHeader.getFileName()), headerBytesList);
                sizeOfFileHeader += Zip4jUtil.getEncodedStringLength(fileHeader.getFileName());
            }
            if (writeZip64FileSize || writeZip64OffsetLocalHeader) {
                zipModel.setZip64Format(true);
                Raw.writeShortLittleEndian(shortByte, 0, (short)1);
                this.copyByteArrayToArrayList(shortByte, headerBytesList);
                sizeOfFileHeader += 2;
                int dataSize = 0;
                if (writeZip64FileSize) {
                    dataSize += 16;
                }
                if (writeZip64OffsetLocalHeader) {
                    dataSize += 8;
                }
                Raw.writeShortLittleEndian(shortByte, 0, (short)dataSize);
                this.copyByteArrayToArrayList(shortByte, headerBytesList);
                sizeOfFileHeader += 2;
                if (writeZip64FileSize) {
                    Raw.writeLongLittleEndian(longByte, 0, fileHeader.getUncompressedSize());
                    this.copyByteArrayToArrayList(longByte, headerBytesList);
                    sizeOfFileHeader += 8;
                    Raw.writeLongLittleEndian(longByte, 0, fileHeader.getCompressedSize());
                    this.copyByteArrayToArrayList(longByte, headerBytesList);
                    sizeOfFileHeader += 8;
                }
                if (writeZip64OffsetLocalHeader) {
                    Raw.writeLongLittleEndian(longByte, 0, fileHeader.getOffsetLocalHeader());
                    this.copyByteArrayToArrayList(longByte, headerBytesList);
                    sizeOfFileHeader += 8;
                }
            }
            if (fileHeader.getAesExtraDataRecord() != null) {
                AESExtraDataRecord aesExtraDataRecord = fileHeader.getAesExtraDataRecord();
                Raw.writeShortLittleEndian(shortByte, 0, (short)aesExtraDataRecord.getSignature());
                this.copyByteArrayToArrayList(shortByte, headerBytesList);
                Raw.writeShortLittleEndian(shortByte, 0, (short)aesExtraDataRecord.getDataSize());
                this.copyByteArrayToArrayList(shortByte, headerBytesList);
                Raw.writeShortLittleEndian(shortByte, 0, (short)aesExtraDataRecord.getVersionNumber());
                this.copyByteArrayToArrayList(shortByte, headerBytesList);
                this.copyByteArrayToArrayList(aesExtraDataRecord.getVendorID().getBytes(), headerBytesList);
                byte[] aesStrengthBytes = new byte[]{(byte)aesExtraDataRecord.getAesStrength()};
                this.copyByteArrayToArrayList(aesStrengthBytes, headerBytesList);
                Raw.writeShortLittleEndian(shortByte, 0, (short)aesExtraDataRecord.getCompressionMethod());
                this.copyByteArrayToArrayList(shortByte, headerBytesList);
                sizeOfFileHeader += 11;
            }
            return sizeOfFileHeader;
        } catch (Exception e) {
            throw new ZipException(e);
        }
    }

    private void writeZip64EndOfCentralDirectoryRecord(ZipModel zipModel, OutputStream outputStream, int sizeOfCentralDir, long offsetCentralDir, List headerBytesList) throws ZipException {
        if (zipModel == null || outputStream == null) {
            throw new ZipException("zip model or output stream is null, cannot write zip64 end of central directory record");
        }
        try {
            byte[] shortByte = new byte[2];
            byte[] emptyShortByte = new byte[]{0, 0};
            byte[] intByte = new byte[4];
            byte[] longByte = new byte[8];
            Raw.writeIntLittleEndian(intByte, 0, 101075792);
            this.copyByteArrayToArrayList(intByte, headerBytesList);
            Raw.writeLongLittleEndian(longByte, 0, 44L);
            this.copyByteArrayToArrayList(longByte, headerBytesList);
            if (zipModel.getCentralDirectory() != null && zipModel.getCentralDirectory().getFileHeaders() != null && zipModel.getCentralDirectory().getFileHeaders().size() > 0) {
                Raw.writeShortLittleEndian(shortByte, 0, (short)((FileHeader)zipModel.getCentralDirectory().getFileHeaders().get(0)).getVersionMadeBy());
                this.copyByteArrayToArrayList(shortByte, headerBytesList);
                Raw.writeShortLittleEndian(shortByte, 0, (short)((FileHeader)zipModel.getCentralDirectory().getFileHeaders().get(0)).getVersionNeededToExtract());
                this.copyByteArrayToArrayList(shortByte, headerBytesList);
            } else {
                this.copyByteArrayToArrayList(emptyShortByte, headerBytesList);
                this.copyByteArrayToArrayList(emptyShortByte, headerBytesList);
            }
            Raw.writeIntLittleEndian(intByte, 0, zipModel.getEndCentralDirRecord().getNoOfThisDisk());
            this.copyByteArrayToArrayList(intByte, headerBytesList);
            Raw.writeIntLittleEndian(intByte, 0, zipModel.getEndCentralDirRecord().getNoOfThisDiskStartOfCentralDir());
            this.copyByteArrayToArrayList(intByte, headerBytesList);
            int numEntries = 0;
            int numEntriesOnThisDisk = 0;
            if (zipModel.getCentralDirectory() == null || zipModel.getCentralDirectory().getFileHeaders() == null) {
                throw new ZipException("invalid central directory/file headers, cannot write end of central directory record");
            }
            numEntries = zipModel.getCentralDirectory().getFileHeaders().size();
            if (zipModel.isSplitArchive()) {
                this.countNumberOfFileHeaderEntriesOnDisk(zipModel.getCentralDirectory().getFileHeaders(), zipModel.getEndCentralDirRecord().getNoOfThisDisk());
            } else {
                numEntriesOnThisDisk = numEntries;
            }
            Raw.writeLongLittleEndian(longByte, 0, numEntriesOnThisDisk);
            this.copyByteArrayToArrayList(longByte, headerBytesList);
            Raw.writeLongLittleEndian(longByte, 0, numEntries);
            this.copyByteArrayToArrayList(longByte, headerBytesList);
            Raw.writeLongLittleEndian(longByte, 0, sizeOfCentralDir);
            this.copyByteArrayToArrayList(longByte, headerBytesList);
            Raw.writeLongLittleEndian(longByte, 0, offsetCentralDir);
            this.copyByteArrayToArrayList(longByte, headerBytesList);
        } catch (ZipException zipException) {
            throw zipException;
        } catch (Exception e) {
            throw new ZipException(e);
        }
    }

    private void writeZip64EndOfCentralDirectoryLocator(ZipModel zipModel, OutputStream outputStream, List headerBytesList) throws ZipException {
        if (zipModel == null || outputStream == null) {
            throw new ZipException("zip model or output stream is null, cannot write zip64 end of central directory locator");
        }
        try {
            byte[] intByte = new byte[4];
            byte[] longByte = new byte[8];
            Raw.writeIntLittleEndian(intByte, 0, 117853008);
            this.copyByteArrayToArrayList(intByte, headerBytesList);
            Raw.writeIntLittleEndian(intByte, 0, zipModel.getZip64EndCentralDirLocator().getNoOfDiskStartOfZip64EndOfCentralDirRec());
            this.copyByteArrayToArrayList(intByte, headerBytesList);
            Raw.writeLongLittleEndian(longByte, 0, zipModel.getZip64EndCentralDirLocator().getOffsetZip64EndOfCentralDirRec());
            this.copyByteArrayToArrayList(longByte, headerBytesList);
            Raw.writeIntLittleEndian(intByte, 0, zipModel.getZip64EndCentralDirLocator().getTotNumberOfDiscs());
            this.copyByteArrayToArrayList(intByte, headerBytesList);
        } catch (ZipException zipException) {
            throw zipException;
        } catch (Exception e) {
            throw new ZipException(e);
        }
    }

    private void writeEndOfCentralDirectoryRecord(ZipModel zipModel, OutputStream outputStream, int sizeOfCentralDir, long offsetCentralDir, List headrBytesList) throws ZipException {
        if (zipModel == null || outputStream == null) {
            throw new ZipException("zip model or output stream is null, cannot write end of central directory record");
        }
        try {
            byte[] shortByte = new byte[2];
            byte[] intByte = new byte[4];
            byte[] longByte = new byte[8];
            Raw.writeIntLittleEndian(intByte, 0, (int)zipModel.getEndCentralDirRecord().getSignature());
            this.copyByteArrayToArrayList(intByte, headrBytesList);
            Raw.writeShortLittleEndian(shortByte, 0, (short)zipModel.getEndCentralDirRecord().getNoOfThisDisk());
            this.copyByteArrayToArrayList(shortByte, headrBytesList);
            Raw.writeShortLittleEndian(shortByte, 0, (short)zipModel.getEndCentralDirRecord().getNoOfThisDiskStartOfCentralDir());
            this.copyByteArrayToArrayList(shortByte, headrBytesList);
            int numEntries = 0;
            int numEntriesOnThisDisk = 0;
            if (zipModel.getCentralDirectory() == null || zipModel.getCentralDirectory().getFileHeaders() == null) {
                throw new ZipException("invalid central directory/file headers, cannot write end of central directory record");
            }
            numEntries = zipModel.getCentralDirectory().getFileHeaders().size();
            numEntriesOnThisDisk = zipModel.isSplitArchive() ? this.countNumberOfFileHeaderEntriesOnDisk(zipModel.getCentralDirectory().getFileHeaders(), zipModel.getEndCentralDirRecord().getNoOfThisDisk()) : numEntries;
            Raw.writeShortLittleEndian(shortByte, 0, (short)numEntriesOnThisDisk);
            this.copyByteArrayToArrayList(shortByte, headrBytesList);
            Raw.writeShortLittleEndian(shortByte, 0, (short)numEntries);
            this.copyByteArrayToArrayList(shortByte, headrBytesList);
            Raw.writeIntLittleEndian(intByte, 0, sizeOfCentralDir);
            this.copyByteArrayToArrayList(intByte, headrBytesList);
            if (offsetCentralDir > 0xFFFFFFFFL) {
                Raw.writeLongLittleEndian(longByte, 0, 0xFFFFFFFFL);
                System.arraycopy(longByte, 0, intByte, 0, 4);
                this.copyByteArrayToArrayList(intByte, headrBytesList);
            } else {
                Raw.writeLongLittleEndian(longByte, 0, offsetCentralDir);
                System.arraycopy(longByte, 0, intByte, 0, 4);
                this.copyByteArrayToArrayList(intByte, headrBytesList);
            }
            int commentLength = 0;
            if (zipModel.getEndCentralDirRecord().getComment() != null) {
                commentLength = zipModel.getEndCentralDirRecord().getCommentLength();
            }
            Raw.writeShortLittleEndian(shortByte, 0, (short)commentLength);
            this.copyByteArrayToArrayList(shortByte, headrBytesList);
            if (commentLength > 0) {
                this.copyByteArrayToArrayList(zipModel.getEndCentralDirRecord().getCommentBytes(), headrBytesList);
            }
        } catch (Exception e) {
            throw new ZipException(e);
        }
    }

    public void updateLocalFileHeader(LocalFileHeader localFileHeader, long offset, int toUpdate, ZipModel zipModel, byte[] bytesToWrite, int noOfDisk, SplitOutputStream outputStream) throws ZipException {
        if (localFileHeader == null || offset < 0L || zipModel == null) {
            throw new ZipException("invalid input parameters, cannot update local file header");
        }
        try {
            boolean closeFlag = false;
            SplitOutputStream currOutputStream = null;
            if (noOfDisk != outputStream.getCurrSplitFileCounter()) {
                File zipFile = new File(zipModel.getZipFile());
                String parentFile = zipFile.getParent();
                String fileNameWithoutExt = Zip4jUtil.getZipFileNameWithoutExt(zipFile.getName());
                String fileName = parentFile + System.getProperty("file.separator");
                fileName = noOfDisk < 9 ? fileName + fileNameWithoutExt + ".z0" + (noOfDisk + 1) : fileName + fileNameWithoutExt + ".z" + (noOfDisk + 1);
                currOutputStream = new SplitOutputStream(new File(fileName));
                closeFlag = true;
            } else {
                currOutputStream = outputStream;
            }
            long currOffset = currOutputStream.getFilePointer();
            switch (toUpdate) {
                case 14: {
                    currOutputStream.seek(offset + (long)toUpdate);
                    currOutputStream.write(bytesToWrite);
                    break;
                }
                case 18: 
                case 22: {
                    this.updateCompressedSizeInLocalFileHeader(currOutputStream, localFileHeader, offset, toUpdate, bytesToWrite, zipModel.isZip64Format());
                    break;
                }
            }
            if (closeFlag) {
                currOutputStream.close();
            } else {
                outputStream.seek(currOffset);
            }
        } catch (Exception e) {
            throw new ZipException(e);
        }
    }

    private void updateCompressedSizeInLocalFileHeader(SplitOutputStream outputStream, LocalFileHeader localFileHeader, long offset, long toUpdate, byte[] bytesToWrite, boolean isZip64Format) throws ZipException {
        if (outputStream == null) {
            throw new ZipException("invalid output stream, cannot update compressed size for local file header");
        }
        try {
            if (localFileHeader.isWriteComprSizeInZip64ExtraRecord()) {
                if (bytesToWrite.length != 8) {
                    throw new ZipException("attempting to write a non 8-byte compressed size block for a zip64 file");
                }
                long zip64CompressedSizeOffset = offset + toUpdate + 4L + 4L + 2L + 2L + (long)localFileHeader.getFileNameLength() + 2L + 2L + 8L;
                if (toUpdate == 22L) {
                    zip64CompressedSizeOffset += 8L;
                }
                outputStream.seek(zip64CompressedSizeOffset);
                outputStream.write(bytesToWrite);
            } else {
                outputStream.seek(offset + toUpdate);
                outputStream.write(bytesToWrite);
            }
        } catch (IOException e) {
            throw new ZipException(e);
        }
    }

    private void copyByteArrayToArrayList(byte[] byteArray, List arrayList) throws ZipException {
        if (arrayList == null || byteArray == null) {
            throw new ZipException("one of the input parameters is null, cannot copy byte array to array list");
        }
        for (int i = 0; i < byteArray.length; ++i) {
            arrayList.add(Byte.toString(byteArray[i]));
        }
    }

    private byte[] byteArrayListToByteArray(List arrayList) throws ZipException {
        if (arrayList == null) {
            throw new ZipException("input byte array list is null, cannot conver to byte array");
        }
        if (arrayList.size() <= 0) {
            return null;
        }
        byte[] retBytes = new byte[arrayList.size()];
        for (int i = 0; i < arrayList.size(); ++i) {
            retBytes[i] = Byte.parseByte((String)arrayList.get(i));
        }
        return retBytes;
    }

    private int countNumberOfFileHeaderEntriesOnDisk(ArrayList fileHeaders, int numOfDisk) throws ZipException {
        if (fileHeaders == null) {
            throw new ZipException("file headers are null, cannot calculate number of entries on this disk");
        }
        int noEntries = 0;
        for (int i = 0; i < fileHeaders.size(); ++i) {
            FileHeader fileHeader = (FileHeader)fileHeaders.get(i);
            if (fileHeader.getDiskNumberStart() != numOfDisk) continue;
            ++noEntries;
        }
        return noEntries;
    }
}

