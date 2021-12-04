package org.telegram.ui.Reactions;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class SpacingHorizontalDecorator extends RecyclerView.ItemDecoration {

    final int spacing;

    public SpacingHorizontalDecorator(int spacing) {
        this.spacing = spacing;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);

        if (position < state.getItemCount() - 1) {
            outRect.set(0,0, spacing, 0);
        } else {
            outRect.setEmpty();
        }
    }
}


