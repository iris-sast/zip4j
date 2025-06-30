/*
 * Decompiled with CFR 0.153-SNAPSHOT (3d1d0f4).
 */
package net.lingala.zip4j.model;

import java.util.List;
import net.lingala.zip4j.model.ArchiveExtraDataRecord;
import net.lingala.zip4j.model.CentralDirectory;
import net.lingala.zip4j.model.EndCentralDirRecord;
import net.lingala.zip4j.model.Zip64EndCentralDirLocator;
import net.lingala.zip4j.model.Zip64EndCentralDirRecord;

public class ZipModel
implements Cloneable {
    private List localFileHeaderList;
    private List dataDescriptorList;
    private ArchiveExtraDataRecord archiveExtraDataRecord;
    private CentralDirectory centralDirectory;
    private EndCentralDirRecord endCentralDirRecord;
    private Zip64EndCentralDirLocator zip64EndCentralDirLocator;
    private Zip64EndCentralDirRecord zip64EndCentralDirRecord;
    private boolean splitArchive;
    private long splitLength = -1L;
    private String zipFile;
    private boolean isZip64Format;
    private boolean isNestedZipFile;
    private long start;
    private long end;
    private String fileNameCharset;

    public List getLocalFileHeaderList() {
        return this.localFileHeaderList;
    }

    public void setLocalFileHeaderList(List localFileHeaderList) {
        this.localFileHeaderList = localFileHeaderList;
    }

    public List getDataDescriptorList() {
        return this.dataDescriptorList;
    }

    public void setDataDescriptorList(List dataDescriptorList) {
        this.dataDescriptorList = dataDescriptorList;
    }

    public CentralDirectory getCentralDirectory() {
        return this.centralDirectory;
    }

    public void setCentralDirectory(CentralDirectory centralDirectory) {
        this.centralDirectory = centralDirectory;
    }

    public EndCentralDirRecord getEndCentralDirRecord() {
        return this.endCentralDirRecord;
    }

    public void setEndCentralDirRecord(EndCentralDirRecord endCentralDirRecord) {
        this.endCentralDirRecord = endCentralDirRecord;
    }

    public ArchiveExtraDataRecord getArchiveExtraDataRecord() {
        return this.archiveExtraDataRecord;
    }

    public void setArchiveExtraDataRecord(ArchiveExtraDataRecord archiveExtraDataRecord) {
        this.archiveExtraDataRecord = archiveExtraDataRecord;
    }

    public boolean isSplitArchive() {
        return this.splitArchive;
    }

    public void setSplitArchive(boolean splitArchive) {
        this.splitArchive = splitArchive;
    }

    public String getZipFile() {
        return this.zipFile;
    }

    public void setZipFile(String zipFile) {
        this.zipFile = zipFile;
    }

    public Zip64EndCentralDirLocator getZip64EndCentralDirLocator() {
        return this.zip64EndCentralDirLocator;
    }

    public void setZip64EndCentralDirLocator(Zip64EndCentralDirLocator zip64EndCentralDirLocator) {
        this.zip64EndCentralDirLocator = zip64EndCentralDirLocator;
    }

    public Zip64EndCentralDirRecord getZip64EndCentralDirRecord() {
        return this.zip64EndCentralDirRecord;
    }

    public void setZip64EndCentralDirRecord(Zip64EndCentralDirRecord zip64EndCentralDirRecord) {
        this.zip64EndCentralDirRecord = zip64EndCentralDirRecord;
    }

    public boolean isZip64Format() {
        return this.isZip64Format;
    }

    public void setZip64Format(boolean isZip64Format) {
        this.isZip64Format = isZip64Format;
    }

    public boolean isNestedZipFile() {
        return this.isNestedZipFile;
    }

    public void setNestedZipFile(boolean isNestedZipFile) {
        this.isNestedZipFile = isNestedZipFile;
    }

    public long getStart() {
        return this.start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return this.end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public long getSplitLength() {
        return this.splitLength;
    }

    public void setSplitLength(long splitLength) {
        this.splitLength = splitLength;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public String getFileNameCharset() {
        return this.fileNameCharset;
    }

    public void setFileNameCharset(String fileNameCharset) {
        this.fileNameCharset = fileNameCharset;
    }
}

