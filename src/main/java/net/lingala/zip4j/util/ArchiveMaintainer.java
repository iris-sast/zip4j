/*
 * Decompiled with CFR 0.153-SNAPSHOT (3d1d0f4).
 */
package net.lingala.zip4j.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import net.lingala.zip4j.core.HeaderReader;
import net.lingala.zip4j.core.HeaderWriter;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.SplitOutputStream;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.util.Raw;
import net.lingala.zip4j.util.Zip4jUtil;

public class ArchiveMaintainer {
    public HashMap removeZipFile(final ZipModel zipModel, final FileHeader fileHeader, final ProgressMonitor progressMonitor, boolean runInThread) throws ZipException {
        if (runInThread) {
            Thread thread = new Thread("Zip4j"){

                @Override
                public void run() {
                    try {
                        ArchiveMaintainer.this.initRemoveZipFile(zipModel, fileHeader, progressMonitor);
                        progressMonitor.endProgressMonitorSuccess();
                    } catch (ZipException zipException) {
                        // empty catch block
                    }
                }
            };
            thread.start();
            return null;
        }
        HashMap retMap = this.initRemoveZipFile(zipModel, fileHeader, progressMonitor);
        progressMonitor.endProgressMonitorSuccess();
        return retMap;
    }

    /*
     * Loose catch block
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public HashMap initRemoveZipFile(ZipModel zipModel, FileHeader fileHeader, ProgressMonitor progressMonitor) throws ZipException {
        long offsetEndOfCompressedFile;
        long offsetLocalFileHeader;
        int indexOfFileHeader;
        HashMap<String, String> retMap;
        String tmpZipFileName;
        boolean successFlag;
        RandomAccessFile inputStream;
        File zipFile;
        OutputStream outputStream;
        block42: {
            FileHeader nextFileHeader;
            ArrayList fileHeaderList;
            if (fileHeader == null) throw new ZipException("input parameters is null in maintain zip file, cannot remove file from archive");
            if (zipModel == null) {
                throw new ZipException("input parameters is null in maintain zip file, cannot remove file from archive");
            }
            outputStream = null;
            zipFile = null;
            inputStream = null;
            successFlag = false;
            tmpZipFileName = null;
            retMap = new HashMap<String, String>();
            indexOfFileHeader = Zip4jUtil.getIndexOfFileHeader(zipModel, fileHeader);
            if (indexOfFileHeader < 0) {
                throw new ZipException("file header not found in zip model, cannot remove file");
            }
            if (zipModel.isSplitArchive()) {
                throw new ZipException("This is a split archive. Zip file format does not allow updating split/spanned files");
            }
            long currTime = System.currentTimeMillis();
            tmpZipFileName = zipModel.getZipFile() + currTime % 1000L;
            File tmpFile = new File(tmpZipFileName);
            while (tmpFile.exists()) {
                currTime = System.currentTimeMillis();
                tmpZipFileName = zipModel.getZipFile() + currTime % 1000L;
                tmpFile = new File(tmpZipFileName);
            }
            try {
                outputStream = new SplitOutputStream(new File(tmpZipFileName));
            } catch (FileNotFoundException e1) {
                throw new ZipException(e1);
            }
            zipFile = new File(zipModel.getZipFile());
            inputStream = this.createFileHandler(zipModel, "r");
            HeaderReader headerReader = new HeaderReader(inputStream);
            LocalFileHeader localFileHeader = headerReader.readLocalFileHeader(fileHeader);
            if (localFileHeader == null) {
                throw new ZipException("invalid local file header, cannot remove file from archive");
            }
            offsetLocalFileHeader = fileHeader.getOffsetLocalHeader();
            if (fileHeader.getZip64ExtendedInfo() != null && fileHeader.getZip64ExtendedInfo().getOffsetLocalHeader() != -1L) {
                offsetLocalFileHeader = fileHeader.getZip64ExtendedInfo().getOffsetLocalHeader();
            }
            offsetEndOfCompressedFile = -1L;
            long offsetStartCentralDir = zipModel.getEndCentralDirRecord().getOffsetOfStartOfCentralDir();
            if (zipModel.isZip64Format() && zipModel.getZip64EndCentralDirRecord() != null) {
                offsetStartCentralDir = zipModel.getZip64EndCentralDirRecord().getOffsetStartCenDirWRTStartDiskNo();
            }
            if (indexOfFileHeader == (fileHeaderList = zipModel.getCentralDirectory().getFileHeaders()).size() - 1) {
                offsetEndOfCompressedFile = offsetStartCentralDir - 1L;
            } else {
                nextFileHeader = (FileHeader)fileHeaderList.get(indexOfFileHeader + 1);
                if (nextFileHeader != null) {
                    offsetEndOfCompressedFile = nextFileHeader.getOffsetLocalHeader() - 1L;
                    if (nextFileHeader.getZip64ExtendedInfo() != null && nextFileHeader.getZip64ExtendedInfo().getOffsetLocalHeader() != -1L) {
                        offsetEndOfCompressedFile = nextFileHeader.getZip64ExtendedInfo().getOffsetLocalHeader() - 1L;
                    }
                }
            }
            if (offsetLocalFileHeader < 0L) throw new ZipException("invalid offset for start and end of local file, cannot remove file");
            if (offsetEndOfCompressedFile < 0L) {
                throw new ZipException("invalid offset for start and end of local file, cannot remove file");
            }
            if (indexOfFileHeader == 0) {
                if (zipModel.getCentralDirectory().getFileHeaders().size() > 1) {
                    this.copyFile(inputStream, outputStream, offsetEndOfCompressedFile + 1L, offsetStartCentralDir, progressMonitor);
                }
            } else if (indexOfFileHeader == fileHeaderList.size() - 1) {
                this.copyFile(inputStream, outputStream, 0L, offsetLocalFileHeader, progressMonitor);
            } else {
                this.copyFile(inputStream, outputStream, 0L, offsetLocalFileHeader, progressMonitor);
                this.copyFile(inputStream, outputStream, offsetEndOfCompressedFile + 1L, offsetStartCentralDir, progressMonitor);
            }
            if (!progressMonitor.isCancelAllTasks()) break block42;
            progressMonitor.setResult(3);
            progressMonitor.setState(0);
            nextFileHeader = null;
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                throw new ZipException("cannot close input stream or output stream when trying to delete a file from zip file");
            }
            if (successFlag) {
                this.restoreFileName(zipFile, tmpZipFileName);
                return nextFileHeader;
            }
            File newZipFile = new File(tmpZipFileName);
            newZipFile.delete();
            return nextFileHeader;
        }
        zipModel.getEndCentralDirRecord().setOffsetOfStartOfCentralDir(((SplitOutputStream)outputStream).getFilePointer());
        zipModel.getEndCentralDirRecord().setTotNoOfEntriesInCentralDir(zipModel.getEndCentralDirRecord().getTotNoOfEntriesInCentralDir() - 1);
        zipModel.getEndCentralDirRecord().setTotNoOfEntriesInCentralDirOnThisDisk(zipModel.getEndCentralDirRecord().getTotNoOfEntriesInCentralDirOnThisDisk() - 1);
        zipModel.getCentralDirectory().getFileHeaders().remove(indexOfFileHeader);
        for (int i = indexOfFileHeader; i < zipModel.getCentralDirectory().getFileHeaders().size(); ++i) {
            long offsetLocalHdr = ((FileHeader)zipModel.getCentralDirectory().getFileHeaders().get(i)).getOffsetLocalHeader();
            if (((FileHeader)zipModel.getCentralDirectory().getFileHeaders().get(i)).getZip64ExtendedInfo() != null && ((FileHeader)zipModel.getCentralDirectory().getFileHeaders().get(i)).getZip64ExtendedInfo().getOffsetLocalHeader() != -1L) {
                offsetLocalHdr = ((FileHeader)zipModel.getCentralDirectory().getFileHeaders().get(i)).getZip64ExtendedInfo().getOffsetLocalHeader();
            }
            ((FileHeader)zipModel.getCentralDirectory().getFileHeaders().get(i)).setOffsetLocalHeader(offsetLocalHdr - (offsetEndOfCompressedFile - offsetLocalFileHeader) - 1L);
        }
        HeaderWriter headerWriter = new HeaderWriter();
        headerWriter.finalizeZipFile(zipModel, outputStream);
        successFlag = true;
        retMap.put("offsetCentralDir", Long.toString(zipModel.getEndCentralDirRecord().getOffsetOfStartOfCentralDir()));
        try {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException e) {
            throw new ZipException("cannot close input stream or output stream when trying to delete a file from zip file");
        }
        if (successFlag) {
            this.restoreFileName(zipFile, tmpZipFileName);
            return retMap;
        }
        File newZipFile = new File(tmpZipFileName);
        newZipFile.delete();
        return retMap;
        catch (ZipException e) {
            try {
                progressMonitor.endProgressMonitorError(e);
                throw e;
                catch (Exception e2) {
                    progressMonitor.endProgressMonitorError(e2);
                    throw new ZipException(e2);
                }
            } catch (Throwable throwable) {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    if (outputStream != null) {
                        outputStream.close();
                    }
                } catch (IOException e3) {
                    throw new ZipException("cannot close input stream or output stream when trying to delete a file from zip file");
                }
                if (successFlag) {
                    this.restoreFileName(zipFile, tmpZipFileName);
                    throw throwable;
                }
                File newZipFile2 = new File(tmpZipFileName);
                newZipFile2.delete();
                throw throwable;
            }
        }
    }

    private void restoreFileName(File zipFile, String tmpZipFileName) throws ZipException {
        if (zipFile.delete()) {
            File newZipFile = new File(tmpZipFileName);
            if (!newZipFile.renameTo(zipFile)) {
                throw new ZipException("cannot rename modified zip file");
            }
        } else {
            throw new ZipException("cannot delete old zip file");
        }
    }

    private void copyFile(RandomAccessFile inputStream, OutputStream outputStream, long start, long end, ProgressMonitor progressMonitor) throws ZipException {
        if (inputStream == null || outputStream == null) {
            throw new ZipException("input or output stream is null, cannot copy file");
        }
        if (start < 0L) {
            throw new ZipException("starting offset is negative, cannot copy file");
        }
        if (end < 0L) {
            throw new ZipException("end offset is negative, cannot copy file");
        }
        if (start > end) {
            throw new ZipException("start offset is greater than end offset, cannot copy file");
        }
        if (start == end) {
            return;
        }
        if (progressMonitor.isCancelAllTasks()) {
            progressMonitor.setResult(3);
            progressMonitor.setState(0);
            return;
        }
        try {
            inputStream.seek(start);
            int readLen = -2;
            long bytesRead = 0L;
            long bytesToRead = end - start;
            byte[] buff = end - start < 4096L ? new byte[(int)(end - start)] : new byte[4096];
            while ((readLen = inputStream.read(buff)) != -1) {
                outputStream.write(buff, 0, readLen);
                progressMonitor.updateWorkCompleted(readLen);
                if (progressMonitor.isCancelAllTasks()) {
                    progressMonitor.setResult(3);
                    return;
                }
                if ((bytesRead += (long)readLen) != bytesToRead) {
                    if (bytesRead + (long)buff.length <= bytesToRead) continue;
                    buff = new byte[(int)(bytesToRead - bytesRead)];
                    continue;
                }
                break;
            }
        } catch (IOException e) {
            throw new ZipException(e);
        } catch (Exception e) {
            throw new ZipException(e);
        }
    }

    private RandomAccessFile createFileHandler(ZipModel zipModel, String mode) throws ZipException {
        if (zipModel == null || !Zip4jUtil.isStringNotNullAndNotEmpty(zipModel.getZipFile())) {
            throw new ZipException("input parameter is null in getFilePointer, cannot create file handler to remove file");
        }
        try {
            return new RandomAccessFile(new File(zipModel.getZipFile()), mode);
        } catch (FileNotFoundException e) {
            throw new ZipException(e);
        }
    }

    public void mergeSplitZipFiles(final ZipModel zipModel, final File outputZipFile, final ProgressMonitor progressMonitor, boolean runInThread) throws ZipException {
        if (runInThread) {
            Thread thread = new Thread("Zip4j"){

                @Override
                public void run() {
                    try {
                        ArchiveMaintainer.this.initMergeSplitZipFile(zipModel, outputZipFile, progressMonitor);
                    } catch (ZipException zipException) {
                        // empty catch block
                    }
                }
            };
            thread.start();
        } else {
            this.initMergeSplitZipFile(zipModel, outputZipFile, progressMonitor);
        }
    }

    private void initMergeSplitZipFile(ZipModel zipModel, File outputZipFile, ProgressMonitor progressMonitor) throws ZipException {
        if (zipModel == null) {
            ZipException e = new ZipException("one of the input parameters is null, cannot merge split zip file");
            progressMonitor.endProgressMonitorError(e);
            throw e;
        }
        if (!zipModel.isSplitArchive()) {
            ZipException e = new ZipException("archive not a split zip file");
            progressMonitor.endProgressMonitorError(e);
            throw e;
        }
        OutputStream outputStream = null;
        RandomAccessFile inputStream = null;
        ArrayList<Long> fileSizeList = new ArrayList<Long>();
        long totBytesWritten = 0L;
        boolean splitSigRemoved = false;
        try {
            int totNoOfSplitFiles = zipModel.getEndCentralDirRecord().getNoOfThisDisk();
            if (totNoOfSplitFiles <= 0) {
                throw new ZipException("corrupt zip model, archive not a split zip file");
            }
            outputStream = this.prepareOutputStreamForMerge(outputZipFile);
            for (int i = 0; i <= totNoOfSplitFiles; ++i) {
                inputStream = this.createSplitZipFileHandler(zipModel, i);
                int start = 0;
                Long end = new Long(inputStream.length());
                if (i == 0 && zipModel.getCentralDirectory() != null && zipModel.getCentralDirectory().getFileHeaders() != null && zipModel.getCentralDirectory().getFileHeaders().size() > 0) {
                    byte[] buff = new byte[4];
                    inputStream.seek(0L);
                    inputStream.read(buff);
                    if ((long)Raw.readIntLittleEndian(buff, 0) == 134695760L) {
                        start = 4;
                        splitSigRemoved = true;
                    }
                }
                if (i == totNoOfSplitFiles) {
                    end = new Long(zipModel.getEndCentralDirRecord().getOffsetOfStartOfCentralDir());
                }
                this.copyFile(inputStream, outputStream, start, end, progressMonitor);
                totBytesWritten += end - (long)start;
                if (progressMonitor.isCancelAllTasks()) {
                    progressMonitor.setResult(3);
                    progressMonitor.setState(0);
                    return;
                }
                fileSizeList.add(end);
                try {
                    inputStream.close();
                    continue;
                } catch (IOException iOException) {
                    // empty catch block
                }
            }
            ZipModel newZipModel = (ZipModel)zipModel.clone();
            newZipModel.getEndCentralDirRecord().setOffsetOfStartOfCentralDir(totBytesWritten);
            this.updateSplitZipModel(newZipModel, fileSizeList, splitSigRemoved);
            HeaderWriter headerWriter = new HeaderWriter();
            headerWriter.finalizeZipFileWithoutValidations(newZipModel, outputStream);
            progressMonitor.endProgressMonitorSuccess();
        } catch (IOException e) {
            progressMonitor.endProgressMonitorError(e);
            throw new ZipException(e);
        } catch (Exception e) {
            progressMonitor.endProgressMonitorError(e);
            throw new ZipException(e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException iOException) {}
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException iOException) {}
            }
        }
    }

    private RandomAccessFile createSplitZipFileHandler(ZipModel zipModel, int partNumber) throws ZipException {
        if (zipModel == null) {
            throw new ZipException("zip model is null, cannot create split file handler");
        }
        if (partNumber < 0) {
            throw new ZipException("invlaid part number, cannot create split file handler");
        }
        try {
            String curZipFile = zipModel.getZipFile();
            String partFile = null;
            partFile = partNumber == zipModel.getEndCentralDirRecord().getNoOfThisDisk() ? zipModel.getZipFile() : (partNumber >= 9 ? curZipFile.substring(0, curZipFile.lastIndexOf(".")) + ".z" + (partNumber + 1) : curZipFile.substring(0, curZipFile.lastIndexOf(".")) + ".z0" + (partNumber + 1));
            File tmpFile = new File(partFile);
            if (!Zip4jUtil.checkFileExists(tmpFile)) {
                throw new ZipException("split file does not exist: " + partFile);
            }
            return new RandomAccessFile(tmpFile, "r");
        } catch (FileNotFoundException e) {
            throw new ZipException(e);
        } catch (Exception e) {
            throw new ZipException(e);
        }
    }

    private OutputStream prepareOutputStreamForMerge(File outFile) throws ZipException {
        if (outFile == null) {
            throw new ZipException("outFile is null, cannot create outputstream");
        }
        try {
            return new FileOutputStream(outFile);
        } catch (FileNotFoundException e) {
            throw new ZipException(e);
        } catch (Exception e) {
            throw new ZipException(e);
        }
    }

    private void updateSplitZipModel(ZipModel zipModel, ArrayList fileSizeList, boolean splitSigRemoved) throws ZipException {
        if (zipModel == null) {
            throw new ZipException("zip model is null, cannot update split zip model");
        }
        zipModel.setSplitArchive(false);
        this.updateSplitFileHeader(zipModel, fileSizeList, splitSigRemoved);
        this.updateSplitEndCentralDirectory(zipModel);
        if (zipModel.isZip64Format()) {
            this.updateSplitZip64EndCentralDirLocator(zipModel, fileSizeList);
            this.updateSplitZip64EndCentralDirRec(zipModel, fileSizeList);
        }
    }

    private void updateSplitFileHeader(ZipModel zipModel, ArrayList fileSizeList, boolean splitSigRemoved) throws ZipException {
        try {
            if (zipModel.getCentralDirectory() == null) {
                throw new ZipException("corrupt zip model - getCentralDirectory, cannot update split zip model");
            }
            int fileHeaderCount = zipModel.getCentralDirectory().getFileHeaders().size();
            int splitSigOverhead = 0;
            if (splitSigRemoved) {
                splitSigOverhead = 4;
            }
            for (int i = 0; i < fileHeaderCount; ++i) {
                long offsetLHToAdd = 0L;
                for (int j = 0; j < ((FileHeader)zipModel.getCentralDirectory().getFileHeaders().get(i)).getDiskNumberStart(); ++j) {
                    offsetLHToAdd += ((Long)fileSizeList.get(j)).longValue();
                }
                ((FileHeader)zipModel.getCentralDirectory().getFileHeaders().get(i)).setOffsetLocalHeader(((FileHeader)zipModel.getCentralDirectory().getFileHeaders().get(i)).getOffsetLocalHeader() + offsetLHToAdd - (long)splitSigOverhead);
                ((FileHeader)zipModel.getCentralDirectory().getFileHeaders().get(i)).setDiskNumberStart(0);
            }
        } catch (ZipException e) {
            throw e;
        } catch (Exception e) {
            throw new ZipException(e);
        }
    }

    private void updateSplitEndCentralDirectory(ZipModel zipModel) throws ZipException {
        try {
            if (zipModel == null) {
                throw new ZipException("zip model is null - cannot update end of central directory for split zip model");
            }
            if (zipModel.getCentralDirectory() == null) {
                throw new ZipException("corrupt zip model - getCentralDirectory, cannot update split zip model");
            }
            zipModel.getEndCentralDirRecord().setNoOfThisDisk(0);
            zipModel.getEndCentralDirRecord().setNoOfThisDiskStartOfCentralDir(0);
            zipModel.getEndCentralDirRecord().setTotNoOfEntriesInCentralDir(zipModel.getCentralDirectory().getFileHeaders().size());
            zipModel.getEndCentralDirRecord().setTotNoOfEntriesInCentralDirOnThisDisk(zipModel.getCentralDirectory().getFileHeaders().size());
        } catch (ZipException e) {
            throw e;
        } catch (Exception e) {
            throw new ZipException(e);
        }
    }

    private void updateSplitZip64EndCentralDirLocator(ZipModel zipModel, ArrayList fileSizeList) throws ZipException {
        if (zipModel == null) {
            throw new ZipException("zip model is null, cannot update split Zip64 end of central directory locator");
        }
        if (zipModel.getZip64EndCentralDirLocator() == null) {
            return;
        }
        zipModel.getZip64EndCentralDirLocator().setNoOfDiskStartOfZip64EndOfCentralDirRec(0);
        long offsetZip64EndCentralDirRec = 0L;
        for (int i = 0; i < fileSizeList.size(); ++i) {
            offsetZip64EndCentralDirRec += ((Long)fileSizeList.get(i)).longValue();
        }
        zipModel.getZip64EndCentralDirLocator().setOffsetZip64EndOfCentralDirRec(zipModel.getZip64EndCentralDirLocator().getOffsetZip64EndOfCentralDirRec() + offsetZip64EndCentralDirRec);
        zipModel.getZip64EndCentralDirLocator().setTotNumberOfDiscs(1);
    }

    private void updateSplitZip64EndCentralDirRec(ZipModel zipModel, ArrayList fileSizeList) throws ZipException {
        if (zipModel == null) {
            throw new ZipException("zip model is null, cannot update split Zip64 end of central directory record");
        }
        if (zipModel.getZip64EndCentralDirRecord() == null) {
            return;
        }
        zipModel.getZip64EndCentralDirRecord().setNoOfThisDisk(0);
        zipModel.getZip64EndCentralDirRecord().setNoOfThisDiskStartOfCentralDir(0);
        zipModel.getZip64EndCentralDirRecord().setTotNoOfEntriesInCentralDirOnThisDisk(zipModel.getEndCentralDirRecord().getTotNoOfEntriesInCentralDir());
        long offsetStartCenDirWRTStartDiskNo = 0L;
        for (int i = 0; i < fileSizeList.size(); ++i) {
            offsetStartCenDirWRTStartDiskNo += ((Long)fileSizeList.get(i)).longValue();
        }
        zipModel.getZip64EndCentralDirRecord().setOffsetStartCenDirWRTStartDiskNo(zipModel.getZip64EndCentralDirRecord().getOffsetStartCenDirWRTStartDiskNo() + offsetStartCenDirWRTStartDiskNo);
    }

    public void setComment(ZipModel zipModel, String comment) throws ZipException {
        if (comment == null) {
            throw new ZipException("comment is null, cannot update Zip file with comment");
        }
        if (zipModel == null) {
            throw new ZipException("zipModel is null, cannot update Zip file with comment");
        }
        String encodedComment = comment;
        byte[] commentBytes = comment.getBytes();
        int commentLength = comment.length();
        if (Zip4jUtil.isSupportedCharset("windows-1254")) {
            try {
                encodedComment = new String(comment.getBytes("windows-1254"), "windows-1254");
                commentBytes = encodedComment.getBytes("windows-1254");
                commentLength = encodedComment.length();
            } catch (UnsupportedEncodingException e) {
                encodedComment = comment;
                commentBytes = comment.getBytes();
                commentLength = comment.length();
            }
        }
        if (commentLength > 65535) {
            throw new ZipException("comment length exceeds maximum length");
        }
        zipModel.getEndCentralDirRecord().setComment(encodedComment);
        zipModel.getEndCentralDirRecord().setCommentBytes(commentBytes);
        zipModel.getEndCentralDirRecord().setCommentLength(commentLength);
        SplitOutputStream outputStream = null;
        try {
            HeaderWriter headerWriter = new HeaderWriter();
            outputStream = new SplitOutputStream(zipModel.getZipFile());
            if (zipModel.isZip64Format()) {
                outputStream.seek(zipModel.getZip64EndCentralDirRecord().getOffsetStartCenDirWRTStartDiskNo());
            } else {
                outputStream.seek(zipModel.getEndCentralDirRecord().getOffsetOfStartOfCentralDir());
            }
            headerWriter.finalizeZipFileWithoutValidations(zipModel, outputStream);
        } catch (FileNotFoundException e) {
            throw new ZipException(e);
        } catch (IOException e) {
            throw new ZipException(e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException iOException) {}
            }
        }
    }

    public void initProgressMonitorForRemoveOp(ZipModel zipModel, FileHeader fileHeader, ProgressMonitor progressMonitor) throws ZipException {
        if (zipModel == null || fileHeader == null || progressMonitor == null) {
            throw new ZipException("one of the input parameters is null, cannot calculate total work");
        }
        progressMonitor.setCurrentOperation(2);
        progressMonitor.setFileName(fileHeader.getFileName());
        progressMonitor.setTotalWork(this.calculateTotalWorkForRemoveOp(zipModel, fileHeader));
        progressMonitor.setState(1);
    }

    private long calculateTotalWorkForRemoveOp(ZipModel zipModel, FileHeader fileHeader) throws ZipException {
        return Zip4jUtil.getFileLengh(new File(zipModel.getZipFile())) - fileHeader.getCompressedSize();
    }

    public void initProgressMonitorForMergeOp(ZipModel zipModel, ProgressMonitor progressMonitor) throws ZipException {
        if (zipModel == null) {
            throw new ZipException("zip model is null, cannot calculate total work for merge op");
        }
        progressMonitor.setCurrentOperation(4);
        progressMonitor.setFileName(zipModel.getZipFile());
        progressMonitor.setTotalWork(this.calculateTotalWorkForMergeOp(zipModel));
        progressMonitor.setState(1);
    }

    private long calculateTotalWorkForMergeOp(ZipModel zipModel) throws ZipException {
        long totSize = 0L;
        if (zipModel.isSplitArchive()) {
            int totNoOfSplitFiles = zipModel.getEndCentralDirRecord().getNoOfThisDisk();
            String partFile = null;
            String curZipFile = zipModel.getZipFile();
            int partNumber = 0;
            for (int i = 0; i <= totNoOfSplitFiles; ++i) {
                partFile = partNumber == zipModel.getEndCentralDirRecord().getNoOfThisDisk() ? zipModel.getZipFile() : (partNumber >= 9 ? curZipFile.substring(0, curZipFile.lastIndexOf(".")) + ".z" + (partNumber + 1) : curZipFile.substring(0, curZipFile.lastIndexOf(".")) + ".z0" + (partNumber + 1));
                totSize += Zip4jUtil.getFileLengh(new File(partFile));
            }
        }
        return totSize;
    }
}

