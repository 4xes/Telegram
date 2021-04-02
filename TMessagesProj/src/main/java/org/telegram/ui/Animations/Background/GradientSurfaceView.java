package org.telegram.ui.Animations.Background;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.animation.Interpolator;

import org.telegram.ui.Animations.AnimationManager;
import org.telegram.ui.Animations.AnimationPreferences;
import org.telegram.ui.Animations.AnimationType;
import org.telegram.ui.Animations.Background.gradient.GradientColorEvaluator;
import org.telegram.ui.Animations.Background.gradient.GradientPointsEvaluator;
import org.telegram.ui.Animations.Background.gradient.GradientRenderer;
import org.telegram.ui.Animations.Background.gradient.Points;
import org.telegram.ui.Animations.Parameter;
import org.telegram.ui.Animations.InterpolatorData;
import org.telegram.ui.Components.CubicBezierInterpolator;

public class GradientSurfaceView extends GLTextureView {

    public int[] indexes;

    AnimationType animationType = AnimationType.Background;
    private GradientRenderer gradientRenderer;

    boolean scheduleAnimation = false;
    private boolean needSaveIndexes;
    private AnimationPreferences preferences;

    public GradientSurfaceView(Context context, AnimationPreferences preferences, boolean needSaveIndexes) {
        super(context);
        indexes = preferences.getBackgroundIndexes();
        this.preferences = preferences;
        this.needSaveIndexes = needSaveIndexes;
        init();
    }

    public GradientSurfaceView(Context context) {
        super(context);
        indexes = Points.startPoints();
        init();
    }

    public void init() {
        setEGLContextClientVersion(2);
        gradientRenderer = new GradientRenderer(getContext());
        gradientRenderer.setColors(new int[]{
                0xfffff6bf,
                0xff76a076,
                0xfff6e477,
                0xff316b4d
        });
        Points.fillPoints(indexes, gradientRenderer.points);
        setRenderer(gradientRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    private final ValueAnimator.AnimatorUpdateListener progressUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            requestRender();
        }
    };

    private final ValueAnimator.AnimatorUpdateListener colorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            requestRender();
        }
    };

    private final Animator.AnimatorListener scheduleAnimatorListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (nextParameter != null) {
                animatePosition(nextParameter);
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    };

    private Parameter nextParameter = null;

    public void requestPositionAnimation(Parameter parameter) {
        if (scheduleAnimation && nextParameter != null && positionAnimator != null && positionAnimator.isRunning()) {
            nextParameter = parameter;
        } else {
            animatePosition(parameter);
        }
    }

    public Animator.AnimatorListener animatorListener;

    private void animatePosition(Parameter parameter) {
        float[][] start = Points.emptyPoints();
        Points.copyPoints(start, gradientRenderer.points);
        float[][] end = Points.emptyPoints();
        Points.shiftIndexes(indexes);
        if (needSaveIndexes && preferences != null) {
            preferences.putBackgroundIndexes(indexes);
        }
        Points.fillPoints(indexes, end);

        if (positionAnimator != null) {
            positionAnimator.cancel();
            positionAnimator = null;
        }
        long duration = AnimationManager.getInstance().getDuration(animationType, parameter);
        Interpolator interpolator = AnimationManager.getInstance().getInterpolator(animationType, parameter);
        positionAnimator = ValueAnimator.ofObject(new GradientPointsEvaluator(gradientRenderer.points), start, end);
        positionAnimator.setDuration(duration);
        positionAnimator.setInterpolator(interpolator);
        positionAnimator.addUpdateListener(progressUpdateListener);
        if (animatorListener != null) {
            positionAnimator.addListener(animatorListener);
        }
        if (scheduleAnimation) {
            positionAnimator.addListener(scheduleAnimatorListener);
        }
        positionAnimator.start();
    }

    public void setColors(int[] colors, boolean animate) {
        if (colorAnimator != null) {
            colorAnimator.cancel();
            colorAnimator = null;
        }
        if (animate) {
            int[] start = new int[4];
            int[] end = new int[4];
            for (int i = 0; i < 4; i++) {
                start[i] = gradientRenderer.colors[i];
                end[i] = colors[i];
            }
            colorAnimator = ValueAnimator.ofObject(new GradientColorEvaluator(gradientRenderer.colors), start, end);
            colorAnimator.setDuration(1000L);
            colorAnimator.addUpdateListener(colorUpdateListener);
            colorAnimator.addListener(animatorListener);
            colorAnimator.start();
        } else {
            gradientRenderer.setColors(colors);
        }
    }

    private ValueAnimator positionAnimator;
    private ValueAnimator colorAnimator;

    private static final String TAG = GradientRenderer.class.getSimpleName();
}
