package org.telegram.ui.Animations.Cells;

import android.content.Context;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.Animations.Background.GradientSurfaceView;

public class GradientSurfaceCell extends GradientSurfaceView {

    private final int size;

    public GradientSurfaceCell(Context context) {
        super(context);
        size = 136;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(size), MeasureSpec.EXACTLY));
    }
}
