package com.antik.DexPatcher.Translation;

import com.antik.DexPatcher.Translation.antik.PairipClass;
import com.antik.Main;
import com.reandroid.apk.ApkModule;
import com.reandroid.archive.ByteInputSource;
import com.reandroid.archive.InputSource;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.writer.io.MemoryDataStore;
import org.jf.dexlib2.writer.pool.DexPool;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TranslationPatcher {

    private static final byte[] DEX_MAGIC = new byte[]{0x64, 0x65, 0x78, 0x0A};
    private static final byte[] PAIRIP_ASSET_HEADER = new byte[]{0x00, 0x49, 0x41, 0x50, 0x02};

    public static void patch(ApkModule m, File jsonFile) throws Exception {
        System.out.println("[INFO] Loading translation file: " + jsonFile.getName());
        TranslationData data = new TranslationData(jsonFile);
        TranslationRewriter rewriter = new TranslationRewriter(data);

        List<String> dexNames = new ArrayList<>();
        for (InputSource s : m.getInputSources()) {
            if (s.getName().endsWith(".dex")) {
                dexNames.add(s.getName());
            }
        }

        Set<String> existingDexNames = new HashSet<>(dexNames);
        Set<String> existingClassTypes = new HashSet<>();
        for (String dn : dexNames) {
            InputSource s = m.getInputSource(dn);
            if (s == null) continue;
            try (InputStream input = s.openStream()) {
                DexFile dexFile = loadDexFile(readAllBytes(input), "trans_scan");
                for (ClassDef classDef : dexFile.getClasses()) {
                    existingClassTypes.add(classDef.getType());
                }
            }
        }

        List<PendingDex> extraDexes = new ArrayList<>();
        if (data.hasMethods) {
            extraDexes.addAll(collectEmbeddedAssetDexes(m, existingClassTypes));
            PendingDex restoreMethodDex = loadRestoreMethodDex(existingClassTypes);
            if (restoreMethodDex != null) {
                extraDexes.add(restoreMethodDex);
            }
        }

        for (String dn : dexNames) {
            InputSource s = m.getInputSource(dn);
            if (s == null) continue;

            byte[] d_bs;
            try (InputStream i = s.openStream()) {
                d_bs = readAllBytes(i);
            }

            DexFile d_f = loadDexFile(d_bs, "trans");

            List<ClassDef> cds = new ArrayList<>();
            boolean mod = false;

            for (ClassDef cd : d_f.getClasses()) {
                String type = cd.getType();
                if (type.startsWith("Lcom/pairip/")) {
                    if (PairipClass.contains(type)) {
                        System.out.println("[INFO] Patching " + type + " in " + dn);
                        cds.add(rewriter.rewrite(cd));
                        mod = true;
                    } else if (!type.equals("Lcom/pairip/PairipLog;") && !type.equals("Lcom/pairip/RestoreMethod;")) {
                        System.out.println("[INFO] Removing class: " + type);
                        mod = true;
                    } else {
                        cds.add(cd);
                    }
                } else {
                    cds.add(cd);
                }
            }

            if (mod) {
                MemoryDataStore ds = new MemoryDataStore();
                DexPool dp = new DexPool(Opcodes.getDefault());
                for (ClassDef c : cds) {
                    dp.internClass(c);
                }
                dp.writeTo(ds);
                byte[] r_bs = Arrays.copyOf(ds.getData(), ds.getSize());
                m.add(new ByteInputSource(r_bs, dn));
                System.out.println("[BUILD] Patched translation in " + dn);
            }
        }

        int nextDexNumber = nextDexNumber(existingDexNames);
        for (PendingDex pendingDex : extraDexes) {
            if (!pendingDex.canAddAgainst(existingClassTypes)) {
                if (!pendingDex.isFullyPresentIn(existingClassTypes)) {
                    System.err.println("[WARN] Skipping " + pendingDex.sourceName + " due to partial class overlap");
                }
                continue;
            }

            String dexEntryName = nextDexEntryName(existingDexNames, nextDexNumber);
            nextDexNumber = dexNumberOf(dexEntryName) + 1;
            m.add(new ByteInputSource(pendingDex.dexBytes, dexEntryName));
            existingDexNames.add(dexEntryName);
            existingClassTypes.addAll(pendingDex.classTypes);
            System.out.println("[INFO] Added " + dexEntryName + " from " + pendingDex.sourceName);
        }

        List<String> libFiles = new ArrayList<>();
        for (InputSource source : m.getInputSources()) {
            if (source.getName().contains("libpairipcore.so")) {
                libFiles.add(source.getName());
            }
        }
        for (String libFile : libFiles) {
            try {
                m.getZipEntryMap().remove(libFile);
                System.out.println("[INFO] Removing lib: " + libFile);
            } catch (Exception e) {
                System.err.println("[WARN] Could not remove lib: " + libFile);
            }
        }
    }

    private static byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int len;
        while ((len = input.read(buf)) != -1) {
            baos.write(buf, 0, len);
        }
        return baos.toByteArray();
    }

    private static DexFile loadDexFile(byte[] dexBytes, String prefix) throws Exception {
        File tempDex = File.createTempFile(prefix, ".dex");
        try {
            Files.write(tempDex.toPath(), dexBytes);
            return DexFileFactory.loadDexFile(tempDex, Opcodes.getDefault());
        } finally {
            tempDex.delete();
        }
    }

    private static List<PendingDex> collectEmbeddedAssetDexes(ApkModule module, Set<String> existingClassTypes) throws Exception {
        List<PendingDex> pendingDexes = new ArrayList<>();
        for (InputSource source : module.getInputSources()) {
            if (!isTopLevelAsset(source.getName())) {
                continue;
            }

            byte[] assetBytes;
            try (InputStream input = source.openStream()) {
                assetBytes = readAllBytes(input);
            }

            if (!startsWith(assetBytes, PAIRIP_ASSET_HEADER)) {
                continue;
            }

            int dexStart = findEmbeddedDexStart(assetBytes);
            if (dexStart < 0) {
                continue;
            }

            try {
                byte[] dexBytes = extractEmbeddedDex(assetBytes, dexStart);
                PendingDex pendingDex = pendingDexFromBytes(source.getName(), dexBytes, "asset_dex");
                if (!pendingDex.isFullyPresentIn(existingClassTypes)) {
                    pendingDexes.add(pendingDex);
                }
            } catch (Exception ignored) {
            }
        }

        pendingDexes.sort((left, right) -> {
            int bySize = Integer.compare(right.classTypes.size(), left.classTypes.size());
            if (bySize != 0) {
                return bySize;
            }
            return left.sourceName.compareTo(right.sourceName);
        });
        return pendingDexes;
    }

    private static PendingDex loadRestoreMethodDex(Set<String> existingClassTypes) throws Exception {
        try (InputStream input = Main.class.getResourceAsStream("/restoreMethod.dex")) {
            if (input == null) {
                throw new FileNotFoundException("Missing resource: /restoreMethod.dex");
            }
            PendingDex pendingDex = pendingDexFromBytes("restoreMethod.dex", readAllBytes(input), "restore_method");
            System.out.println("[INFO] Loaded classes from restoreMethod.dex");
            return pendingDex.isFullyPresentIn(existingClassTypes) ? null : pendingDex;
        }
    }

    private static PendingDex pendingDexFromBytes(String sourceName, byte[] dexBytes, String prefix) throws Exception {
        DexFile dexFile = loadDexFile(dexBytes, prefix);
        Set<String> classTypes = new HashSet<>();
        for (ClassDef classDef : dexFile.getClasses()) {
            classTypes.add(classDef.getType());
        }
        return new PendingDex(sourceName, dexBytes, classTypes);
    }

    private static boolean isTopLevelAsset(String entryName) {
        if (!entryName.startsWith("assets/")) {
            return false;
        }
        return entryName.indexOf('/', "assets/".length()) < 0;
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static int findEmbeddedDexStart(byte[] bytes) {
        for (int i = 0; i <= bytes.length - DEX_MAGIC.length; i++) {
            boolean matches = true;
            for (int j = 0; j < DEX_MAGIC.length; j++) {
                if (bytes[i + j] != DEX_MAGIC[j]) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return i;
            }
        }
        return -1;
    }

    private static byte[] extractEmbeddedDex(byte[] assetBytes, int dexStart) {
        int headerOffset = dexStart + 0x20;
        if (headerOffset + 4 > assetBytes.length) {
            throw new IllegalArgumentException("Embedded dex header truncated");
        }

        int declaredFileSize = readLittleEndianInt(assetBytes, headerOffset);
        if (declaredFileSize <= 0) {
            throw new IllegalArgumentException("Embedded dex declared invalid file size: " + declaredFileSize);
        }

        int dexEnd = dexStart + declaredFileSize;
        if (dexEnd > assetBytes.length) {
            throw new IllegalArgumentException("Embedded dex declared size exceeds asset bounds: " + declaredFileSize);
        }

        return Arrays.copyOfRange(assetBytes, dexStart, dexEnd);
    }

    private static int readLittleEndianInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8) | ((bytes[offset + 2] & 0xFF) << 16) | ((bytes[offset + 3] & 0xFF) << 24);
    }

    private static int nextDexNumber(Set<String> dexEntryNames) {
        int next = 1;
        for (String name : dexEntryNames) {
            next = Math.max(next, dexNumberOf(name) + 1);
        }
        return next;
    }

    private static int dexNumberOf(String dexEntryName) {
        if ("classes.dex".equals(dexEntryName)) {
            return 1;
        }
        if (!dexEntryName.startsWith("classes") || !dexEntryName.endsWith(".dex")) {
            return -1;
        }
        String middle = dexEntryName.substring("classes".length(), dexEntryName.length() - ".dex".length());
        if (middle.isEmpty()) {
            return 1;
        }
        try {
            return Integer.parseInt(middle);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static String nextDexEntryName(Set<String> dexEntryNames, int startNumber) {
        int number = Math.max(startNumber, 1);
        while (true) {
            String candidate = number == 1 ? "classes.dex" : "classes" + number + ".dex";
            if (!dexEntryNames.contains(candidate)) {
                return candidate;
            }
            number++;
        }
    }

    private static final class PendingDex {
        private final String sourceName;
        private final byte[] dexBytes;
        private final Set<String> classTypes;

        private PendingDex(String sourceName, byte[] dexBytes, Set<String> classTypes) {
            this.sourceName = sourceName;
            this.dexBytes = dexBytes;
            this.classTypes = classTypes;
        }

        private boolean isFullyPresentIn(Set<String> existingClassTypes) {
            return existingClassTypes.containsAll(classTypes);
        }

        private boolean canAddAgainst(Set<String> existingClassTypes) {
            for (String classType : classTypes) {
                if (existingClassTypes.contains(classType)) {
                    return false;
                }
            }
            return true;
        }
    }
}
