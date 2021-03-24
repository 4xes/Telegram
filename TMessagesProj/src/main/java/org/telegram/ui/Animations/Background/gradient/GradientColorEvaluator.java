package org.telegram.ui.Animations.Background.gradient;


import android.animation.ArgbEvaluator;
import android.animation.TypeEvaluator;

public class GradientColorEvaluator implements TypeEvaluator<int[]> {

    ArgbEvaluator evaluator = new ArgbEvaluator();

    int[] reuse;

    public GradientColorEvaluator(int[] reuse) {
        this.reuse = reuse;
    }

    @Override
    public int[] evaluate(float fraction, int[] startValue, int[] endValue) {
        for (int i = 0; i < 4; i++) {
            reuse[i] = (int) evaluator.evaluate(fraction, startValue[i], endValue[i]);
        }
        return reuse;
    }

}
