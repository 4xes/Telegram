package org.telegram.ui.Animations;

public class InterpolatorData {
    public float progressionTop;
    public float progressionBottom;
    public float timeStart;
    public float timeEnd;

    public InterpolatorData(float progressionTop, float progressionBottom, float timeStart, float timeEnd) {
        this.progressionTop = progressionTop;
        this.progressionBottom = progressionBottom;
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
    }

    public static InterpolatorData defaultData() {
        return new InterpolatorData(
                DEFAULT_PROGRESSION_TOP,
                DEFAULT_PROGRESSION_BOTTOM,
                DEFAULT_TIME_START,
                DEFAULT_TIME_END
        );
    }

    public static final float DEFAULT_PROGRESSION_TOP = 0f;
    public static final float DEFAULT_PROGRESSION_BOTTOM = 0.33f;
    public static final float DEFAULT_TIME_START = 0f;
    public static final float DEFAULT_TIME_END = 0.5f;
}