package org.telegram.ui.Animations.Components;

import android.graphics.PointF;

import org.telegram.ui.Animations.InterpolatorData;
import org.telegram.ui.Components.CubicBezierInterpolator;

public class AnimationCubicBezierInterpolator extends CubicBezierInterpolator {
    protected float timeStart;
    protected float timeEnd;


    public AnimationCubicBezierInterpolator(InterpolatorData data) {
        super(new PointF(data.timeStart, 0f), new PointF(data.timeEnd, 1f));
        this.timeStart = data.timeStart;
        this.timeEnd = data.timeEnd;
    }

    @Override
    public float getInterpolation(float time) {
        if (time < timeStart) {
            return 0f;
        }
        if (time > timeEnd) {
            return 1f;
        }
        float range = (timeEnd - timeStart);
        float bezierTime = (time - timeStart) / range;
        return getBezierCoordinateY(getXForTime(bezierTime));
    }
}
