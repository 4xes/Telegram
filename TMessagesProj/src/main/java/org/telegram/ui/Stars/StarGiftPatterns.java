package org.telegram.ui.Stars;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.dpf2;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

public class StarGiftPatterns {

    public static final int TYPE_DEFAULT = 0;
    public static final int TYPE_ACTION = 1;
    public static final int TYPE_GIFT = 2;
    public static final int TYPE_LINK_PREVIEW = 3;

    private static final float[][] patternLocations = new float[][] {
        {
            83.33f, 24, 27.33f, .22f,
            68.66f, 75.33f, 25.33f, .21f,
            0, 86, 25.33f, .12f,
            -68.66f, 75.33f, 25.33f, .21f,
            -82.66f, 13.66f, 27.33f, .22f,
            -80, -33.33f, 20, .24f,
            -46.5f, -63.16f, 27, .21f,
            1, -82.66f, 20, .15f,
            46.5f, -63.16f, 27, .21f,
            80, -33.33f, 19.33f, .24f,

            115.66f, -63, 20, .15f,
            134, -10.66f, 20, .18f,
            118.66f, 55.66f, 20, .15f,
            124.33f, 98.33f, 20, .11f,

            -128, 98.33f, 20, .11f,
            -108, 55.66f, 20, .15f,
            -123.33f, -10.66f, 20, .18f,
            -116, -63.33f, 20, .15f
        },
        {
            27.33f, -57.66f, 20, .12f,
            59, -32, 19.33f, .22f,
            77, 4.33f, 22.66f, .2f,
            100, 40.33f, 18, .12f,
            58.66f, 59, 20, .18f,
            73.33f, 100.33f, 22.66f, .15f,
            75, 155, 22, .11f,

            -27.33f, -57.33f, 20, .12f,
            -59, -32.33f, 19.33f, .2f,
            -77, 4.66f, 23.33f, .2f,
            -98.66f, 41, 18.66f, .12f,
            -58, 59.33f, 19.33f, .18f,
            -73.33f, 100, 22, .15f,
            -75.66f, 155, 22, .11f
        },
        {
            -0.83f, -52.16f, 12.33f, .2f,
            26.66f, -40.33f, 16, .2f,
            44.16f, -20.5f, 12.33f, .2f,
            53, 7.33f, 16, .2f,
            31, 23.66f, 14.66f, .2f,
            0, 32, 13.33f, .2f,
            -29, 23.66f, 14, .2f,
            -53, 7.33f, 16, .2f,
            -44.5f, -20.16f, 12.33f, .2f,
            -27.33f, -40.33f, 16, .2f,
            43.66f, 50, 14.66f, .2f,
            -41.66f, 48, 14.66f, .2f
        },
        {
            -0.16f, -103.5f, 20.33f, .15f,
            39.66f, -77.33f, 26.66f, .15f,
            70.66f, -46.33f, 21.33f, .15f,
            84.5f, -3.83f, 29.66f, .15f,
            65.33f, 56.33f, 24.66f, .15f,
            0, 67.66f, 24.66f, .15f,
            -65.66f, 56.66f, 24.66f, .15f,
            -85, -4, 29.33f, .15f,
            -70.66f, -46.33f, 21.33f, .15f,
            -40.33f, -77.66f, 26.66f, .15f,

            62.66f, -109.66f, 21.33f, .11f,
            103.166f, -67.5f, 20.33f, .11f,
            110.33f, 37.66f, 20.66f, .11f,
            94.166f, 91.16f, 20.33f, .11f,
            38.83f, 91.16f, 20.33f, .11f,
            0, 112.5f, 20.33f, .11f,
            -38.83f, 91.16f, 20.33f, .11f,
            -94.166f, 91.16f, 20.33f, .11f,
            -110.33f, 37.66f, 20.66f, .11f,
            -103.166f, -67.5f, 20.33f, .11f,
            -62.66f, -109.66f, 21.33f, .11f
        }
    };

    public static void drawPattern(Canvas canvas, Drawable pattern, float w, float h, float alpha, float scale) {
        drawPattern(canvas, TYPE_DEFAULT, pattern, w, h, alpha, scale);
    }

    public static void drawPattern(Canvas canvas, int type, Drawable pattern, float w, float h, float alpha, float scale) {
        if (alpha <= 0.0f) return;
        for (int i = 0; i < patternLocations[type].length; i += 4) {
            final float x = patternLocations[type][i];
            final float y = patternLocations[type][i + 1];
            final float size = patternLocations[type][i + 2];
            final float thisAlpha = patternLocations[type][i + 3];

            float cx = x, cy = y, sz = size;
            if (w < h && type == TYPE_DEFAULT) {
                cx = y;
                cy = x;
            }
            cx *= scale;
            cy *= scale;
            sz *= scale;
            pattern.setBounds((int) (dp(cx) - dp(sz) / 2.0f), (int) (dp(cy) - dp(sz) / 2.0f), (int) (dp(cx) + dp(sz) / 2.0f), (int) (dp(cy) + dp(sz) / 2.0f));

            pattern.setAlpha((int) (0xFF * alpha * thisAlpha));
            pattern.draw(canvas);
        }
    }

    private static final float[] profileRight = new float[] {
        -35.66f, -5, 24, .2388f,
        -14.33f, -29.33f, 20.66f, .32f,
        -15, -73.66f, 19.33f, .32f,
        -2, -99.66f, 18, .1476f,
        -64.33f, -24.66f, 23.33f, .3235f,
        -40.66f, -53.33f, 24, .3654f,
        -50.33f, -85.66f, 20, .172f,
        -96, -1.33f, 19.33f, .3343f,
        -136.66f, -13, 18.66f, .2569f,
        -104.66f, -33.66f, 20.66f, .2216f,
        -82, -62.33f, 22.66f, .2562f,
        -131.66f, -60, 18, .1316f,
        -105.66f, -88.33f, 18, .1487f
    };
    private static final float[] profileLeft = new float[] {
        0, -107.33f, 16, .1505f,
        14.33f, -84, 18, .1988f,
        0, -50.66f, 18.66f, .3225f,
        13, -15, 18.66f, .37f,
        43.33f, 1, 18.66f, .3186f
    };


    public static void drawProfilePattern(Canvas canvas, Drawable pattern, float w, float h, float alpha, float full) {
        if (alpha <= 0.0f) return;

        final float b = h;
        final float l = 0, r = w;

        if (full > 0) {
            for (int i = 0; i < profileLeft.length; i += 4) {
                final float x = profileLeft[i];
                final float y = profileLeft[i + 1];
                final float size = profileLeft[i + 2];
                final float thisAlpha = profileLeft[i + 3];

                pattern.setBounds(
                    (int) (l + dpf2(x) - dpf2(size) / 2.0f),
                    (int) (b + dpf2(y) - dpf2(size) / 2.0f),
                    (int) (l + dpf2(x) + dpf2(size) / 2.0f),
                    (int) (b + dpf2(y) + dpf2(size) / 2.0f)
                );
                pattern.setAlpha((int) (0xFF * alpha * thisAlpha * full));
                pattern.draw(canvas);
            }

            final float sl = 77.5f, sr = 173.33f;
            final float space = w / AndroidUtilities.density - sl - sr;
            int count = Math.max(0, Math.round(space / 27.25f));
            if (count % 2 == 0) {
                count++;
            }
            for (int i = 0; i < count; ++i) {
                final float x = sl + space * ((float) i / (count - 1));
                final float y = i % 2 == 0 ? 0 : -12.5f;
                final float size = 17;
                final float thisAlpha = .21f;

                pattern.setBounds(
                    (int) (l + dpf2(x) - dpf2(size) / 2.0f),
                    (int) (b + dpf2(y) - dpf2(size) / 2.0f),
                    (int) (l + dpf2(x) + dpf2(size) / 2.0f),
                    (int) (b + dpf2(y) + dpf2(size) / 2.0f)
                );
                pattern.setAlpha((int) (0xFF * alpha * thisAlpha * full));
                pattern.draw(canvas);
            }
        }

        for (int i = 0; i < profileRight.length; i += 4) {
            final float x = profileRight[i];
            final float y = profileRight[i + 1];
            final float size = profileRight[i + 2];
            final float thisAlpha = profileRight[i + 3];

            pattern.setBounds(
                (int) (r + dpf2(x) - dpf2(size) / 2.0f),
                (int) (b + dpf2(y) - dpf2(size) / 2.0f),
                (int) (r + dpf2(x) + dpf2(size) / 2.0f),
                (int) (b + dpf2(y) + dpf2(size) / 2.0f)
            );
            pattern.setAlpha((int) (0xFF * alpha * thisAlpha));
            pattern.draw(canvas);
        }
    }

    // Заранее подготовленные значения чувствительности
    private static final float[] sensitivityLevels = {
            0.15f, 0.2f, 0.25f, 0.28f, 0.3f
    };

    /* ----------------------------------------------------
     *  Fox-ring pattern   (all values — dp)
     * --------------------------------------------------*/
    private static final int   OUTER_COUNT =  8;   // точек на внешнем кольце
    private static final int   INNER_COUNT = 10;   // точек на внутреннем кольце

    private static final float RING_SCALE = 0.5f;  // 1.0 = как есть, >1 = дальше, <1 = ближе

    private static float A_OUTER = 240f * RING_SCALE;
    private static float B_OUTER = 160f * RING_SCALE;
    private static float A_INNER = 155f * RING_SCALE;
    private static float B_INNER = 105f * RING_SCALE;

    // смещение начала (в градусах) — кручём овал, чтобы точки
    // совпали с макетом:   0° = вправо, 90° = вверх
    private static final float OUTER_OFFSET_DEG = -20f;
    private static final float INNER_OFFSET_DEG =   9f;

    // размеры / прозрачность одинаковые для всех точек,
    // но при желании их тоже можно вынести в отдельные константы
    private static final float SIZE_DP   = 20f;
    private static final float ALPHA     = 0.25f;
    private static final float SENS_OUT  = sensitivityLevels[2];   // внешнее кольцо
    private static final float SENS_IN   = sensitivityLevels[1];   // внутреннее

    /* ----------------------------------------------------
     *  Строим массив patternPoints динамически
     * --------------------------------------------------*/
    private static final float[] patternPoints = buildPattern();

    /** Собирает patternPoints вида  {x, y, size, alpha, sensitivity … }  */
    private static float[] buildPattern() {
        java.util.ArrayList<Float> pts = new java.util.ArrayList<>(
                (OUTER_COUNT + INNER_COUNT) * 5);

        addRing(pts, OUTER_COUNT, A_OUTER, B_OUTER,
                OUTER_OFFSET_DEG, SIZE_DP, ALPHA, SENS_OUT);
        addRing(pts, INNER_COUNT, A_INNER, B_INNER,
                INNER_OFFSET_DEG, SIZE_DP, ALPHA, SENS_IN);

        // превратим List<Float> в float[]
        float[] res = new float[pts.size()];
        for (int i = 0; i < res.length; i++) res[i] = pts.get(i);
        return res;
    }

    /** Добавляет в список точки одного кольца-овала  */
    private static void addRing(java.util.List<Float> pts,
                                int count, float a, float b,
                                float startDeg,
                                float size, float alpha, float sensitivity) {
        final double step = 2 * Math.PI / count;
        final double offset = Math.toRadians(startDeg);

        for (int i = 0; i < count; i++) {
            double ang = offset + i * step;
            float  x   =  (float) (Math.cos(ang) * a);
            float  y   =  (float) (Math.sin(ang) * b);

            pts.add(x);           // смещение по-X относительно центра
            pts.add(y);           // смещение по-Y
            pts.add(size);        // диаметр иконки
            pts.add(alpha);       // локальная прозрачность
            pts.add(sensitivity); // «чувствительность» (порог появления)
        }
    }

    public static void drawProfilePattern(Canvas canvas, Drawable drawable, float width, float height, float visibility) {
        float midX = width / 2f;
        float midY = AndroidUtilities.lerp(height / (2f / visibility), -dp(16), 1f - visibility);

        for (int i = 0; i < patternPoints.length; i += 5) {
            float offsetX = patternPoints[i];
            float offsetY = patternPoints[i + 1];
            float baseSize = patternPoints[i + 2];
            float localAlpha = patternPoints[i + 3];
            float threshold = patternPoints[i + 4];

            float remaining = 1f - threshold;
            if (remaining == 0f) remaining = 1f;

            float progress = Math.max(0f, (1f - visibility - threshold)) / remaining;
            float factor = Math.min(progress / threshold, 1f);

            float x = visibility == 0f ? 0f : AndroidUtilities.lerp(offsetX, 0f, factor);
            float y = visibility == 0f ? 0f : AndroidUtilities.lerp(offsetY, 0f, factor);

            float finalSize = dpf2(baseSize * visibility);
            float posX = midX + dpf2(x) - finalSize / 2f;
            float posY = midY + dpf2(y) - finalSize / 2f;

            drawable.setBounds(
                    (int) posX, (int) posY,
                    (int) (posX + finalSize), (int) (posY + finalSize)
            );
            drawable.setAlpha((int) (255 * visibility * localAlpha));
            drawable.draw(canvas);
        }
    }

}
