package com.antik.librarySurgery.elf;

import com.antik.librarySurgery.io.ByteReader;

public class ElfHeader {

    private final long phOff;
    private final int phNum;
    private final boolean valid;

    public ElfHeader(ByteReader reader) {
        if (!reader.checkMagic(ElfConstants.ELF_MAGIC)) {
            this.valid = false;
            this.phOff = 0L;
            this.phNum = 0;
            return;
        }
        this.phOff = reader.readUInt64(0x20);
        this.phNum = reader.readUInt16(0x38);
        this.valid = true;
    }

    public boolean isValid() {
        return valid;
    }

    public long getPhOff() {
        return phOff;
    }

    public int getPhNum() {
        return phNum;
    }
}
