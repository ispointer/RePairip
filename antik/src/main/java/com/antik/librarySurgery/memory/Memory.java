package com.antik.librarySurgery.memory;

import com.antik.librarySurgery.shell.Shell;

import java.io.File;
import java.nio.file.Files;

public class Memory {

    public static MemoryRegion<Long> findRxRegion(String pid) {
        MemoryMap map = MemoryMap.parseFromProcess(pid);
        return map.findRxRegion();
    }

    public static byte[] dumpMemory(String pid, String rxAddr) {
        if (rxAddr == null || rxAddr.isEmpty()) {
            return null;
        }
        String dumpPath = "/data/local/tmp/pairip_dump.bin";
        System.out.println("[ROOT] Dumping decrypted .text memory from RAM (Address 0x" + rxAddr + ")");
        Shell.runRootCmd("dd if=/proc/" + pid + "/mem of=" + dumpPath + " bs=1024 count=576 skip=$((0x" + rxAddr + "/1024))");

        File dumpFile = new File(dumpPath);
        byte[] data = null;
        if (dumpFile.exists() && dumpFile.length() > 0) {
            try {
                data = Files.readAllBytes(dumpFile.toPath());
                System.out.println("[ROOT] Captured " + data.length + " bytes decrypted memory dump");
            } catch (Exception e) {
                System.err.println("[ERROR] Reading memory dump failed : " + e.getMessage());
            }
        }
        Shell.runRootCmd("rm -f " + dumpPath);
        return data;
    }
}
