package com.antik.librarySurgery.elf;

public class DynamicEntry {

    private final long tag;
    private final long val;

    public DynamicEntry(long tag, long val) {
        this.tag = tag;
        this.val = val;
    }

    public long getTag() {
        return tag;
    }

    public long getVal() {
        return val;
    }
}
