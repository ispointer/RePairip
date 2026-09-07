package com.antik.librarySurgery.memory;

import com.antik.librarySurgery.shell.Shell;
import com.antik.librarySurgery.shell.ShellResult;

import java.util.ArrayList;
import java.util.List;

public class MemoryMap {

    private final List<MemoryRegion<Long>> regions = new ArrayList<>();

    public static MemoryMap parseFromProcess(String pid) {
        MemoryMap map = new MemoryMap();
        ShellResult result = Shell.runRootCmd("grep 'r-xp' /proc/" + pid + "/maps");
        if (!result.getStdout().isEmpty()) {
            for (String line : result.getStdout().split("\r?\n")) {
                String trimmed = line.trim();
                String[] parts = trimmed.split("\\s+");
                if (parts.length >= 1) {
                    String[] addrs = parts[0].split("-");
                    if (addrs.length == 2) {
                        try {
                            long start = Long.parseUnsignedLong(addrs[0], 16);
                            long end = Long.parseUnsignedLong(addrs[1], 16);
                            String perms = parts.length > 1 ? parts[1] : "";
                            String path = parts.length > 5 ? parts[5] : "";
                            map.regions.add(new MemoryRegion<>(start, end, perms, path));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }
        return map;
    }

    public List<MemoryRegion<Long>> getRegions() {
        return regions;
    }

    public MemoryRegion<Long> findRxRegion() {
        for (MemoryRegion<Long> region : regions) {
            String path = region.getPathname();
            if (region.isExecutable() && (path.contains("apk") || path.contains("lib"))) {
                return region;
            }
        }
        return null;
    }

    public String findRxAddressString() {
        MemoryRegion<Long> region = findRxRegion();
        if (region != null) {
            return Long.toHexString(region.getStartAddress());
        }
        return null;
    }
}
