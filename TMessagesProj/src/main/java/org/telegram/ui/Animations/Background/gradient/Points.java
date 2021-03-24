package org.telegram.ui.Animations.Background.gradient;

import android.util.Log;

import java.util.Arrays;

public class Points {
    private static final float[] p1 = new float[]{0.352f, 0.255f};
    private static final float[] p2 = new float[]{0.819f, 0.081f};
    private static final float[] p3 = inverted(new float[]{0.352f, 0.255f});
    private static final float[] p4 = inverted(new float[]{0.819f, 0.081f});
    private static final float[] p5 = new float[]{0.266f, 0.578f};
    private static final float[] p6 = new float[]{0.586f, 0.161f};
    private static final float[] p7 = inverted(new float[]{0.266f, 0.578f});
    private static final float[] p8 = inverted(new float[]{0.586f, 0.161f});

    private static final float[][] pointsPool = new float[][]{
            p1, p5, p4, p8, p3, p7, p2, p6
    };

    public static void shiftIndexes(int[] dest) {
        for (int i = 0; i < dest.length; i++) {
            dest[i] = (dest[i] + 1) % 8;
        }
        Log.e("T", Arrays.toString(dest));
    }
    public static void fillPoints(int[] indexes, float[][] points) {
        for (int i = 0; i < indexes.length; i ++) {
            points[i][0] = pointsPool[indexes[i]][0];
            points[i][1] = pointsPool[indexes[i]][1];
        }
    }

    public static void copyPoints(float[][] dest, float[][] src) {
        for (int i = 0; i < dest.length; i ++) {
            dest[i][0] = src[i][0];
            dest[i][1] = src[i][1];
        }
    }

    public static int[] startPoints() {
        return new int[] {0, 6, 4, 2};
    }

    public static float[][] emptyPoints() {
        return new float[4][2];
    }

    private static float[] inverted(float[] point) {
        return new float[]{1.0f - point[0], 1.0f - point[1]};
    }
}
