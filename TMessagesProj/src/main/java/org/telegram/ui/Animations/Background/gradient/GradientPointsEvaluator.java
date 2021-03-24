package org.telegram.ui.Animations.Background.gradient;


import android.animation.TypeEvaluator;

public class GradientPointsEvaluator implements TypeEvaluator<float[][]> {

    float[][] reuse;

    public GradientPointsEvaluator(float[][] reuse) {
        this.reuse = reuse;
    }

    @Override
    public float[][] evaluate(float fraction, float[][] startValue, float[][] endValue) {
        for (int i = 0; i < 4; i++) {
            reuse[i][0] = evaluate(fraction, startValue[i][0], endValue[i][0]);
            reuse[i][1] = evaluate(fraction, startValue[i][1], endValue[i][1]);
        }
        return reuse;
    }

    private float evaluate(float fraction, float startValue, float endValue) {
        return startValue + fraction * (endValue - startValue);
    }
}
