package com.antik.librarySurgery.elf;

import com.antik.librarySurgery.io.ByteReader;

import java.util.ArrayList;
import java.util.List;

public class Elf {

    private final ByteReader reader;
    private final ElfHeader header;
    private final List<ProgramHeader> programHeaders = new ArrayList<>();
    private ProgramHeader dynamicHeader = null;
    private Long strtabVa = null;
    private Long strtabOff = null;
    private Long symtabVa = null;
    private Long symtabOff = null;

    public Elf(byte[] data) {
        this.reader = new ByteReader(data);
        this.header = new ElfHeader(reader);

        if (header.isValid()) {
            parseProgramHeaders();
            parseDynamicSection();
        }
    }

    private void parseProgramHeaders() {
        long phOff = header.getPhOff();
        int phNum = header.getPhNum();
        for (int i = 0; i < phNum; i++) {
            int offset = (int) (phOff + i * 56L);
            if (offset + 56 > reader.length()) break;
            ProgramHeader ph = new ProgramHeader(reader, offset);
            programHeaders.add(ph);
            if (ph.getPType() == ElfConstants.PT_DYNAMIC) {
                this.dynamicHeader = ph;
            }
        }
    }

    private void parseDynamicSection() {
        if (dynamicHeader == null) return;
        int dynOff = (int) dynamicHeader.getPOffset();
        int dynSz = (int) dynamicHeader.getPFileSz();

        for (int j = 0; j < dynSz; j += 16) {
            int pos = dynOff + j;
            if (pos + 16 > reader.length()) break;
            long tag = reader.readUInt64(pos);
            long val = reader.readUInt64(pos + 8);
            if (tag == ElfConstants.DT_STRTAB) {
                this.strtabVa = val;
            } else if (tag == ElfConstants.DT_SYMTAB) {
                this.symtabVa = val;
            }
        }

        if (strtabVa != null) {
            this.strtabOff = vaddrToOffset(strtabVa);
        }
        if (symtabVa != null) {
            this.symtabOff = vaddrToOffset(symtabVa);
        }
    }

    public Long vaddrToOffset(long vaddr) {
        for (ProgramHeader ph : programHeaders) {
            if (ph.getPType() == ElfConstants.PT_LOAD && ph.getPVaddr() <= vaddr && vaddr < ph.getPVaddr() + ph.getPMemSz()) {
                return ph.getPOffset() + (vaddr - ph.getPVaddr());
            }
        }
        return null;
    }

    public Long findSymbolAddress(String symbolName) {
        if (symtabOff == null || strtabOff == null) return null;
        int symOff = symtabOff.intValue();
        int maxSyms = 3000;
        for (int i = 0; i < maxSyms; i++) {
            int entryPos = symOff + i * 24;
            if (entryPos + 24 > reader.length()) break;
            long nameIdx = reader.readUInt32(entryPos);
            long value = reader.readUInt64(entryPos + 8);
            if (nameIdx > 0) {
                String name = reader.readNullTerminatedString((int) (strtabOff + nameIdx));
                if (symbolName.equals(name)) {
                    return vaddrToOffset(value);
                }
            }
        }
        return null;
    }

    public ByteReader getReader() {
        return reader;
    }

    public ElfHeader getHeader() {
        return header;
    }

    public List<ProgramHeader> getProgramHeaders() {
        return programHeaders;
    }

    public ProgramHeader getDynamicHeader() {
        return dynamicHeader;
    }

    public Long getStrtabOff() {
        return strtabOff;
    }

    public String readStringFromStrtab(long offsetVal) {
        if (strtabOff == null) return "";
        return reader.readNullTerminatedString((int) (strtabOff + offsetVal));
    }
}
