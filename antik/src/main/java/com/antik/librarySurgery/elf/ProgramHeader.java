package com.antik.librarySurgery.elf;

import com.antik.librarySurgery.io.ByteReader;

public class ProgramHeader {

    private final int headerOffset;
    private final int pType;
    private final long pOffset;
    private final long pVaddr;
    private final long pFileSz;
    private final long pMemSz;

    public ProgramHeader(ByteReader reader, int headerOffset) {
        this.headerOffset = headerOffset;
        this.pType = (int) reader.readUInt32(headerOffset);
        this.pOffset = reader.readUInt64(headerOffset + 8);
        this.pVaddr = reader.readUInt64(headerOffset + 16);
        this.pFileSz = reader.readUInt64(headerOffset + 32);
        this.pMemSz = reader.readUInt64(headerOffset + 40);
    }

    public int getHeaderOffset() {
        return headerOffset;
    }

    public int getPType() {
        return pType;
    }

    public long getPOffset() {
        return pOffset;
    }

    public long getPVaddr() {
        return pVaddr;
    }

    public long getPFileSz() {
        return pFileSz;
    }

    public long getPMemSz() {
        return pMemSz;
    }
}
