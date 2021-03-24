package org.telegram.ui.Animations.Background;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.View;

import org.telegram.ui.Animations.Background.gradient.GradientColorEvaluator;
import org.telegram.ui.Animations.Background.gradient.GradientPointsEvaluator;
import org.telegram.ui.Animations.Background.gradient.GradientRenderer;
import org.telegram.ui.Animations.Background.gradient.Points;

public class GradientSurfaceView extends GLTextureView {

    public int[] indexes = Points.startPoints();

    private GradientRenderer gradientRenderer;

    public GradientSurfaceView(Context context) {
        super(context);
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
        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPositionAnimation();
            }
        });
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

    private final Animator.AnimatorListener animatorListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (requestedAnimation) {
                requestedAnimation = false;
                animatePosition();
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    };

    private boolean requestedAnimation = false;

    public void requestPositionAnimation() {
        if (positionAnimator != null && positionAnimator.isRunning()) {
            requestedAnimation = true;
        } else {
            animatePosition();
        }
    }

    private void animatePosition() {
        float[][] start = Points.emptyPoints();
        Points.copyPoints(start, gradientRenderer.points);
        float[][] end = Points.emptyPoints();
        Points.shiftIndexes(indexes);
        Points.fillPoints(indexes, end);

        if (positionAnimator != null) {
            positionAnimator.cancel();
            positionAnimator = null;
        }
        positionAnimator = ValueAnimator.ofObject(new GradientPointsEvaluator(gradientRenderer.points), start, end);
        positionAnimator.setDuration(1000L);
        positionAnimator.addUpdateListener(progressUpdateListener);
        positionAnimator.addListener(animatorListener);
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
