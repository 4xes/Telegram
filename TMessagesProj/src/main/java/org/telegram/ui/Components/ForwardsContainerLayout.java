package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.SavedMessagesController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.ChatActivity;

import java.util.ArrayList;
import java.util.Objects;

public class ForwardsContainerLayout extends FrameLayout implements ValueAnimator.AnimatorUpdateListener {

    private static final int MAX_FORWARDS = 5;
    private static final long SELECT_ANIMATION_DURATION = 200L;
    private final Theme.ResourcesProvider resourcesProvider;
    private final ChatActivity fragment;
    private final int currentAccount;
    private final RecyclerListView recyclerListView;
    private final Rect rectLocation = new Rect();
    private final int[] location = new int[2];
    private final float[] pos = new float[2];
    private final OvershootInterpolator overshootInterpolator = new OvershootInterpolator(1.1f);
    private final OvershootInterpolator avatarOvershootInterpolator = new OvershootInterpolator(1.4f);
    private final AccelerateDecelerateInterpolator shareInterpolator = new AccelerateDecelerateInterpolator();
    private final ArrayList<TLRPC.Dialog> dialogs;
    private final ArrayList<MessageObject> messages;
    private final int avatarSize = dp(48);
    private final int forwardsHorizontalPadding = dp(12);
    private final int forwardsVerticalPadding = dp(8);
    private final int forwardsHeight = forwardsVerticalPadding + avatarSize + forwardsVerticalPadding;
    private final int forwardsSpacing = dp(12);
    private final int forwardsCorners = dp(32);
    private final int shareRadius = dp(16);
    private final int spaceBetweenShareAndRightForwards = dp(56);
    private final int verticalPaddingBetweenShareAndForwards = dp(18);
    private final int shareAnimationHeightMove = dp(18);
    private final int horizontalPadding = dp(8);
    private final int nameHeight = dp(24);
    private final int verticalPaddingBetweenForwardsAndName = dp(12);
    private final int forwardsWidth;
    private final Path path = new Path();
    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint testPaintCorner = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ChatMessageCell cell;
    private final Rect shadowPad = new Rect();
    private final RectF endRect = new RectF();
    private final RectF forwardsRect = new RectF();
    private final RectF startRect = new RectF();
    private final RectF shareRect = new RectF();
    private final RectF tempRect = new RectF();
    private final Drawable shadow;
    private final boolean debug = false;
    private Button debugButton;
    private float progress = 0f;
    private ValueAnimator animator;
    private int selected = -1;
    private boolean isSending = false;

    private final ForwardsAdapter adapter;

    private NameTextView[] nameViews;

    private final float nameStartAlpha = 0f;
    private final float nameStartScale = 0.7f;
    private final int nameStartTransitionY = dp(6);

    private boolean isDismiss = false;

    Matrix matrix = new Matrix();

    public ForwardsContainerLayout(ChatActivity fragment, @NonNull Context context, int currentAccount, Theme.ResourcesProvider resourcesProvider, ChatMessageCell cell, ArrayList<MessageObject> messages) {
        super(context);

        this.resourcesProvider = resourcesProvider;
        this.currentAccount = currentAccount;
        this.fragment = fragment;
        this.cell = cell;
        this.messages = messages;
        this.dialogs = fetchDialogs();
        this.adapter = new ForwardsAdapter(this.dialogs);
        this.nameViews = new NameTextView[this.dialogs.size()];
        for (int i = 0; i < nameViews.length; i++) {
            String name = getName(this.dialogs.get(i).id);
            if (name != null) {
                NameTextView nameView = new NameTextView(context, name);
                nameView.setAlpha(nameStartAlpha);
                nameView.setTranslationY(nameStartTransitionY);
                nameView.setScaleY(nameStartScale);
                nameView.setScaleX(nameStartScale);
                this.nameViews[i] = nameView;
            }
        }

        fragment.prepareBlur();

        forwardsWidth = calculateForwardsWidth();
        recyclerListView = new ForwardsRecyclerView(context);

        calculateStartRect();
        calculateEndRect(fragment.fragmentView.getMeasuredWidth());

        updateMargins();

        recyclerListView.setAdapter(adapter);

        addView(recyclerListView);

        for (int i = 0; i < nameViews.length; i++) {
            addView(nameViews[i]);
        }

        shadow = Objects.requireNonNull(ContextCompat.getDrawable(context, R.drawable.reactions_bubble_shadow)).mutate();
        shadowPad.left = shadowPad.top = shadowPad.right = shadowPad.bottom = dp(7);
        shadow.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chat_messagePanelShadow, resourcesProvider), PorterDuff.Mode.MULTIPLY));

//        debugButton = new Button(context);
//        debugButton.setText("StartAnimation");
//        debugButton.setOnClickListener(v -> {
//            startAnimation();
//        });
//        addView(debugButton, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT, 16, 200, 0, 0));

        testPaintCorner.setStyle(Paint.Style.STROKE);

        recyclerListView.setOnItemClickListener((view, position) -> onSelected(position));

        setOnTouchListener(new View.OnTouchListener() {


            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    if (!isRunningAnimation()) {
                        computeLocation(recyclerListView);
                        if (!rectLocation.contains((int) event.getX(), (int) event.getY())) {
                            dismiss();
                        }
                    }
                } else if (event.getActionMasked() == MotionEvent.ACTION_OUTSIDE) {
                    if (!isRunningAnimation()) {
                        dismiss();
                    }
                }
                return false;
            }
        });
    }

    public void onSelected(int position) {
        if (isDismiss) {
            return;
        }
        for (int i = 0; i < recyclerListView.getChildCount(); i++) {
            View avatarView =  recyclerListView.getChildAt(i);
            NameTextView nameView = nameViews[i];
            if (selected != position) {
                boolean isSelected = i == position;
                float avatarAlpha = isSelected ? 1.0f: 0.5f;
                float avatarScale = isSelected ? 1.1f: 1.0f;
                avatarView.clearAnimation();
                avatarView.animate()
                        .alpha(avatarAlpha)
                        .scaleX(avatarScale)
                        .scaleY(avatarScale)
                        .setDuration(SELECT_ANIMATION_DURATION)
                        .start();

                if (nameView != null) {
                    float nameAlpha = isSelected ? 1.0f: nameStartAlpha;
                    float nameScale = isSelected ? 1.0f: nameStartScale;
                    float nameTransitionY = isSelected ? 0: nameStartTransitionY;
                    nameView.clearAnimation();
                    nameView.animate()
                            .alpha(nameAlpha)
                            .scaleX(nameScale)
                            .scaleY(nameScale)
                            .translationY(nameTransitionY)
                            .setDuration(SELECT_ANIMATION_DURATION)
                            .start();

                }
            }
        }
        if (selected == position) {
            sendToAndAnimate(position);
        }
        selected = position;
    }

    public void sendToAndAnimate(int position) {
        if (isSending) {
            return;
        }
        isSending = true;
        long did = dialogs.get(position).id;

        int result = SendMessagesHelper.getInstance(currentAccount).sendMessage(new ArrayList<MessageObject>() {{ add(cell.getMessageObject()); }}, did, false,false, true, 0, null);
        if (result == 0) {
            SpannableStringBuilder text;
            int icon;
            int messagesCount = 1;
            if (did == UserConfig.getInstance(UserConfig.selectedAccount).clientUserId) {
                if (messagesCount <= 1) {
                    text = AndroidUtilities.replaceSingleTag(LocaleController.getString(R.string.FwdMessageToSavedMessages), SavedMessagesController::openSavedMessages);
                } else {
                    text = AndroidUtilities.replaceSingleTag(LocaleController.getString(R.string.FwdMessagesToSavedMessages), SavedMessagesController::openSavedMessages);
                }
                icon = R.raw.saved_messages;
            } else {
                if (DialogObject.isChatDialog(did)) {
                    TLRPC.Chat chat = MessagesController.getInstance(UserConfig.selectedAccount).getChat(-did);
                    if (messagesCount <= 1) {
                        text = AndroidUtilities.replaceTags(LocaleController.formatString("FwdMessageToGroup", R.string.FwdMessageToGroup, chat.title));
                    } else {
                        text = AndroidUtilities.replaceTags(LocaleController.formatString("FwdMessagesToGroup", R.string.FwdMessagesToGroup, chat.title));
                    }
                } else {
                    TLRPC.User user = MessagesController.getInstance(UserConfig.selectedAccount).getUser(did);
                    if (messagesCount <= 1) {
                        text = AndroidUtilities.replaceTags(LocaleController.formatString("FwdMessageToUser", R.string.FwdMessageToUser, UserObject.getFirstName(user)));
                    } else {
                        text = AndroidUtilities.replaceTags(LocaleController.formatString("FwdMessagesToUser", R.string.FwdMessagesToUser, UserObject.getFirstName(user)));
                    }
                }
                icon = R.raw.forward;
            }
            Bulletin bulletin = BulletinFactory.of(fragment)
                    .createSimpleBulletin(
                            icon,
                            text
                    );

            Bulletin.LottieLayout  layout = ((Bulletin.LottieLayout) bulletin.getLayout());
            RLottieImageView imageView = layout.imageView;

            layout.postDelayed(() -> {
                layout.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            }, 300);
            bulletin.show();
        }
        dismiss();
    }

    private void calculateStartRect() {
        cell.getShareButtonRect(startRect);
        cell.getLocationOnScreen(location);
        startRect.offset(location[0], location[1]);
    }

    private void computeLocation(View view) {
        view.getLocationInWindow(location);
        rectLocation.set(location[0], location[1], location[0] + view.getMeasuredWidth(), location[1] + view.getMeasuredHeight());
    }

    private int calculateForwardsWidth() {
        int forwardsSpacings = Math.max(0, dialogs.size() - 1) * forwardsSpacing;
        return forwardsHorizontalPadding + dialogs.size() * avatarSize + forwardsSpacings + forwardsHorizontalPadding;
    }

    public void startAnimation() {
        if (animator != null) {
            animator.cancel();
        }
        cell.transitionSideButtonParams.drawBackground = false;
        cell.transitionSideButtonParams.drawIcon = false;

        cell.invalidateOutbounds();
        invalidate();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.addUpdateListener(this);
        animator.setDuration(1000L);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                cell.transitionSideButtonParams.drawBackground = true;
                cell.transitionSideButtonParams.drawIcon = true;
                cell.invalidateOutbounds();
            }
        });
        animator.start();
    }

    @Override
    public void onAnimationUpdate(@NonNull ValueAnimator animation) {
        progress = (float) animation.getAnimatedValue();
        if (isDismiss) {
            setAlpha(progress);
        }
        invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        calculateStartRect();
        calculateEndRect(fragment.fragmentView.getMeasuredWidth());
        shader = null;
        getShader();


        updateMargins();
    }

    public void updateMargins() {
        updateMargins(recyclerListView, (int) endRect.width(), (int) endRect.height(), (int) endRect.left, (int) endRect.top);

        float width = fragment.fragmentView.getMeasuredWidth();
        float top = endRect.top - verticalPaddingBetweenForwardsAndName - nameHeight;
        for (int i = 0; i < nameViews.length; i++) {
            NameTextView name = nameViews[i];
            if (name == null) {
                continue;
            }
            float centerX = endRect.left + forwardsHorizontalPadding + (avatarSize * 0.5f) + (avatarSize + forwardsSpacing) * i;
            float widthName = name.calculateWidth();
            float left = Math.max(centerX - widthName / 2f, horizontalPadding);
            float right = Math.min(left + widthName, width - horizontalPadding);
            left = right - widthName;
            updateMargins(name, (int) left, (int) top);
        }
    }

    public void updateMargins(View view, int width, int height, int marginLeft, int marginTop) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
        if (params == null) {
            params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        params.width = width;
        params.height = height;
        params.setMargins(marginLeft, marginTop, 0, 0);
        view.setLayoutParams(params);
    }

    public void updateMargins(View view, int marginLeft, int marginTop) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
        if (params == null) {
            params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        params.setMargins(marginLeft, marginTop, 0, 0);
        view.setLayoutParams(params);
    }

    private void dismiss() {
        if (!isDismiss) {
            isDismiss = true;
            if (!isRunningAnimation()) {
                animate().alpha(0f).setDuration(300L).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        deleteFromParent();
                    }
                }).start();
            } else {
                if (animator != null) {
                    animator.cancel();
                }
                cell.transitionSideButtonParams.drawBackground = false;
                cell.transitionSideButtonParams.drawIcon = false;

                cell.invalidateOutbounds();
                invalidate();
                animator = ValueAnimator.ofFloat(progress, 0f);
                animator.addUpdateListener(this);
                animator.setDuration((long) (300L));
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        cell.transitionSideButtonParams.drawBackground = true;
                        cell.transitionSideButtonParams.drawIcon = true;
                        cell.invalidateOutbounds();
                        deleteFromParent();
                    }
                });
                animator.start();
            }
        }
    }

    private void deleteFromParent() {
        ViewParent parent = getParent();
        if (parent instanceof ViewGroup) {
            try {
                ((ViewGroup) parent).removeView(this);
            } catch (Exception e) {
                //noinspection CallToPrintStackTrace
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            dismiss();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean b = super.dispatchTouchEvent(ev);
        if (ev.getAction() == MotionEvent.ACTION_DOWN && !b) {
            dismiss();
            return true;
        }
        return b;
    }

    private void calculateEndRect(int width) {
        endRect.right = Math.min(startRect.right + spaceBetweenShareAndRightForwards, width - horizontalPadding);
        endRect.left = Math.max(horizontalPadding, endRect.right - forwardsWidth);
        endRect.right = endRect.left + forwardsWidth;

        float relativeEndPositionCenterY = shareRadius + verticalPaddingBetweenShareAndForwards + (forwardsHeight / 2f);
        endRect.top = startRect.centerY() - relativeEndPositionCenterY - (forwardsHeight / 2f);
        endRect.bottom = endRect.top + forwardsHeight;
    }

    private boolean isRunningAnimation() {
        return animator != null && animator.isRunning();
    }

    private boolean isShowed() {
        return progress == 1.0;
    }

    private float getAvatarScaleProgress(int index) {
        float avatarProgress;

        int centerIndex = dialogs.size() / 2;
        if (index == centerIndex) {
            avatarProgress = progressAfterTime(0.05f);
        } else if (index == centerIndex - 1 || index == centerIndex + 1) {
            avatarProgress = progressAfterTime(0.20f);
        } else {
            avatarProgress = progressAfterTime(0.30f);
        }
        return avatarOvershootInterpolator.getInterpolation(avatarProgress);
    }

    private float progressAfterTime(float time) {
        return Math.max(0.0f, progress - time) / (1.0f - time);
    }

    private final Paint gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        calculateStartRect();

//        matrix.reset();
//        matrix.setTranslate(300, 300);
//        Shader shader = getShader();
//        shader.setLocalMatrix(matrix);
//        canvas.drawCircle(300, 300, 100, gradientPaint);

        float cellX = location[0];
        float cellY = location[1];

        calculateEndRect(getMeasuredWidth());
        float shareProgress = interpolateShare(progress);
        evaluateShareRect(shareProgress, startRect.centerX(), startRect.centerY(), shareRadius, shareAnimationHeightMove);

        evaluateForwardsRect(progress, forwardsCorners);

        float forwardsRadius = forwardsRect.height() / 2f;

        float curveProgress = interpolateCurve(progress);
        float curveControlProgress = interpolateCurveControl(progress);

        float forwardTop = forwardsRect.top + forwardsRadius;

        float curveRotationTime = 0.3f;
        float curveMoveProgress = Math.max(0f, curveProgress - curveRotationTime) / (1f - curveRotationTime);

        boolean isShortLeft = shareRect.left < endRect.left + forwardsCorners;
        if (curveProgress < curveRotationTime || isShortLeft) {
            float curveRotationProgress = curveProgress / curveRotationTime;
            float leftForwardsDegree = interpolate(
                    isShortLeft? curveProgress : curveRotationProgress,
                    -90,
                    isShortLeft ? -45 : 0
            );
            pointOnTheCircle(leftForwardsDegree, forwardsRadius, forwardsRect.left + forwardsRadius, forwardTop);
        } else {
            float startLeft = forwardsRect.left + forwardsRadius;
            pos[0] = interpolate(curveMoveProgress, startLeft, Math.max(startLeft, shareRect.left));
            pos[1] = forwardsRect.bottom;
        }
        float leftStartX = pos[0];
        float leftStartY = pos[1];

        float leftShareDegree = interpolate(curveProgress, 0, -180);
        pointOnTheCircle(leftShareDegree, shareRadius, shareRect.centerX(), shareRect.centerY());

        float leftEndX = pos[0];
        float leftEndY = pos[1];

        float leftControlX = interpolate(curveControlProgress, leftStartX, leftEndX);
        float leftControlY = interpolate(curveControlProgress, leftEndY, leftStartY);

        float rightShareDegree = interpolate(curveProgress, 0, 180);
        pointOnTheCircle(rightShareDegree, shareRadius, shareRect.centerX(), shareRect.centerY());
        float rightStartX = pos[0];
        float rightStartY = pos[1];

        boolean isShortRight = shareRect.right > endRect.right - forwardsCorners;
        if (curveProgress < curveRotationTime || isShortRight) {
            float curveRotationProgress = curveProgress / curveRotationTime;
            float rightForwardsDegree = interpolate(
                    isShortRight? curveProgress : curveRotationProgress,
                    90,
                    isShortRight ? 45 : 0
            );
            pointOnTheCircle(rightForwardsDegree, forwardsRadius, forwardsRect.right - forwardsRadius, forwardTop);
        } else {
            float startRight = forwardsRect.right - forwardsRadius;
            pos[0] = interpolate(curveMoveProgress, startRight, Math.min(shareRect.right, startRight));
            pos[1] = forwardsRect.bottom;
        }

        float rightEndX = pos[0];
        float rightEndY = pos[1];

        float rightControlX = interpolate(curveControlProgress, rightEndX, rightStartX);
        float rightControlY = interpolate(curveControlProgress, rightStartY, rightEndY);

        float curveBottomOffset = Math.max(0f, progress - 0.7f) / 0.3f * verticalPaddingBetweenShareAndForwards;

        if (isRunningAnimation()) {
            path.rewind();
            path.moveTo(forwardsRect.centerX(), forwardsRect.centerY());
            path.lineTo(leftStartX, leftStartY);
            path.quadTo(leftControlX, leftControlY, leftEndX, leftEndY - curveBottomOffset);
            path.lineTo(rightStartX, rightStartY - curveBottomOffset);
            path.quadTo(rightControlX, rightControlY, rightEndX, rightEndY);
            path.close();
        }

        if (isRunningAnimation() || isShowed()) {
            shadow.setBounds((int) forwardsRect.left - shadowPad.left, (int) forwardsRect.top - shadowPad.top, (int) forwardsRect.right + shadowPad.right, (int) forwardsRect.bottom + shadowPad.bottom);
            shadow.draw(canvas);
        }

        backgroundPaint.setColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground, resourcesProvider));

//        drawShape(canvas, getButtonPaint(), forwardsRadius);
        drawShape(canvas, forwardsRadius);

        if (isRunningAnimation()) {
            int saveCount = canvas.save();
            boolean drawBackground = cell.transitionSideButtonParams.drawBackground;
            boolean iconDraw = cell.transitionSideButtonParams.drawIcon;
            cell.transitionSideButtonParams.drawBackground = true;
            cell.transitionSideButtonParams.drawIcon = true;
            float cellOffsetY = startRect.centerY() - shareRect.centerY();
            canvas.translate(cellX, cellY - cellOffsetY);

            cell.getShareButtonRect(tempRect);
            float sideButtonRotation = interpolate(shareProgress, 0, -45);

            canvas.rotate(sideButtonRotation, tempRect.centerX(), tempRect.centerY());
            cell.drawSideButton(canvas);
            cell.transitionSideButtonParams.drawBackground = drawBackground;
            cell.transitionSideButtonParams.drawIcon = iconDraw;
            canvas.restoreToCount(saveCount);
        }

        if (!isShowed()) {
            for (int i = 0; i < recyclerListView.getChildCount(); i++) {
                int avatarsSave = canvas.save();
                View avatarView =  recyclerListView.getChildAt(i);

                float scale = getAvatarScaleProgress(i);
                float radius = avatarView.getMeasuredWidth() / 2f;
                float posXInEnd = avatarView.getLeft() + radius;

                float percentXInEnd = posXInEnd / endRect.width();

                float translateToX = forwardsRect.left + (forwardsRect.width() * percentXInEnd);
                float translateToY = forwardsRect.centerY();

                canvas.translate(translateToX - radius, translateToY - radius);
                canvas.scale(scale, scale, radius, radius);
                avatarView.draw(canvas);
                canvas.restoreToCount(avatarsSave);
            }

            if (debugButton != null) {
                int buttonDebug = canvas.save();
                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) debugButton.getLayoutParams();
                canvas.translate(layoutParams.leftMargin, layoutParams.topMargin);
                debugButton.draw(canvas);
                canvas.restoreToCount(buttonDebug);
            }

        } else {
            super.dispatchDraw(canvas);
        }

        if (debug) {
            testPaintCorner.setColor(Color.GREEN);
            canvas.drawRoundRect(endRect, forwardsCorners, forwardsCorners, testPaintCorner);

            testPaintCorner.setColor(Color.RED);
            canvas.drawPath(path, testPaintCorner);
        }

    }

    private Shader shader;

    private Shader getShader() {
        if (shader == null) {
            int colorCenter = Color.TRANSPARENT;
            int colorAround = Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground, resourcesProvider);
            Shader gradientShader =  new RadialGradient(0, 0, shareRadius * 2, new int[]{colorCenter, colorAround }, new float[] {0.0f, 1.0f}, Shader.TileMode.CLAMP);
            gradientShader.setLocalMatrix(matrix);
            gradientPaint.setShader(gradientShader);
            shader = gradientShader;
        }

        return shader;
    }

    private void drawShape(Canvas canvas, float forwardsRadius) {
        if (isRunningAnimation()) {

            canvas.drawPath(path, backgroundPaint);
        }

        if (isRunningAnimation() || isShowed()) {
            canvas.drawRoundRect(forwardsRect, forwardsRadius, forwardsRadius, backgroundPaint);
        }

//        if (isRunningAnimation()) {
//            matrix.reset();
//            matrix.setTranslate(shareRect.centerX(), shareRect.centerY());
//            Shader shader = getShader();
//            shader.setLocalMatrix(matrix);
//            canvas.drawCircle(shareRect.centerX(), shareRect.centerY(), shareRadius, gradientPaint);
//        }
    }

    private Paint getButtonPaint() {
        return getThemedPaint(cell.isSideButtonPressed() ? Theme.key_paint_chatActionBackgroundSelected : Theme.key_paint_chatActionBackground);
    }

    private Paint getGradintPaint() {
        return Theme.chat_actionBackgroundGradientDarkenPaint;
    }

    public boolean hasGradientService() {
        return resourcesProvider != null ? resourcesProvider.hasGradientService() : Theme.hasGradientService();
    }

    private float interpolateShare(float progress) {
        float circleProgress = shareInterpolator.getInterpolation(progress);
        // [0, 0.5, 0]
        if (circleProgress <= 0.5f) {
            return circleProgress * 2f;
        } else {
            return 1f - (circleProgress - 0.5f) * 2f;
        }
    }

    private float interpolateCurve(float progress) {
        return Math.min(1f, progress * 2.f);
    }

    private float interpolateCurveControl(float progress) {
        return Math.min(1f, 0.3f + progress * 4.f);
    }

    private float interpolateForwards(float progress) {
        return overshootInterpolator.getInterpolation(progress);
    }

    private void pointOnTheCircle(float degree, float radius, float cX, float cY) {
        double radians = Math.toRadians(degree);
        pos[0] = (float) (cX + (radius * Math.sin(radians)));
        pos[1] = (float) (cY + (radius * Math.cos(radians)));
    }

    private float interpolate(float fraction, float start, float end) {
        return start + ((end - start) * fraction);
    }

    private void evaluateShareRect(float fraction, float cX, float cY, float shareRadius, float moveHeight) {

        float circleY = cY - (moveHeight * fraction);

        shareRect.left = cX - shareRadius;
        shareRect.top = circleY - shareRadius;
        shareRect.right = cX + shareRadius;
        shareRect.bottom = circleY + shareRadius;
    }

    private void evaluateForwardsRect(float fraction, float maxCorners) {
        float rectFraction = interpolateForwards(fraction);
        float step1LeftEnd = startRect.centerX() - maxCorners;
        float step1RightEnd = startRect.centerX() + maxCorners;

        float timeStep1 = 0.3f;
        if (rectFraction < timeStep1) {
            //Log.e("PROGRESS", "progress1: " + rectFraction);
            float step1Fraction = rectFraction / timeStep1;
            forwardsRect.left = startRect.left + ((step1LeftEnd - startRect.left) * step1Fraction);
            forwardsRect.right = startRect.right + ((step1RightEnd - startRect.right) * step1Fraction);
        } else {
            float step2Fraction = Math.max(0f, rectFraction - timeStep1) / (1.0f - timeStep1);
            //Log.e("PROGRESS", "progress2: " + step2Fraction);
            forwardsRect.left = step1LeftEnd + ((endRect.left - step1LeftEnd) * step2Fraction);
            forwardsRect.right = step1RightEnd + ((endRect.right - step1RightEnd) * step2Fraction);
        }
        forwardsRect.top = startRect.top + ((endRect.top - startRect.top) * rectFraction);
        forwardsRect.bottom = startRect.bottom + ((endRect.bottom - startRect.bottom) * rectFraction);
        //Log.e("PROGRESS", "Rect: " + forwardsRect);
    }

    private ArrayList<TLRPC.Dialog> fetchDialogs() {
        long selfUserId = UserConfig.getInstance(currentAccount).clientUserId;
        ArrayList<TLRPC.Dialog> dialogs = new ArrayList<>();
        LongSparseArray<TLRPC.Dialog> dialogsMap = new LongSparseArray<>();

        ArrayList<TLRPC.Dialog> dialogsForward = MessagesController.getInstance(currentAccount).dialogsForward;
        if (!dialogsForward.isEmpty()) {
            if (dialogsForward.size() >= MAX_FORWARDS) {
                for (int i = 0; i < MAX_FORWARDS; i++) {
                    dialogs.add(dialogsForward.get(i));
                }
                return dialogs;
            } else {
                TLRPC.Dialog dialog = MessagesController.getInstance(currentAccount).dialogsForward.get(0);
                dialogs.add(dialog);
                dialogsMap.put(dialog.id, dialog);
            }
        }
        ArrayList<TLRPC.Dialog> archivedDialogs = new ArrayList<>();
        ArrayList<TLRPC.Dialog> allDialogs = MessagesController.getInstance(currentAccount).getAllDialogs();
        for (int a = 0; a < allDialogs.size(); a++) {
            TLRPC.Dialog dialog = allDialogs.get(a);
            if (!(dialog instanceof TLRPC.TL_dialog)) {
                continue;
            }
            if (dialog.id == selfUserId) {
                continue;
            }
            if (!DialogObject.isEncryptedDialog(dialog.id)) {
                if (DialogObject.isUserDialog(dialog.id)) {
                    if (dialog.folder_id == 1) {
                        archivedDialogs.add(dialog);
                    } else {
                        dialogs.add(dialog);
                        if (dialogs.size() == MAX_FORWARDS) {
                            return dialogs;
                        }
                    }
                    dialogsMap.put(dialog.id, dialog);
                } else {
                    TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialog.id);
                    if (!(chat == null || ChatObject.isNotInChat(chat) || chat.gigagroup && !ChatObject.hasAdminRights(chat) || ChatObject.isChannel(chat) && !chat.creator && (chat.admin_rights == null || !chat.admin_rights.post_messages) && !chat.megagroup)) {
                        if (dialog.folder_id == 1) {
                            archivedDialogs.add(dialog);
                        } else {
                            dialogs.add(dialog);
                            if (dialogs.size() == MAX_FORWARDS) {
                                return dialogs;
                            }
                        }
                        dialogsMap.put(dialog.id, dialog);
                    }
                }
            }
        }
        if (dialogs.size() < MAX_FORWARDS) {
            int size = MAX_FORWARDS - dialogs.size();
            int addingSize = Math.min(size, archivedDialogs.size());
            for (int i = 0; i < addingSize; i++) {
                dialogs.add(archivedDialogs.get(i));
            }
        }
        dialogs.addAll(archivedDialogs);
        return dialogs;
    }

    private static class SpacingDecoration extends RecyclerView.ItemDecoration {
        private final int spacing;

        public SpacingDecoration(int spacing) {
            this.spacing = spacing;
        }

        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, RecyclerView parent,
                                   RecyclerView.State state) {
            if (parent.getAdapter() != null) {
                int dataCount= parent.getAdapter().getItemCount();
                int  viewPosition = parent.getChildAdapterPosition(view);
                outRect.set(0, 0, (viewPosition != dataCount - 1) ? spacing: 0, 0);
            }
        }
    }

    private static class ForwardsRecyclerView extends RecyclerListView {
        public ForwardsRecyclerView(Context context) {
            super(context);
            setClipChildren(false);
            setClipToPadding(false);
            setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
            final int horizontalPadding = dp(12);
            final int verticalPadding = dp(8);
            addItemDecoration(new SpacingDecoration(dp(12)));
            setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
        }

        @Override
        public boolean drawChild(Canvas canvas, View child, long drawingTime) {
            return super.drawChild(canvas, child, drawingTime);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            return super.dispatchTouchEvent(ev);
        }
    }

    public Paint getThemedPaint(String paintKey) {
        Paint paint = resourcesProvider != null ? resourcesProvider.getPaint(paintKey) : null;
        return paint != null ? paint : Theme.getThemePaint(paintKey);
    }

    private class ForwardsAdapter extends RecyclerView.Adapter<ForwardViewHolder> {

        private final ArrayList<TLRPC.Dialog> dialogs;

        public ForwardsAdapter(ArrayList<TLRPC.Dialog> dialogs) {
            this.dialogs = dialogs;
        }

        @NonNull
        @Override
        public ForwardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            BackupImageView imageView = new BackupImageView(parent.getContext());
            int size = avatarSize;
            imageView.setRoundRadius(size / 2);
            ViewGroup.LayoutParams layoutParams = new LayoutParams(size, size);
            imageView.setLayoutParams(layoutParams);
            return new ForwardViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull ForwardViewHolder holder, int position) {
            holder.bind(currentAccount, dialogs.get(position).id);
        }

        @Override
        public long getItemId(int position) {
            return super.getItemId(position);
        }

        @Override
        public int getItemCount() {
            return dialogs.size();
        }
    }

    private static class ForwardViewHolder extends RecyclerView.ViewHolder {

        private final BackupImageView imageView;

        private final AvatarDrawable avatarDrawable = new AvatarDrawable() {
            @Override
            public void invalidateSelf() {
                super.invalidateSelf();
                imageView.invalidate();
            }
        };

        public ForwardViewHolder(BackupImageView imageView) {
            super(imageView);
            this.imageView = imageView;
        }

        public void bind(int currentAccount, long uid) {
            TLRPC.User user;
            if (DialogObject.isUserDialog(uid)) {
                user = MessagesController.getInstance(currentAccount).getUser(uid);
                avatarDrawable.setInfo(currentAccount, user);
                if (UserObject.isUserSelf(user)) {
                    avatarDrawable.setAvatarType(AvatarDrawable.AVATAR_TYPE_SAVED);
                    imageView.setImage(null, null, avatarDrawable, user);
                } else {
                    imageView.setForUserOrChat(user, avatarDrawable);
                }
            } else {
                TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-uid);
                avatarDrawable.setInfo(currentAccount, chat);
                imageView.setForUserOrChat(chat, avatarDrawable);
            }
        }

    }

    @Nullable
    private String getName(long uid) {
        TLRPC.User user;
        if (DialogObject.isUserDialog(uid)) {
            user = MessagesController.getInstance(currentAccount).getUser(uid);
            if (UserObject.isUserSelf(user)) {
                return LocaleController.getString(R.string.SavedMessages);
            } else {
                if (user != null) {
                    return ContactsController.formatName(user.first_name, user.last_name);
                } else {
                    return null;
                }
            }
        } else {
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-uid);
            if (chat != null) {
                return chat.title;
            } else {
                return null;
            }
        }
    }

    private class NameTextView extends View {

        private final int horizontalPadding = dp(8);
        private final String text;

        public NameTextView(@NonNull Context context, String text) {
            super(context);
            this.text = text;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float radius = getMeasuredHeight() / 2f;

            RectF rect = AndroidUtilities.rectTmp;
            rect.left = 0f;
            rect.top = 0f;
            rect.right = getMeasuredWidth();
            rect.bottom = getMeasuredHeight();

            if (fragment.themeDelegate != null) {
                fragment.themeDelegate.applyServiceShaderMatrix(ForwardsContainerLayout.this.getMeasuredWidth(), ForwardsContainerLayout.this.getMeasuredHeight(), 0, 0);
            }

            rect.offset(getX(), getY());
            boolean isDrawBlur = fragment.drawBlurRound(canvas, rect, getX(), getY(), radius, radius);
            rect.offset(-getX(), -getY());
            if (!isDrawBlur) {
                Paint backgroundPaint = getThemedPaint(Theme.key_paint_chatActionBackground);

                canvas.drawRoundRect(rect, radius, radius, backgroundPaint);
                if (hasGradientService()) {
                    Paint darkenBackgroundPaint = getThemedPaint(Theme.key_paint_chatActionBackgroundDarken);
                    canvas.drawRoundRect(rect, radius, radius, darkenBackgroundPaint);
                }
            }

            Rect textRect = AndroidUtilities.rectTmp2;
            TextPaint textPaint = (TextPaint) getThemedPaint(Theme.key_paint_chatActionText);
            textPaint.getTextBounds(text, 0, text.length(), textRect);

            float y = ((getMeasuredHeight() / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)) ;

            canvas.drawText(text, horizontalPadding + rect.left, y, textPaint);
            testPaintCorner.setColor(Color.YELLOW);
            testPaintCorner.setAlpha(1);
            rect.left = 0f;
            rect.top = 0f;
            rect.right = getMeasuredWidth();
            rect.bottom = getMeasuredHeight();
            rect.inset(dp(1), dp(1));
            canvas.drawRoundRect(rect, radius, radius, testPaintCorner);
        }

        private TextPaint getTextPaint() {
            return (TextPaint) getThemedPaint(Theme.key_paint_chatActionText);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = calculateWidth();

            super.onMeasure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(nameHeight, MeasureSpec.EXACTLY)
            );
        }

        public int calculateWidth() {
            return (int) (horizontalPadding + getTextPaint().measureText(text) + horizontalPadding);
        }
    }

}
