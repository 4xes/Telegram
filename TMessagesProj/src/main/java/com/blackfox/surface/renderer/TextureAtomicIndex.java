package com.blackfox.surface.renderer;

import java.util.concurrent.atomic.AtomicInteger;

public class TextureAtomicIndex {
    static AtomicInteger index = new AtomicInteger(0);

    public static int getIndex() {
        int i = index.incrementAndGet();
        if (i > 31) {
            index.set(0);
            return 0;
        }
        return i;
    }
}
