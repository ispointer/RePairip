package com.antik.librarySurgery.elf;

public class ElfConstants {

    public static final byte[] ELF_MAGIC = new byte[]{(byte) 0x7f, 'E', 'L', 'F'};

    public static final int PT_NULL = 0;
    public static final int PT_LOAD = 1;
    public static final int PT_DYNAMIC = 2;

    public static final long DT_NULL = 0L;
    public static final long DT_NEEDED = 1L;
    public static final long DT_STRTAB = 5L;
    public static final long DT_SYMTAB = 6L;
    public static final long DT_INIT = 12L;
    public static final long DT_INIT_ARRAY = 25L;
    public static final long DT_INIT_ARRAYSZ = 27L;

    public static final byte[] OLD_EXECUTE_PROGRAM = "ExecuteProgram\0".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    public static final byte[] NEW_MEMSET = "memset\0\0\0\0\0\0\0\0\0".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

    public static final byte[] ARM64_STUB_BYTES = new byte[]{
            (byte) 0xc0, 0x00, (byte) 0x80, 0x52, // mov w0, #0
            0x20, 0x00, (byte) 0xa0, 0x72,        // movk w0, #0x10, lsl #16 (JNI_VERSION_1_6 = 0x00010000)
            (byte) 0xc0, 0x03, 0x5f, (byte) 0xd6  // ret
    };
}
