/*
 * Decompiled with CFR 0.153-SNAPSHOT (3d1d0f4).
 */
package net.lingala.zip4j.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import net.lingala.zip4j.core.HeaderReader;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.UnzipParameters;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.unzip.Unzip;
import net.lingala.zip4j.util.ArchiveMaintainer;
import net.lingala.zip4j.util.InternalZipConstants;
import net.lingala.zip4j.util.Zip4jUtil;
import net.lingala.zip4j.zip.ZipEngine;

public class ZipFile {
    private String file;
    private int mode;
    private ZipModel zipModel;
    private boolean isEncrypted;
    private ProgressMonitor progressMonitor;
    private boolean runInThread;
    private String fileNameCharset;

    public ZipFile(String zipFile) throws ZipException {
        this(new File(zipFile));
    }

    public ZipFile(File zipFile) throws ZipException {
        if (zipFile == null) {
            throw new ZipException("Input zip file parameter is not null", 1);
        }
        this.file = zipFile.getPath();
        this.mode = 2;
        this.progressMonitor = new ProgressMonitor();
        this.runInThread = false;
    }

    public void createZipFile(File sourceFile, ZipParameters parameters) throws ZipException {
        ArrayList<File> sourceFileList = new ArrayList<File>();
        sourceFileList.add(sourceFile);
        this.createZipFile(sourceFileList, parameters, false, -1L);
    }

    public void createZipFile(File sourceFile, ZipParameters parameters, boolean splitArchive, long splitLength) throws ZipException {
        ArrayList<File> sourceFileList = new ArrayList<File>();
        sourceFileList.add(sourceFile);
        this.createZipFile(sourceFileList, parameters, splitArchive, splitLength);
    }

    public void createZipFile(ArrayList sourceFileList, ZipParameters parameters) throws ZipException {
        this.createZipFile(sourceFileList, parameters, false, -1L);
    }

    public void createZipFile(ArrayList sourceFileList, ZipParameters parameters, boolean splitArchive, long splitLength) throws ZipException {
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(this.file)) {
            throw new ZipException("zip file path is empty");
        }
        if (Zip4jUtil.checkFileExists(this.file)) {
            throw new ZipException("zip file: " + this.file + " already exists. To add files to existing zip file use addFile method");
        }
        if (sourceFileList == null) {
            throw new ZipException("input file ArrayList is null, cannot create zip file");
        }
        if (!Zip4jUtil.checkArrayListTypes(sourceFileList, 1)) {
            throw new ZipException("One or more elements in the input ArrayList is not of type File");
        }
        this.createNewZipModel();
        this.zipModel.setSplitArchive(splitArchive);
        this.zipModel.setSplitLength(splitLength);
        this.addFiles(sourceFileList, parameters);
    }

    public void createZipFileFromFolder(String folderToAdd, ZipParameters parameters, boolean splitArchive, long splitLength) throws ZipException {
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(folderToAdd)) {
            throw new ZipException("folderToAdd is empty or null, cannot create Zip File from folder");
        }
        this.createZipFileFromFolder(new File(folderToAdd), parameters, splitArchive, splitLength);
    }

    public void createZipFileFromFolder(File folderToAdd, ZipParameters parameters, boolean splitArchive, long splitLength) throws ZipException {
        if (folderToAdd == null) {
            throw new ZipException("folderToAdd is null, cannot create zip file from folder");
        }
        if (parameters == null) {
            throw new ZipException("input parameters are null, cannot create zip file from folder");
        }
        if (Zip4jUtil.checkFileExists(this.file)) {
            throw new ZipException("zip file: " + this.file + " already exists. To add files to existing zip file use addFolder method");
        }
        this.createNewZipModel();
        this.zipModel.setSplitArchive(splitArchive);
        if (splitArchive) {
            this.zipModel.setSplitLength(splitLength);
        }
        this.addFolder(folderToAdd, parameters, false);
    }

    public void addFile(File sourceFile, ZipParameters parameters) throws ZipException {
        ArrayList<File> sourceFileList = new ArrayList<File>();
        sourceFileList.add(sourceFile);
        this.addFiles(sourceFileList, parameters);
    }

    public void addFiles(ArrayList sourceFileList, ZipParameters parameters) throws ZipException {
        this.checkZipModel();
        if (this.zipModel == null) {
            throw new ZipException("internal error: zip model is null");
        }
        if (sourceFileList == null) {
            throw new ZipException("input file ArrayList is null, cannot add files");
        }
        if (!Zip4jUtil.checkArrayListTypes(sourceFileList, 1)) {
            throw new ZipException("One or more elements in the input ArrayList is not of type File");
        }
        if (parameters == null) {
            throw new ZipException("input parameters are null, cannot add files to zip");
        }
        if (this.progressMonitor.getState() == 1) {
            throw new ZipException("invalid operation - Zip4j is in busy state");
        }
        if (Zip4jUtil.checkFileExists(this.file) && this.zipModel.isSplitArchive()) {
            throw new ZipException("Zip file already exists. Zip file format does not allow updating split/spanned files");
        }
        ZipEngine zipEngine = new ZipEngine(this.zipModel);
        zipEngine.addFiles(sourceFileList, parameters, this.progressMonitor, this.runInThread);
    }

    public void addFolder(String path, ZipParameters parameters) throws ZipException {
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(path)) {
            throw new ZipException("input path is null or empty, cannot add folder to zip file");
        }
        this.addFolder(new File(path), parameters);
    }

    public void addFolder(File path, ZipParameters parameters) throws ZipException {
        if (path == null) {
            throw new ZipException("input path is null, cannot add folder to zip file");
        }
        if (parameters == null) {
            throw new ZipException("input parameters are null, cannot add folder to zip file");
        }
        this.addFolder(path, parameters, true);
    }

    private void addFolder(File path, ZipParameters parameters, boolean checkSplitArchive) throws ZipException {
        this.checkZipModel();
        if (this.zipModel == null) {
            throw new ZipException("internal error: zip model is null");
        }
        if (checkSplitArchive && this.zipModel.isSplitArchive()) {
            throw new ZipException("This is a split archive. Zip file format does not allow updating split/spanned files");
        }
        ZipEngine zipEngine = new ZipEngine(this.zipModel);
        zipEngine.addFolderToZip(path, parameters, this.progressMonitor, this.runInThread);
    }

    public void addStream(InputStream inputStream, ZipParameters parameters) throws ZipException {
        if (inputStream == null) {
            throw new ZipException("inputstream is null, cannot add file to zip");
        }
        if (parameters == null) {
            throw new ZipException("zip parameters are null");
        }
        this.setRunInThread(false);
        this.checkZipModel();
        if (this.zipModel == null) {
            throw new ZipException("internal error: zip model is null");
        }
        if (Zip4jUtil.checkFileExists(this.file) && this.zipModel.isSplitArchive()) {
            throw new ZipException("Zip file already exists. Zip file format does not allow updating split/spanned files");
        }
        ZipEngine zipEngine = new ZipEngine(this.zipModel);
        zipEngine.addStreamToZip(inputStream, parameters);
    }

    private void readZipInfo() throws ZipException {
        if (!Zip4jUtil.checkFileExists(this.file)) {
            throw new ZipException("zip file does not exist");
        }
        if (!Zip4jUtil.checkFileReadAccess(this.file)) {
            throw new ZipException("no read access for the input zip file");
        }
        if (this.mode != 2) {
            throw new ZipException("Invalid mode");
        }
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(new File(this.file), "r");
            if (this.zipModel == null) {
                HeaderReader headerReader = new HeaderReader(raf);
                this.zipModel = headerReader.readAllHeaders(this.fileNameCharset);
                if (this.zipModel != null) {
                    this.zipModel.setZipFile(this.file);
                }
            }
        } catch (FileNotFoundException e) {
            throw new ZipException(e);
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {}
            }
        }
    }

    public void extractAll(String destPath) throws ZipException {
        this.extractAll(destPath, null);
    }

    public void extractAll(String destPath, UnzipParameters unzipParameters) throws ZipException {
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(destPath)) {
            throw new ZipException("output path is null or invalid");
        }
        if (!Zip4jUtil.checkOutputFolder(destPath)) {
            throw new ZipException("invalid output path");
        }
        if (this.zipModel == null) {
            this.readZipInfo();
        }
        if (this.zipModel == null) {
            throw new ZipException("Internal error occurred when extracting zip file");
        }
        if (this.progressMonitor.getState() == 1) {
            throw new ZipException("invalid operation - Zip4j is in busy state");
        }
        Unzip unzip = new Unzip(this.zipModel);
        unzip.extractAll(unzipParameters, destPath, this.progressMonitor, this.runInThread);
    }

    public void extractFile(FileHeader fileHeader, String destPath) throws ZipException {
        this.extractFile(fileHeader, destPath, null);
    }

    public void extractFile(FileHeader fileHeader, String destPath, UnzipParameters unzipParameters) throws ZipException {
        this.extractFile(fileHeader, destPath, unzipParameters, null);
    }

    public void extractFile(FileHeader fileHeader, String destPath, UnzipParameters unzipParameters, String newFileName) throws ZipException {
        if (fileHeader == null) {
            throw new ZipException("input file header is null, cannot extract file");
        }
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(destPath)) {
            throw new ZipException("destination path is empty or null, cannot extract file");
        }
        this.readZipInfo();
        if (this.progressMonitor.getState() == 1) {
            throw new ZipException("invalid operation - Zip4j is in busy state");
        }
        fileHeader.extractFile(this.zipModel, destPath, unzipParameters, newFileName, this.progressMonitor, this.runInThread);
    }

    public void extractFile(String fileName, String destPath) throws ZipException {
        this.extractFile(fileName, destPath, null);
    }

    public void extractFile(String fileName, String destPath, UnzipParameters unzipParameters) throws ZipException {
        this.extractFile(fileName, destPath, unzipParameters, null);
    }

    public void extractFile(String fileName, String destPath, UnzipParameters unzipParameters, String newFileName) throws ZipException {
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(fileName)) {
            throw new ZipException("file to extract is null or empty, cannot extract file");
        }
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(destPath)) {
            throw new ZipException("destination string path is empty or null, cannot extract file");
        }
        this.readZipInfo();
        FileHeader fileHeader = Zip4jUtil.getFileHeader(this.zipModel, fileName);
        if (fileHeader == null) {
            throw new ZipException("file header not found for given file name, cannot extract file");
        }
        if (this.progressMonitor.getState() == 1) {
            throw new ZipException("invalid operation - Zip4j is in busy state");
        }
        fileHeader.extractFile(this.zipModel, destPath, unzipParameters, newFileName, this.progressMonitor, this.runInThread);
    }

    public void setPassword(String password) throws ZipException {
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(password)) {
            throw new NullPointerException();
        }
        this.setPassword(password.toCharArray());
    }

    public void setPassword(char[] password) throws ZipException {
        if (this.zipModel == null) {
            this.readZipInfo();
            if (this.zipModel == null) {
                throw new ZipException("Zip Model is null");
            }
        }
        if (this.zipModel.getCentralDirectory() == null || this.zipModel.getCentralDirectory().getFileHeaders() == null) {
            throw new ZipException("invalid zip file");
        }
        for (int i = 0; i < this.zipModel.getCentralDirectory().getFileHeaders().size(); ++i) {
            if (this.zipModel.getCentralDirectory().getFileHeaders().get(i) == null || !((FileHeader)this.zipModel.getCentralDirectory().getFileHeaders().get(i)).isEncrypted()) continue;
            ((FileHeader)this.zipModel.getCentralDirectory().getFileHeaders().get(i)).setPassword(password);
        }
    }

    public List getFileHeaders() throws ZipException {
        this.readZipInfo();
        if (this.zipModel == null || this.zipModel.getCentralDirectory() == null) {
            return null;
        }
        return this.zipModel.getCentralDirectory().getFileHeaders();
    }

    public FileHeader getFileHeader(String fileName) throws ZipException {
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(fileName)) {
            throw new ZipException("input file name is emtpy or null, cannot get FileHeader");
        }
        this.readZipInfo();
        if (this.zipModel == null || this.zipModel.getCentralDirectory() == null) {
            return null;
        }
        return Zip4jUtil.getFileHeader(this.zipModel, fileName);
    }

    public boolean isEncrypted() throws ZipException {
        if (this.zipModel == null) {
            this.readZipInfo();
            if (this.zipModel == null) {
                throw new ZipException("Zip Model is null");
            }
        }
        if (this.zipModel.getCentralDirectory() == null || this.zipModel.getCentralDirectory().getFileHeaders() == null) {
            throw new ZipException("invalid zip file");
        }
        ArrayList fileHeaderList = this.zipModel.getCentralDirectory().getFileHeaders();
        for (int i = 0; i < fileHeaderList.size(); ++i) {
            FileHeader fileHeader = (FileHeader)fileHeaderList.get(i);
            if (fileHeader == null || !fileHeader.isEncrypted()) continue;
            this.isEncrypted = true;
            break;
        }
        return this.isEncrypted;
    }

    public boolean isSplitArchive() throws ZipException {
        if (this.zipModel == null) {
            this.readZipInfo();
            if (this.zipModel == null) {
                throw new ZipException("Zip Model is null");
            }
        }
        return this.zipModel.isSplitArchive();
    }

    public void removeFile(String fileName) throws ZipException {
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(fileName)) {
            throw new ZipException("file name is empty or null, cannot remove file");
        }
        if (this.zipModel == null && Zip4jUtil.checkFileExists(this.file)) {
            this.readZipInfo();
        }
        if (this.zipModel.isSplitArchive()) {
            throw new ZipException("Zip file format does not allow updating split/spanned files");
        }
        FileHeader fileHeader = Zip4jUtil.getFileHeader(this.zipModel, fileName);
        if (fileHeader == null) {
            throw new ZipException("could not find file header for file: " + fileName);
        }
        this.removeFile(fileHeader);
    }

    public void removeFile(FileHeader fileHeader) throws ZipException {
        if (fileHeader == null) {
            throw new ZipException("file header is null, cannot remove file");
        }
        if (this.zipModel == null && Zip4jUtil.checkFileExists(this.file)) {
            this.readZipInfo();
        }
        if (this.zipModel.isSplitArchive()) {
            throw new ZipException("Zip file format does not allow updating split/spanned files");
        }
        ArchiveMaintainer archiveMaintainer = new ArchiveMaintainer();
        archiveMaintainer.initProgressMonitorForRemoveOp(this.zipModel, fileHeader, this.progressMonitor);
        archiveMaintainer.removeZipFile(this.zipModel, fileHeader, this.progressMonitor, this.runInThread);
    }

    public void mergeSplitFiles(File outputZipFile) throws ZipException {
        if (outputZipFile == null) {
            throw new ZipException("outputZipFile is null, cannot merge split files");
        }
        if (outputZipFile.exists()) {
            throw new ZipException("output Zip File already exists");
        }
        this.checkZipModel();
        if (this.zipModel == null) {
            throw new ZipException("zip model is null, corrupt zip file?");
        }
        ArchiveMaintainer archiveMaintainer = new ArchiveMaintainer();
        archiveMaintainer.initProgressMonitorForMergeOp(this.zipModel, this.progressMonitor);
        archiveMaintainer.mergeSplitZipFiles(this.zipModel, outputZipFile, this.progressMonitor, this.runInThread);
    }

    public void setComment(String comment) throws ZipException {
        if (comment == null) {
            throw new ZipException("input comment is null, cannot update zip file");
        }
        if (!Zip4jUtil.checkFileExists(this.file)) {
            throw new ZipException("zip file does not exist, cannot set comment for zip file");
        }
        this.readZipInfo();
        if (this.zipModel == null) {
            throw new ZipException("zipModel is null, cannot update zip file");
        }
        if (this.zipModel.getEndCentralDirRecord() == null) {
            throw new ZipException("end of central directory is null, cannot set comment");
        }
        ArchiveMaintainer archiveMaintainer = new ArchiveMaintainer();
        archiveMaintainer.setComment(this.zipModel, comment);
    }

    public String getComment() throws ZipException {
        return this.getComment(null);
    }

    public String getComment(String encoding) throws ZipException {
        if (encoding == null) {
            encoding = Zip4jUtil.isSupportedCharset("windows-1254") ? "windows-1254" : InternalZipConstants.CHARSET_DEFAULT;
        }
        if (!Zip4jUtil.checkFileExists(this.file)) {
            throw new ZipException("zip file does not exist, cannot read comment");
        }
        this.checkZipModel();
        if (this.zipModel == null) {
            throw new ZipException("zip model is null, cannot read comment");
        }
        if (this.zipModel.getEndCentralDirRecord() == null) {
            throw new ZipException("end of central directory record is null, cannot read comment");
        }
        if (this.zipModel.getEndCentralDirRecord().getCommentBytes() == null || this.zipModel.getEndCentralDirRecord().getCommentBytes().length <= 0) {
            return null;
        }
        try {
            return new String(this.zipModel.getEndCentralDirRecord().getCommentBytes(), encoding);
        } catch (UnsupportedEncodingException e) {
            throw new ZipException(e);
        }
    }

    private void checkZipModel() throws ZipException {
        if (this.zipModel == null) {
            if (Zip4jUtil.checkFileExists(this.file)) {
                this.readZipInfo();
            } else {
                this.createNewZipModel();
            }
        }
    }

    private void createNewZipModel() {
        this.zipModel = new ZipModel();
        this.zipModel.setZipFile(this.file);
        this.zipModel.setFileNameCharset(this.fileNameCharset);
    }

    public void setFileNameCharset(String charsetName) throws ZipException {
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(charsetName)) {
            throw new ZipException("null or empty charset name");
        }
        if (!Zip4jUtil.isSupportedCharset(charsetName)) {
            throw new ZipException("unsupported charset: " + charsetName);
        }
        this.fileNameCharset = charsetName;
    }

    public ZipInputStream getInputStream(FileHeader fileHeader) throws ZipException {
        if (fileHeader == null) {
            throw new ZipException("FileHeader is null, cannot get InputStream");
        }
        this.checkZipModel();
        if (this.zipModel == null) {
            throw new ZipException("zip model is null, cannot get inputstream");
        }
        Unzip unzip = new Unzip(this.zipModel);
        return unzip.getInputStream(fileHeader);
    }

    public boolean isValidZipFile() {
        try {
            this.readZipInfo();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public ArrayList getSplitZipFiles() throws ZipException {
        this.checkZipModel();
        return Zip4jUtil.getSplitZipFiles(this.zipModel);
    }

    public ProgressMonitor getProgressMonitor() {
        return this.progressMonitor;
    }

    public boolean isRunInThread() {
        return this.runInThread;
    }

    public void setRunInThread(boolean runInThread) {
        this.runInThread = runInThread;
    }

    public File getFile() {
        return new File(this.file);
    }
}

