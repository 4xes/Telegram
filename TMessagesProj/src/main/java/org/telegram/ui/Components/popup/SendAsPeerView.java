package org.telegram.ui.Components.popup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.List;
import java.util.Map;

@SuppressLint("ViewConstructor")
public class SendAsPeerView extends FrameLayout {

    private long currentDialogId = 0;
    private final SendAsPeerData data;

    protected TextView titleView;

    private final Theme.ResourcesProvider resourcesProvider;

    public static int startAnimationWidth = AndroidUtilities.dp(174);
    public static int startAnimationHeight = AndroidUtilities.dp(210);

    public static int endAnimationWidth = AndroidUtilities.dp(250);
    public static int endAnimationHeight = AndroidUtilities.dp(400);

    boolean animationInProgress = false;

    int currentScrollY = 0;

    @SuppressLint("NotifyDataSetChanged")
    public SendAsPeerView(@NonNull Context context, SendAsPeerData data, Theme.ResourcesProvider resourcesProvider, SendAsPeerSelectListener listener) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        this.data = data;
        currentDialogId = DialogObject.getPeerDialogId(data.currentPeer);
        titleView = new TextView(context);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        titleView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        titleView.setLines(1);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        titleView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader));
        titleView.setText(LocaleController.getString("SendAsPeerTitle", R.string.SendAsPeerTitle));
        addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 16, 13, 14, 0));
        View shadowView = new View(context);
        shadowView.setBackgroundResource(R.drawable.header_shadow);

        final RecyclerListView listView = createListView();

        final int offsetThresholdY = AndroidUtilities.dp(8);
        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                currentScrollY += dy;
                int value = Math.max(Math.min(currentScrollY, offsetThresholdY), 0);
                float newAlpha = ((float) value / offsetThresholdY);
                if (shadowView.getAlpha() != newAlpha) {
                    shadowView.setAlpha(newAlpha);
                }
            }
        });
        listView.setOnItemClickListener((view, position) -> {
            if (animationInProgress) {
                return;
            }

            long selectedDialog = 0;
            if (view instanceof SendAsPeerCell) {
                SendAsPeerCell cell = ((SendAsPeerCell) view);
                selectedDialog = cell.getCurrentDialog();
                if (selectedDialog == currentDialogId) {
                    listener.onSelectedPeer(null);
                    return;
                }
                cell.setChecked(true, true);
            }
            for (int a = 0, N = listView.getChildCount(); a < N; a++) {
                View child = listView.getChildAt(a);
                if (child != view) {
                    if (view instanceof SendAsPeerCell) {
                        ((SendAsPeerCell) child).setChecked(false, true);
                    }
                }
            }
            listener.onSelectedPeer(data.peersMap.get(selectedDialog));
        });

        addView(listView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 120 - 38, Gravity.LEFT | Gravity.TOP, 0, 38, 0, 0));
        addView(shadowView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 3, Gravity.LEFT | Gravity.TOP, 0, 38, 0, 0));
        setBackground(Theme.createRadSelectorDrawable(Theme.getColor(Theme.key_dialogButtonSelector), AndroidUtilities.dp(4), AndroidUtilities.dp(4)));
        setEnabled(false);
    }

    boolean ignoreLayout;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(endAnimationWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(120), MeasureSpec.EXACTLY));
    }

    @Override
    public void requestLayout() {
        if (ignoreLayout) {
            return;
        }
        super.requestLayout();
    }

    public RecyclerListView createListView() {
        RecyclerListView recyclerListView = new RecyclerListView(getContext());
        recyclerListView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerListView.setAdapter(new SendAsPeerAdapter());
        return recyclerListView;
    }

    private class SendAsPeerAdapter extends RecyclerListView.SelectionAdapter {

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            SendAsPeerCell cell = new SendAsPeerCell(parent.getContext(), resourcesProvider);
            cell.setLayoutParams(new RecyclerView.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            return new RecyclerListView.Holder(cell);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            TLObject object = data.objects.get(position);
            SendAsPeerCell cell = (SendAsPeerCell) holder.itemView;
            boolean checked = false;
            if (object instanceof TLRPC.Chat) {
                checked = currentDialogId == -((TLRPC.Chat) object).id;
            }
            if (object instanceof TLRPC.User) {
                checked = currentDialogId == ((TLRPC.User) object).id;
            }
            cell.setDialog(object, checked);
        }

        @Override
        public int getItemCount() {
            return data.objects.size();
        }

    }


    public static class SendAsPeerData {
        public TLRPC.Peer currentPeer;
        public TLRPC.InputPeer inputPeer;
        public List<TLObject> objects;
        public Map<Long, TLRPC.Peer> peersMap;

        public SendAsPeerData(TLRPC.Peer currentPeer, TLRPC.InputPeer inputPeer, List<TLObject> objects, Map<Long, TLRPC.Peer> peersMap) {
            this.currentPeer = currentPeer;
            this.inputPeer = inputPeer;
            this.objects = objects;
            this.peersMap = peersMap;
        }
    }

    private int getThemedColor(String key) {
        Integer color = resourcesProvider != null ? resourcesProvider.getColor(key) : null;
        return color != null ? color : Theme.getColor(key);
    }

    public interface SendAsPeerSelectListener {
        void onSelectedPeer(TLRPC.Peer peer);
    }
}
