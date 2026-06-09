package com.antik.utils;

public class loading {

    public static void progress(int c, int t) {
        if (t <= 0) t = 1;
        int w = 30;
        float p = (float) c / t;
        if (p > 1.0f) p = 1.0f;
        int f = (int) (p * w);

        StringBuilder s = new StringBuilder("\r[");
        for (int i = 0; i < f; i++) {
            s.append("#");
        }
        for (int i = f; i < w; i++) {
            s.append("-");
        }
        s.append("] ");
        s.append(String.format("%d%%", (int) (p * 100)));
        System.out.print(s.toString());
        System.out.flush();
    }
}
