package com.antik.librarySurgery;

import com.antik.librarySurgery.elf.DynamicEntry;
import com.antik.librarySurgery.elf.Elf;
import com.antik.librarySurgery.elf.ElfConstants;
import com.antik.librarySurgery.elf.ProgramHeader;
import com.antik.librarySurgery.io.ByteReader;
import com.antik.librarySurgery.io.ByteWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LibraryPatcher {

    public static PatchResult patchSingleLib(byte[] data, byte[] memDump) {
        Elf elf = new Elf(data);
        if (!elf.getHeader().isValid()) {
            return new PatchResult(false, "Invalid ELF header");
        }

        ProgramHeader dynamicHeader = elf.getDynamicHeader();
        if (dynamicHeader == null) {
            return new PatchResult(false, "No PT_DYNAMIC header found");
        }

        ByteReader reader = elf.getReader();
        ByteWriter writer = new ByteWriter(data);

        int dynOff = (int) dynamicHeader.getPOffset();
        int dynSz = (int) dynamicHeader.getPFileSz();

        Long initVa = null;
        Long initArrayVa = null;
        Long initArraySz = null;
        List<DynamicEntry> newEntries = new ArrayList<>();
        boolean hasPairip = false;

        for (int j = 0; j < dynSz; j += 16) {
            int entryPos = dynOff + j;
            if (entryPos + 16 > reader.length()) break;
            long tag = reader.readUInt64(entryPos);
            long val = reader.readUInt64(entryPos + 8);

            if (tag == ElfConstants.DT_NULL) {
                break;
            }
            if (tag == ElfConstants.DT_INIT) {
                initVa = val;
                continue;
            }
            if (tag == ElfConstants.DT_INIT_ARRAY) {
                initArrayVa = val;
                continue;
            }
            if (tag == ElfConstants.DT_INIT_ARRAYSZ) {
                initArraySz = val;
                continue;
            }
            if (tag == ElfConstants.DT_NEEDED) {
                String name = elf.readStringFromStrtab(val);
                if (name.toLowerCase().contains("pairip")) {
                    hasPairip = true;
                    continue;
                }
            }
            newEntries.add(new DynamicEntry(tag, val));
        }

        int posStr = reader.indexOf(ElfConstants.OLD_EXECUTE_PROGRAM);
        boolean isPairipProtected = hasPairip || posStr != -1;

        if (!isPairipProtected) {
            return new PatchResult(false, "Not protected by PairIP");
        }

        newEntries.add(new DynamicEntry(ElfConstants.DT_NULL, 0L));

        for (int k = 0; k < newEntries.size(); k++) {
            int pos = dynOff + k * 16;
            if (pos + 16 <= reader.length()) {
                writer.writeUInt64(pos, newEntries.get(k).getTag());
                writer.writeUInt64(pos + 8, newEntries.get(k).getVal());
            }
        }

        writer.zeroRange(dynOff + newEntries.size() * 16, dynOff + dynSz);

        if (initVa != null) {
            for (ProgramHeader ph : elf.getProgramHeaders()) {
                if (ph.getPType() == ElfConstants.PT_LOAD && ph.getPVaddr() <= initVa && initVa < ph.getPVaddr() + ph.getPMemSz()) {
                    writer.writeUInt32(ph.getHeaderOffset(), ElfConstants.PT_NULL);
                }
            }
        }

        if (initArrayVa != null && initArraySz != null) {
            Long initArrayOff = elf.vaddrToOffset(initArrayVa);
            if (initArrayOff != null) {
                int arrayPos = initArrayOff.intValue();
                int arraySize = initArraySz.intValue();
                if (arrayPos >= 0 && arrayPos + arraySize <= reader.length()) {
                    writer.zeroRange(arrayPos, arrayPos + arraySize);
                }
            }
        }

        if (posStr != -1) {
            writer.writeBytes(posStr, ElfConstants.NEW_MEMSET);
        }

        Long jniOnLoadOffset = elf.findSymbolAddress("JNI_OnLoad");
        if (jniOnLoadOffset != null) {
            int stubPos = jniOnLoadOffset.intValue();
            if (stubPos >= 0 && stubPos + ElfConstants.ARM64_STUB_BYTES.length <= reader.length()) {
                writer.writeBytes(stubPos, ElfConstants.ARM64_STUB_BYTES);
            }
        } else {
            System.out.println("[INFO] JNI_OnLoad symbol not found in ELF, skipping stub patch safely.");
        }

        if (memDump != null && memDump.length > 0) {
            int copyLen = Math.min(reader.length(), memDump.length);
            writer.writeBytes(0, Arrays.copyOf(memDump, copyLen));
        }

        return new PatchResult(true, "Successfully patched");
    }
}
