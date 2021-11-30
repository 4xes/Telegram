package org.telegram.ui.Reactions;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

public class ReactionsBubbleListView extends RecyclerListView {

    private final RectF bounds = new RectF();
    private final Path mask = new Path();
    private final int cornerRadius;

    private final Paint gradientPaint = new Paint();

    private LinearGradient gradientShader;

    private final int horizontalPadding = AndroidUtilities.dp(14);
    private float progress = 1f;

    private final ArrayList<TLRPC.TL_availableReaction> reactions;

    public ReactionsBubbleListView(Context context, int cornerRadius, ArrayList<TLRPC.TL_availableReaction> reactions) {
        super(context);
        this.reactions = reactions;
        this.cornerRadius = cornerRadius;
        gradientPaint.setStyle(Paint.Style.FILL);
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
        setClipToPadding(false);

        setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        setAdapter(new ReactionsAdapter());
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
        } else {
            int save = canvas.save();
            canvas.clipPath(mask);
            super.dispatchDraw(canvas);
            canvas.restoreToCount(save);
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
        gradientShader = new LinearGradient(0, horizontalPadding, 0, 0, Color.TRANSPARENT, color, Shader.TileMode.CLAMP);
        gradientPaint.setShader(gradientShader);
    }

    private class ReactionsAdapter extends RecyclerListView.SelectionAdapter {

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ReactionCell cell = new ReactionCell(parent.getContext());
            cell.setLayoutParams(LayoutHelper.createFrame(34, 34, Gravity.TOP, 0, 4, 0, 0));
            return new RecyclerListView.Holder(cell);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            TLRPC.TL_availableReaction object = reactions.get(position);
            ReactionCell cell = (ReactionCell) holder.itemView;
            cell.setSticker(object.select_animation, "react");
        }

        @Override
        public int getItemCount() {
            return reactions.size();
        }

    }
}
