package org.telegram.ui.Components;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.Theme;


public class LinkActionOptionsView extends ImageView {
    private final FrameLayout container;
    private final FrameLayout frameLayout;
    private final float[] point = new float[2];
    private boolean isShowRevoke = true;
    private boolean isShowEdit = true;
    private QrOptionDelegate optionDelegate;
    private ActionBarPopupWindow actionBarPopupWindow;

    public LinkActionOptionsView(Context context, FrameLayout container, FrameLayout frameLayout) {
        super(context);
        this.container = container;
        this.frameLayout = frameLayout;

        setScaleType(ImageView.ScaleType.CENTER);
        setLayoutParams(LayoutHelper.createFrame(40, 48, Gravity.RIGHT | Gravity.CENTER_VERTICAL));
        updateState();
    }

    public void updateState() {
        boolean isMoreOption = isShowEdit || isShowRevoke;
        if (isMoreOption) {
            setImageResource(R.drawable.ic_ab_other);
            setContentDescription(LocaleController.getString(R.string.AccDescrMoreOptions));
            setOnClickListener(v -> {
                showPopupWindow();
            });
        } else {
            setImageResource(R.drawable.msg_qrcode);
            setContentDescription(LocaleController.getString(R.string.GetQRCode));
            setOnClickListener(v -> {
                if (optionDelegate != null) {
                    optionDelegate.onQrOptionClicked();
                }
            });
        }
    }

    private void dismiss() {
        if (actionBarPopupWindow != null) {
            actionBarPopupWindow.dismiss();
        }
    }

    private void showPopupWindow() {
        if (actionBarPopupWindow != null) {
            return;
        }
        ActionBarPopupWindow.ActionBarPopupWindowLayout layout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(getContext());

        ActionBarMenuSubItem subItem;
        if (isShowEdit) {
            subItem = new ActionBarMenuSubItem(getContext(), true, false);
            subItem.setTextAndIcon(LocaleController.getString(R.string.Edit), R.drawable.msg_edit);
            layout.addView(subItem, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
            subItem.setOnClickListener(view -> {
                dismiss();
                if (optionDelegate != null) {
                    optionDelegate.onEditOptionClicked();
                }
            });
        }

        subItem = new ActionBarMenuSubItem(getContext(), true, false);
        subItem.setTextAndIcon(LocaleController.getString(R.string.GetQRCode), R.drawable.msg_qrcode);
        layout.addView(subItem, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
        subItem.setOnClickListener(view -> {
            dismiss();
            if (optionDelegate != null) {
                optionDelegate.onQrOptionClicked();
            }
        });

        if (isShowRevoke) {
            subItem = new ActionBarMenuSubItem(getContext(), false, true);
            subItem.setTextAndIcon(LocaleController.getString(R.string.RevokeLink), R.drawable.msg_delete);
            subItem.setColors(Theme.getColor(Theme.key_text_RedRegular), Theme.getColor(Theme.key_text_RedRegular));
            layout.addView(subItem, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
            subItem.setOnClickListener(view -> {
                dismiss();
                if (optionDelegate != null) {
                    optionDelegate.onRevokeOptionClicked();
                }
            });
        }

        if (container != null) {
            float x = 0;
            float y;
            getPointOnScreen(frameLayout, container, point);
            y = point[1];

            final FrameLayout finalContainer = container;
            View dimView = new View(getContext()) {

                @Override
                protected void onDraw(Canvas canvas) {
                    canvas.drawColor(0x33000000);
                    getPointOnScreen(frameLayout, finalContainer, point);
                    canvas.save();
                    float clipTop = ((View) frameLayout.getParent()).getY() + frameLayout.getY();
                    if (clipTop < 1) {
                        canvas.clipRect(0, point[1] - clipTop + 1, getMeasuredWidth(), getMeasuredHeight());
                    }
                    canvas.translate(point[0], point[1]);

                    frameLayout.draw(canvas);
                    canvas.restore();
                }
            };

            ViewTreeObserver.OnPreDrawListener preDrawListener = () -> {
                dimView.invalidate();
                return true;
            };
            finalContainer.getViewTreeObserver().addOnPreDrawListener(preDrawListener);
            container.addView(dimView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            dimView.setAlpha(0);
            dimView.animate().alpha(1f).setDuration(150);
            layout.measure(View.MeasureSpec.makeMeasureSpec(container.getMeasuredWidth(), View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(container.getMeasuredHeight(), View.MeasureSpec.UNSPECIFIED));


            actionBarPopupWindow = new ActionBarPopupWindow(layout, LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);
            actionBarPopupWindow.setOnDismissListener(() -> {
                actionBarPopupWindow = null;
                dimView.animate().cancel();
                dimView.animate().alpha(0).setDuration(150).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (dimView.getParent() != null) {
                            finalContainer.removeView(dimView);
                        }
                        finalContainer.getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
                    }
                });
            });
            actionBarPopupWindow.setOutsideTouchable(true);
            actionBarPopupWindow.setFocusable(true);
            actionBarPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            actionBarPopupWindow.setAnimationStyle(R.style.PopupContextAnimation);
            actionBarPopupWindow.setInputMethodMode(ActionBarPopupWindow.INPUT_METHOD_NOT_NEEDED);
            actionBarPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED);

            layout.setDispatchKeyEventListener(keyEvent -> {
                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK && keyEvent.getRepeatCount() == 0 && actionBarPopupWindow.isShowing()) {
                    actionBarPopupWindow.dismiss(true);
                }
            });

            if (AndroidUtilities.isTablet()) {
                y += container.getPaddingTop();
                x -= container.getPaddingLeft();
            }
            actionBarPopupWindow.showAtLocation(container, 0, (int) (container.getMeasuredWidth() - layout.getMeasuredWidth() - AndroidUtilities.dp(16) + container.getX() + x), (int) (y + frameLayout.getMeasuredHeight() + container.getY()));
        }
    }

    private void getPointOnScreen(FrameLayout frameLayout, FrameLayout finalContainer, float[] point) {
        float x = 0;
        float y = 0;
        View v = frameLayout;
        while (v != finalContainer) {
            y += v.getY();
            x += v.getX();
            if (v instanceof ScrollView) {
                y -= v.getScrollY();
            }
            if (!(v.getParent() instanceof View)) {
                break;
            }
            v = (View) v.getParent();
            if (!(v instanceof ViewGroup)) {
                return;
            }
        }
        x -= finalContainer.getPaddingLeft();
        y -= finalContainer.getPaddingTop();
        point[0] = x;
        point[1] = y;
    }

    public void setShowRevokeAndEdit(boolean isShowRevoke, boolean isShowEdit) {
        if (this.isShowRevoke != isShowRevoke || this.isShowEdit != isShowEdit) {
            this.isShowRevoke = isShowRevoke;
            this.isShowEdit = isShowEdit;
            updateState();
        }
    }

    public void setShowRevoke(boolean isShowRevoke) {
        if (this.isShowRevoke != isShowRevoke) {
            this.isShowRevoke = isShowRevoke;
            updateState();
        }
    }

    public void setShowEdit(boolean isShowEdit) {
        if (this.isShowEdit != isShowEdit) {
            this.isShowEdit = isShowEdit;
            updateState();
        }
    }

    public void setOptionDelegate(QrOptionDelegate optionDelegate) {
        this.optionDelegate = optionDelegate;
    }

    public interface QrOptionDelegate {
        void onEditOptionClicked();

        void onRevokeOptionClicked();

        void onQrOptionClicked();
    }
}
