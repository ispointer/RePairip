package com.antik.librarySurgery.io;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ByteWriter {

    private final byte[] buffer;
    private final ByteBuffer byteBuffer;

    public ByteWriter(byte[] buffer) {
        this.buffer = buffer;
        this.byteBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public void writeUInt16(int offset, int value) {
        if (offset >= 0 && offset + 2 <= buffer.length) {
            byteBuffer.putShort(offset, (short) value);
        }
    }

    public void writeUInt32(int offset, long value) {
        if (offset >= 0 && offset + 4 <= buffer.length) {
            byteBuffer.putInt(offset, (int) value);
        }
    }

    public void writeUInt64(int offset, long value) {
        if (offset >= 0 && offset + 8 <= buffer.length) {
            byteBuffer.putLong(offset, value);
        }
    }

    public void writeBytes(int offset, byte[] data) {
        if (data != null && offset >= 0 && offset + data.length <= buffer.length) {
            System.arraycopy(data, 0, buffer, offset, data.length);
        }
    }

    public void zeroRange(int startOffset, int endOffset) {
        if (startOffset >= 0 && endOffset <= buffer.length && startOffset < endOffset) {
            for (int i = startOffset; i < endOffset; i++) {
                buffer[i] = 0;
            }
        }
    }
}
