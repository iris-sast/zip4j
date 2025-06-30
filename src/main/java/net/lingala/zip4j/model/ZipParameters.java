/*
 * Decompiled with CFR 0.153-SNAPSHOT (3d1d0f4).
 */
package net.lingala.zip4j.model;

import java.util.TimeZone;
import net.lingala.zip4j.util.InternalZipConstants;
import net.lingala.zip4j.util.Zip4jUtil;

public class ZipParameters
implements Cloneable {
    private int compressionMethod = 8;
    private int compressionLevel;
    private boolean encryptFiles = false;
    private int encryptionMethod = -1;
    private boolean readHiddenFiles = true;
    private char[] password;
    private int aesKeyStrength = -1;
    private boolean includeRootFolder = true;
    private String rootFolderInZip;
    private TimeZone timeZone = TimeZone.getDefault();
    private int sourceFileCRC;
    private String defaultFolderPath;
    private String fileNameInZip;
    private boolean isSourceExternalStream;

    public int getCompressionMethod() {
        return this.compressionMethod;
    }

    public void setCompressionMethod(int compressionMethod) {
        this.compressionMethod = compressionMethod;
    }

    public boolean isEncryptFiles() {
        return this.encryptFiles;
    }

    public void setEncryptFiles(boolean encryptFiles) {
        this.encryptFiles = encryptFiles;
    }

    public int getEncryptionMethod() {
        return this.encryptionMethod;
    }

    public void setEncryptionMethod(int encryptionMethod) {
        this.encryptionMethod = encryptionMethod;
    }

    public int getCompressionLevel() {
        return this.compressionLevel;
    }

    public void setCompressionLevel(int compressionLevel) {
        this.compressionLevel = compressionLevel;
    }

    public boolean isReadHiddenFiles() {
        return this.readHiddenFiles;
    }

    public void setReadHiddenFiles(boolean readHiddenFiles) {
        this.readHiddenFiles = readHiddenFiles;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public char[] getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        if (password == null) {
            return;
        }
        this.setPassword(password.toCharArray());
    }

    public void setPassword(char[] password) {
        this.password = password;
    }

    public int getAesKeyStrength() {
        return this.aesKeyStrength;
    }

    public void setAesKeyStrength(int aesKeyStrength) {
        this.aesKeyStrength = aesKeyStrength;
    }

    public boolean isIncludeRootFolder() {
        return this.includeRootFolder;
    }

    public void setIncludeRootFolder(boolean includeRootFolder) {
        this.includeRootFolder = includeRootFolder;
    }

    public String getRootFolderInZip() {
        return this.rootFolderInZip;
    }

    public void setRootFolderInZip(String rootFolderInZip) {
        if (Zip4jUtil.isStringNotNullAndNotEmpty(rootFolderInZip)) {
            if (!rootFolderInZip.endsWith("\\") && !rootFolderInZip.endsWith("/")) {
                rootFolderInZip = rootFolderInZip + InternalZipConstants.FILE_SEPARATOR;
            }
            rootFolderInZip = rootFolderInZip.replaceAll("\\\\", "/");
        }
        this.rootFolderInZip = rootFolderInZip;
    }

    public TimeZone getTimeZone() {
        return this.timeZone;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public int getSourceFileCRC() {
        return this.sourceFileCRC;
    }

    public void setSourceFileCRC(int sourceFileCRC) {
        this.sourceFileCRC = sourceFileCRC;
    }

    public String getDefaultFolderPath() {
        return this.defaultFolderPath;
    }

    public void setDefaultFolderPath(String defaultFolderPath) {
        this.defaultFolderPath = defaultFolderPath;
    }

    public String getFileNameInZip() {
        return this.fileNameInZip;
    }

    public void setFileNameInZip(String fileNameInZip) {
        this.fileNameInZip = fileNameInZip;
    }

    public boolean isSourceExternalStream() {
        return this.isSourceExternalStream;
    }

    public void setSourceExternalStream(boolean isSourceExternalStream) {
        this.isSourceExternalStream = isSourceExternalStream;
    }
}

