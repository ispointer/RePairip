import com.reandroid.apk.ApkModule;
import com.reandroid.archive.InputSource;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Field;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class JunkClasses {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("usage: java -cp RePairip.jar JunkClasses.java <merged.apk>");
            System.exit(2);
        }
        ApkModule module = ApkModule.loadApkFile(new File(args[0]));
        List<String> junk = new ArrayList<String>();
        for (InputSource source : module.getInputSources()) {
            if (!source.getName().endsWith(".dex")) continue;
            byte[] bytes;
            try (InputStream in = source.openStream()) {
                bytes = in.readAllBytes();
            }
            File temp = File.createTempFile("junk", ".dex");
            Files.write(temp.toPath(), bytes);
            DexFile dex = DexFileFactory.loadDexFile(temp, Opcodes.getDefault());
            temp.delete();
            for (ClassDef cls : dex.getClasses()) {
                if (cls.getMethods().iterator().hasNext()) continue;
                if (!cls.getFields().iterator().hasNext()) continue;
                if (!"Ljava/lang/Object;".equals(cls.getSuperclass())) continue;
                if (cls.getAccessFlags() != 1) continue;
                boolean ok = true;
                for (Field field : cls.getFields()) {
                    if (field.getAccessFlags() != 9) { ok = false; break; }
                    String type = field.getType();
                    if (!type.equals("Ljava/lang/String;") && !type.equals("Ljava/lang/reflect/Method;")) {
                        ok = false;
                        break;
                    }
                    if (field.getInitialValue() != null) { ok = false; break; }
                }
                if (ok) junk.add(cls.getType());
            }
        }
        if (junk.isEmpty()) {
            System.err.println("no junk classes found (is this a PairIP APK?)");
            System.exit(1);
        }
        for (String type : junk) System.out.println(type);
    }
}
