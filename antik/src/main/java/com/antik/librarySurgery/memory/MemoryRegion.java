package com.antik.librarySurgery.memory;

public class MemoryRegion<T extends Number> {

    private final T startAddress;
    private final T endAddress;
    private final String permissions;
    private final String pathname;

    public MemoryRegion(T startAddress, T endAddress, String permissions, String pathname) {
        this.startAddress = startAddress;
        this.endAddress = endAddress;
        this.permissions = permissions;
        this.pathname = pathname;
    }

    public T getStartAddress() {
        return startAddress;
    }

    public T getEndAddress() {
        return endAddress;
    }

    public String getPermissions() {
        return permissions;
    }

    public String getPathname() {
        return pathname;
    }

    public boolean isExecutable() {
        return permissions != null && permissions.contains("x");
    }

    public boolean isReadable() {
        return permissions != null && permissions.contains("r");
    }
}
