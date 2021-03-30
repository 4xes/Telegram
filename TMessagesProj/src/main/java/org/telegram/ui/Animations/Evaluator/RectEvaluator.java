package org.telegram.ui.Animations.Evaluator;

import android.animation.TypeEvaluator;
import android.graphics.Rect;
import android.graphics.RectF;

public class RectEvaluator implements TypeEvaluator<Rect> {

    private Rect mRect;

    public RectEvaluator() {
    }
    public RectEvaluator(Rect reuseRect) {
        mRect = reuseRect;
    }

    @Override
    public Rect evaluate(float fraction, Rect startValue, Rect endValue) {
        return evaluate(mRect, fraction, startValue, endValue);
    }

    public Rect evaluate(Rect rect, float fraction, Rect startValue, Rect endValue) {
        int left = startValue.left + (int) ((endValue.left - startValue.left) * fraction);
        int top = startValue.top + (int) ((endValue.top - startValue.top) * fraction);
        int right = startValue.right + (int) ((endValue.right - startValue.right) * fraction);
        int bottom = startValue.bottom + (int) ((endValue.bottom - startValue.bottom) * fraction);
        if (rect == null) {
            return new Rect(left, top, right, bottom);
        } else {
            rect.set(left, top, right, bottom);
            return rect;
        }
    }

    public Rect evaluate(Rect rect, float fraction, RectF startValue, RectF endValue) {
        int left = (int) (startValue.left + ((endValue.left - startValue.left) * fraction));
        int top = (int) (startValue.top + ((endValue.top - startValue.top) * fraction));
        int right = (int) (startValue.right + ((endValue.right - startValue.right) * fraction));
        int bottom = (int) (startValue.bottom +  ((endValue.bottom - startValue.bottom) * fraction));
        if (rect == null) {
            return new Rect(left, top, right, bottom);
        } else {
            rect.set(left, top, right, bottom);
            return rect;
        }
    }
}
