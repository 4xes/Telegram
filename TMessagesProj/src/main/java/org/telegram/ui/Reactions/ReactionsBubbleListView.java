package org.telegram.ui.Reactions;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.List;

@SuppressLint("ViewConstructor")
public class ReactionsBubbleListView extends RecyclerListView {

    private final RectF bounds = new RectF();
    private final Path mask = new Path();
    private final int cornerRadius;

    private Matrix gradientMatrix;
    private final Paint gradientPaint = new Paint();

    private LinearGradient startGradientShader;
    private LinearGradient endGradientShader;

    public static final int SPACING = 4;
    public static final int HORIZONTAL_PADDING = 14;

    private final int horizontalPadding = AndroidUtilities.dp(HORIZONTAL_PADDING);
    private float progressAnimation = 1f;

    private final List<TLRPC.TL_availableReaction> reactions;

    private int colorBackground = Color.WHITE;

    class ScrollLayoutManager extends LinearLayoutManager {

        final int shrinkPadding = AndroidUtilities.dp(10);
        final int shrinkWidth = shrinkPadding * 2;

        public ScrollLayoutManager(Context context) {
            super(context, LinearLayoutManager.HORIZONTAL, false);
        }

        @Override
        public void onLayoutCompleted(State state) {
            super.onLayoutCompleted(state);
            rescaleChildren();
        }

        @Override
        public int scrollHorizontallyBy(int dx, Recycler recycler, State state) {
            int orientation = getOrientation();
            if (orientation == HORIZONTAL) {
                int scrolled = super.scrollHorizontallyBy(dx, recycler, state);

                rescaleChildren();
                return scrolled;
            } else {
                return 0;
            }
        }

        private void rescaleChildren() {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (child != null) {
                    int left = getDecoratedLeft(child);
                    int right = getDecoratedRight(child);

                    //int width = right - left;

                    int distance = 0;
                    boolean isLeft = false;
                    if (left < shrinkPadding) {
                        isLeft = true;
                        distance = shrinkPadding - left;
                    } else if (right > getMeasuredWidth() - shrinkPadding) {
                        distance = right - (getMeasuredWidth() - shrinkPadding);
                    }

                    if (distance == 0) {
                        child.setScaleX(1f);
                        child.setScaleY(1f);
                        child.setTranslationX(0f);
                    } else {
                        float percentShrink = (float) distance / (float) shrinkWidth;
                        float scale = 1f - Math.min(percentShrink / 3f, 0.25f);
                        child.setScaleX(scale);
                        child.setScaleY(scale);

//                        float offset = (width / 2f) * (1f - scale);
//                        if (isLeft) {
//                            child.setTranslationX(offset);
//                        } else {
//                            child.setTranslationX(-offset);
//                        }
                    }
                }
            }
        }
    }

    public void setProgressAnimation(float progressAnimation) {
        if (this.progressAnimation != progressAnimation) {
            this.progressAnimation = progressAnimation;
            invalidate();
        }
    }

    public ReactionsBubbleListView(Context context, int cornerRadius, List<TLRPC.TL_availableReaction> reactions, @Nullable ReactionSelectedListener selectedListener) {
        super(context);
        this.reactions = reactions;
        this.cornerRadius = cornerRadius;
        gradientPaint.setStyle(Paint.Style.FILL);
        startGradientShader = createShader(true);
        endGradientShader = createShader(false);
        gradientMatrix = new Matrix();

        if (isSupportOutline()) {
            setClipToOutline(true);
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, getMeasuredWidth(), getMeasuredHeight(), cornerRadius);
                }
            });
        }
        setPadding(horizontalPadding, 0, horizontalPadding, 0);
        addItemDecoration(new SpacingHorizontalDecorator(AndroidUtilities.dp(SPACING)));
        setHasFixedSize(true);
        setClipToPadding(false);
        setSelectorDrawableColor(Color.TRANSPARENT);
        setLayoutManager(new ScrollLayoutManager(getContext()));
        setAdapter(new ReactionsAdapter());
        setOnItemClickListener((view, position) -> {
                if (selectedListener != null) {
                    selectedListener.onSelected((ReactionCell) view, reactions.get(position).reaction);
                }
            }
        );
    }

    public int calculateContent() {
        int cellsWidth = AndroidUtilities.dp(ReactionCell.SIZE_CELL) * reactions.size();
        int spacing = AndroidUtilities.dp(SPACING) * (reactions.size() - 1);
        int padding = AndroidUtilities.dp(HORIZONTAL_PADDING) * 2;
        return cellsWidth + spacing + padding;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        bounds.set(0f, 0f, getMeasuredWidth(), getMeasuredHeight());
        rebuildMask();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (isSupportOutline()) {
            super.dispatchDraw(canvas);
            drawFading(canvas);
        } else {
            int save = canvas.save();
            canvas.clipPath(mask);
            super.dispatchDraw(canvas);
            drawFading(canvas);
            canvas.restoreToCount(save);
        }
    }

    public void drawFading(Canvas canvas) {
        int alpha = Math.round(255 * progressAnimation);
        if (alpha != 0) {
            gradientMatrix.setTranslate(bounds.left, 0);
            startGradientShader.setLocalMatrix(gradientMatrix);
            gradientPaint.setShader(startGradientShader);
            gradientPaint.setAlpha(alpha);
            canvas.drawRect(bounds.left, bounds.top, bounds.left + horizontalPadding, bounds.bottom, gradientPaint);

            gradientMatrix.setTranslate(bounds.right - horizontalPadding, 0);
            endGradientShader.setLocalMatrix(gradientMatrix);
            gradientPaint.setShader(endGradientShader);
            gradientPaint.setAlpha(alpha);
            canvas.drawRect(bounds.right - horizontalPadding, bounds.top, bounds.right, bounds.bottom, gradientPaint);
        }
    }

    private boolean isSupportOutline() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    private void rebuildMask() {
        if (!isSupportOutline()) {
            mask.reset();
            mask.addRoundRect(bounds, cornerRadius, cornerRadius, Path.Direction.CW);
            mask.close();
        }
    }

    @Override
    public void setBackgroundColor(int color) {
        if (colorBackground != color) {
            colorBackground = color;
            startGradientShader = createShader(true);
            endGradientShader = createShader(true);
        }
    }

    public LinearGradient createShader(boolean isLeft) {
        int start = isLeft ? colorBackground : Color.TRANSPARENT;
        int end = isLeft ? Color.TRANSPARENT : colorBackground;
        return new LinearGradient(0, 0, horizontalPadding, 0, start, end, Shader.TileMode.CLAMP);
    }

    private class ReactionsAdapter extends RecyclerListView.SelectionAdapter {

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ReactionCell cell = new ReactionCell(parent.getContext());
            cell.setLayoutParams(LayoutHelper.createFrame(ReactionCell.SIZE_CELL, ReactionCell.SIZE_CELL, Gravity.TOP, 0, 4, 0, 0));
            return new RecyclerListView.Holder(cell);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            TLRPC.TL_availableReaction object = reactions.get(position);
            ReactionCell cell = (ReactionCell) holder.itemView;
            cell.setSticker(object, "react");
        }

        @Override
        public int getItemCount() {
            return reactions.size();
        }

    }

    public interface ReactionSelectedListener {
        void onSelected(ReactionCell cell, String reaction);
    }
}
