package com.antik;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class CrcPatcher {
    public static void patch(File s_a, File t_a) throws IOException {
        
        Map<String, long[]> s_es = new LinkedHashMap<String, long[]>();
        Map<String, long[]> t_es = new LinkedHashMap<String, long[]>();

        File[] f_ls = {s_a, t_a};
        
        for (int i = 0; i < 2; i++) {
            File f = f_ls[i];
            Map<String, long[]> cur = (i == 0) ? s_es : t_es;

            try (RandomAccessFile rf = new RandomAccessFile(f, "r")) {
                FileChannel ch = rf.getChannel();
                long sz = ch.size();
                int t_sz = (int) Math.min(65558L, sz);
                ByteBuffer t_bf = ByteBuffer.allocate(t_sz).order(ByteOrder.LITTLE_ENDIAN);
                ch.read(t_bf, sz - t_sz);
                t_bf.flip();

                Long eo = null;
                for (int j = t_bf.limit() - 4; j >= 0; j--) {
                    if (t_bf.getInt(j) == 0x06054b50) {
                        eo = (sz - t_sz) + (long)j;
                        break;
                    }
                }
                
                if (eo != null) {
                    ByteBuffer eb = ByteBuffer.allocate(22).order(ByteOrder.LITTLE_ENDIAN);
                    ch.read(eb, eo);
                    eb.flip();
                    long cds = eb.getInt(12) & 0xFFFFFFFFL;
                    long cdo = eb.getInt(16) & 0xFFFFFFFFL;

                    ByteBuffer hb = ByteBuffer.allocate(46).order(ByteOrder.LITTLE_ENDIAN);
                    long co = cdo;
                    long e_o = cdo + cds;
                    while (co < e_o) {
                        hb.clear();
                        ch.read(hb, co);
                        hb.flip();
                        if (hb.remaining() < 46 || hb.getInt() != 0x02014b50) {
                            break;
                        }

                        long co_c = co + 16;
                        hb.position(16);
                        int c = hb.getInt();

                        hb.position(28);
                        int nl = hb.getShort() & 0xFFFF;
                        int el = hb.getShort() & 0xFFFF;
                        int cl = hb.getShort() & 0xFFFF;

                        hb.position(42);
                        long lho = hb.getInt() & 0xFFFFFFFFL;

                        byte[] nb = new byte[nl];
                        ByteBuffer nbf = ByteBuffer.wrap(nb);
                        ch.read(nbf, co + 46);
                        String n = new String(nb, StandardCharsets.UTF_8);

                        if (n.startsWith("classes") && n.endsWith(".dex")) {
                            ByteBuffer lhb = ByteBuffer.allocate(30).order(ByteOrder.LITTLE_ENDIAN);
                            ch.read(lhb, lho);
                            lhb.flip();
                            if (lhb.remaining() >= 30 && lhb.getInt() == 0x04034b50) {
                                cur.put(n, new long[]{co_c, lho + 14, (long) c});
                            }
                        }

                        co += 46L + nl + el + cl;
                    }
                }
            }
        }

        if (s_es.isEmpty() || t_es.isEmpty()) {
            System.out.println("[CRC] No dex files found in apk");
            return;
        }

        boolean mod = false;
        try (RandomAccessFile rf = new RandomAccessFile(t_a, "rw");
             FileChannel ch = rf.getChannel()) {
            
            ByteBuffer b = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
            
            Iterator<Map.Entry<String, long[]>> it = s_es.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, long[]> e = it.next();

                long[] se = e.getValue();
                long[] te = t_es.get(e.getKey());
                if (te != null && te[2] != se[2]) {
                    int n_c = (int) se[2];
                    b.clear();
                    b.putInt(n_c);
                    b.flip();
                    ch.write(b, te[0]);
                    b.clear();
                    b.putInt(n_c);
                    b.flip();
                    ch.write(b, te[1]);
                    
                    mod = true;
                }
            }
            
            if (mod) {
                ch.force(true);
                System.out.println("[CRC] Patched dex CRC values from merged APK");
            }
        }
    }
}
