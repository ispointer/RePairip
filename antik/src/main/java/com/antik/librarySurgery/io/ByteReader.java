package com.antik.librarySurgery.io;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class ByteReader {

    private final byte[] buffer;
    private final ByteBuffer byteBuffer;

    public ByteReader(byte[] buffer) {
        this.buffer = buffer;
        this.byteBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public int length() {
        return buffer.length;
    }

    public int readUInt16(int offset) {
        if (offset < 0 || offset + 2 > buffer.length) return 0;
        return byteBuffer.getShort(offset) & 0xFFFF;
    }

    public long readUInt32(int offset) {
        if (offset < 0 || offset + 4 > buffer.length) return 0L;
        return byteBuffer.getInt(offset) & 0xFFFFFFFFL;
    }

    public long readUInt64(int offset) {
        if (offset < 0 || offset + 8 > buffer.length) return 0L;
        return byteBuffer.getLong(offset);
    }

    public String readNullTerminatedString(int offset) {
        if (offset < 0 || offset >= buffer.length) return "";
        int end = offset;
        while (end < buffer.length && buffer[end] != 0) {
            end++;
        }
        return new String(buffer, offset, end - offset, StandardCharsets.US_ASCII);
    }

    public int indexOf(byte[] target) {
        if (target == null || target.length == 0) return -1;
        outer:
        for (int i = 0; i <= buffer.length - target.length; i++) {
            for (int j = 0; j < target.length; j++) {
                if (buffer[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    public boolean checkMagic(byte[] magic) {
        if (magic == null || buffer.length < magic.length) return false;
        for (int i = 0; i < magic.length; i++) {
            if (buffer[i] != magic[i]) return false;
        }
        return true;
    }
}
