package org.telegram.ui.Reactions;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class ChatMessageReactionsHelper {

    private final Context context;
    private final Theme.ResourcesProvider resourcesProvider;

    public final static String singleEmoji = "    ";
    public final static String doubleEmoji = "         ";
    private final int privateSingleWidth = (int) Math.ceil(Theme.chat_timePaint.measureText(singleEmoji));
    private final int privateDoubleWidth = (int) Math.ceil(Theme.chat_timePaint.measureText(doubleEmoji));
    private final int privateReactionSize = AndroidUtilities.dp(14);

    private final static Paint debugPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final static Paint backPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final static Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    {
        backPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(AndroidUtilities.dp(12));
        textPaint.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(AndroidUtilities.dp(1.5f));
    }
    private final RectF tempRect = new RectF();

    private final RectF size = new RectF();

    private MessageObject message;

    private final ArrayList<TLRPC.TL_reactionCount> counts = new ArrayList<>(11);
    private final HashMap<String, Reaction> reactions = new HashMap<>(11);

    static class Reaction {
        public RectF rect = new RectF();
        public RectF emoji = new RectF();
        public final String reaction;
        public int count;
        public TLRPC.TL_reactionCount object;
        public String text;
        private StaticLayout textLayout;
        public boolean isSelected = false;

        public Reaction(String reaction) {
            this.reaction = reaction;
        }
    }

    static class Style {
        public int background;
        public int selectedStroke;
        public int text;

        public Style(int background, int selectStroke, int text) {
            this.background = background;
            this.selectedStroke = selectStroke;
            this.text = text;
        }
    }

    private final int reactionMinWidth = AndroidUtilities.dp(53);
    private final int heightButton = AndroidUtilities.dp(26);
    private final int spacing = AndroidUtilities.dp(6);
    private final int corner = AndroidUtilities.dp(13);
    private final int paddingRight = AndroidUtilities.dp(9);
    private final int textLeft = AndroidUtilities.dp(31);
    private final int emojiLeft = AndroidUtilities.dp(7);
    private final int emojiTop = AndroidUtilities.dp(3);
    private final int emojiSize = AndroidUtilities.dp(20);

    private final int topPadding = AndroidUtilities.dp(8);
    private final int leftPadding = AndroidUtilities.dp(10);
    private final int rightPadding = AndroidUtilities.dp(10);
    private final int bottomPadding = AndroidUtilities.dp(9);

    public static final Style bubbleTheme = new Style(
            0x1A378DD1,
            0xcc368dd0,
            0xff378DD1
    );

    public static final Style backgroundTheme = new Style(
            0x3B214119,
            Color.WHITE,
            Color.WHITE
    );

    private Style style = bubbleTheme;

    private final ChatMessageCell cell;

    public ChatMessageReactionsHelper(Context context, Theme.ResourcesProvider resourcesProvider, ChatMessageCell cell) {
        this.context = context;
        this.resourcesProvider = resourcesProvider;
        this.cell = cell;

        debugPaint.setColor(Color.CYAN);
        debugPaint.setAlpha(20);
        debugPaint.setStyle(Paint.Style.FILL);

    }

    private final Comparator<TLRPC.TL_reactionCount> comparator = (o1, o2) -> Integer.compare(o2.count, o1.count);

    public void setCurrentMessage(MessageObject currentMessage) {
        message = currentMessage;
        counts.clear();
        if (message != null) {
            style = message.shouldDrawWithoutBackground() ? backgroundTheme : bubbleTheme;
            ArrayList<TLRPC.TL_reactionCount> messageCounts = message.getReactionCounts();
            if (messageCounts != null) {
                counts.addAll(messageCounts);
                Collections.sort(counts, comparator);

                calculateReactions();
            }
        }
    }

    private int measureText(String text) {
        return (int) Math.ceil(textPaint.measureText(text));
    }

    public void drawReactions(final Canvas canvas, float alpha) {
        if (message == null || DialogObject.isUserDialog(message.getDialogId())) {
            return;
        }
        float left = getOffsetX();
        float top = getOffsetY();

        int saveTranslate = canvas.save();
        canvas.translate(
                left,
                top);
        canvas.drawRect(size, debugPaint);

        for (int i = 0; i < counts.size(); i++) {
            TLRPC.TL_reactionCount count = counts.get(i);
            Reaction reaction = reactions.get(count.reaction);
            if (reaction != null) {
                drawReaction(canvas, reaction);
            }
        }

        canvas.restoreToCount(saveTranslate);
    }

    private float getOffsetX() {
        if (message == null || DialogObject.isUserDialog(message.getDialogId())) {
            return 0f;
        }
        float leftOffset = 0f;
        if (!message.shouldDrawWithoutBackground()) {
            leftOffset = cell.getDrawablePaddingLeft();
        }
        return cell.getBackgroundDrawableLeft() + leftOffset;
    }

    private float getOffsetY() {
        if (message == null || DialogObject.isUserDialog(message.getDialogId())) {
            return 0f;
        }
        float bottomOffset = 0f;
        if (!message.shouldDrawWithoutBackground()) {
            bottomOffset = cell.getBackgroundDrawableBottom() - cell.getTimeY();
        }
        return cell.getBackgroundDrawableBottom() - getHeight() - bottomOffset;
    }

    public TLRPC.TL_reactionCount isTapReaction(float x, float y) {
        float left = getOffsetX();
        float top = getOffsetY();

        for (int i = 0; i < counts.size(); i++) {
            TLRPC.TL_reactionCount count = counts.get(i);
            Reaction reaction = reactions.get(count.reaction);
            if (reaction != null) {
                if (reaction.rect.contains(x - left, y - top)) {
                    return reaction.object;
                }
            }
        }
        return null;
    }

    public float[] getPositionByCount(TLRPC.TL_reactionCount count) {
        float left = getOffsetX();
        float top = getOffsetY();

        Reaction reaction = reactions.get(count.reaction);
        if (reaction != null) {
            return new float[] {left + reaction.rect.left, top + reaction.rect.top};
        }
        return null;
    }

    public RectF getPositionInPrivate() {
        RectF tempRect = new RectF();
        tempRect.right = cell.getDrawTimeX();
        tempRect.left = tempRect.right - privateReactionSize;
        tempRect.top = cell.getDrawTimeY();
        tempRect.bottom = tempRect.top + privateReactionSize;
        if (counts.size() > 1) {
            tempRect.left += privateDoubleWidth;
            tempRect.right += privateDoubleWidth;

        }
        if (counts.size() == 1) {
            if (counts.get(0).count > 1) {
                tempRect.left += privateDoubleWidth;
                tempRect.right += privateDoubleWidth;
            } else {
                tempRect.right += privateSingleWidth;
                tempRect.left += privateSingleWidth;
            }
        }
        return tempRect;
    }

    public void drawReaction(Canvas canvas, Reaction reaction) {
        backPaint.setColor(style.background);
        canvas.drawRoundRect(reaction.rect, corner, corner, backPaint);

        if (reaction.isSelected) {
            strokePaint.setColor(style.selectedStroke);
            canvas.drawRoundRect(reaction.rect, corner, corner, strokePaint);
        }
        ImageReceiver image = getImage(reaction.reaction);
        image.setImageCoords(
                reaction.rect.left + emojiLeft,
                reaction.rect.top + emojiTop,
                emojiSize,
                emojiSize
        );
        image.setAlpha(1f);
        image.draw(canvas);

        int textHeight = reaction.textLayout.getHeight();
        if (reaction.textLayout != null) {
            canvas.save();
            canvas.translate(reaction.rect.left + textLeft, reaction.rect.top + (reaction.rect.height() - textHeight) / 2f + 1);
            textPaint.setColor(style.text);
            reaction.textLayout.draw(canvas);
            canvas.restore();
        }
    }

    public void calculateReactions() {
        float x = 0f;
        int measureWidth = 0;
        int measureHeight = 0;

        for (int i = 0; i < counts.size(); i++) {
            final TLRPC.TL_reactionCount count = counts.get(i);
            Reaction reaction = reactions.get(count.reaction);
            if (reaction == null) {
                reactions.put(count.reaction, reaction = new Reaction(count.reaction));
            }
            if (reaction.count != count.count) {
                reaction.text = LocaleController.formatShortNumber(count.count);
                reaction.textLayout = new StaticLayout(reaction.text, textPaint, AndroidUtilities.dp(100), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0, false);
                reaction.count = count.count;
            }
            reaction.object = count;
            reaction.isSelected = count.chosen;
            if (i % 4 == 0) {
                x = leftPadding;
            }
            reaction.rect.left = x;
            int contentWidth = Math.max(reactionMinWidth, textLeft + measureText(reaction.text) + paddingRight);
            reaction.rect.top = topPadding + ((float) (heightButton + spacing) * (float) (i / 4));
            reaction.rect.right = x + contentWidth;
            reaction.rect.bottom = reaction.rect.top + heightButton;
            x = reaction.rect.right + spacing;
            if (measureWidth < reaction.rect.right) {
                measureWidth = (int) reaction.rect.right;
            }
            if (measureHeight < reaction.rect.bottom) {
                measureHeight = (int) reaction.rect.bottom;
            }
        }
        if (measureWidth > 0) {
            measureWidth += rightPadding;
        }
        if (measureHeight > 0) {
            measureHeight += bottomPadding;
        }
        size.set(0, 0 , measureWidth, measureHeight);
    }

    public int getWidth() {
        if (message == null || DialogObject.isUserDialog(message.getDialogId())) {
            return 0;
        }

        return (int) size.width();
    }

    public boolean hasButtons() {
        return counts.size() > 0;
    }

    public int getHeight() {
        if (message == null || DialogObject.isUserDialog(message.getDialogId())) {
            return 0;
        }
        if (counts.size() == 0) {
            return 0;
        }
        return (int) size.height();
    }

    public boolean hideChoose = false;

    public void drawPrivateReactions(final Canvas canvas, final float timeX, final float timeY) {
        if (message == null || !DialogObject.isUserDialog(message.getDialogId())) {
            return;
        }
        ArrayList<TLRPC.TL_reactionCount> counts = message.getReactionCounts();
        if (counts == null) {
            return;
        }
        tempRect.right = timeX;
        tempRect.left = tempRect.right - privateReactionSize;
        tempRect.top = timeY;
        tempRect.bottom = tempRect.top + privateReactionSize;
        if (counts.size() > 1) {
            tempRect.left += privateDoubleWidth;
            tempRect.right += privateDoubleWidth;
            if (!counts.get(0).chosen || !hideChoose) {
                drawPrivateReaction(canvas, counts.get(0).reaction, 0, 1f, 1f);
            }
            if (!counts.get(0).chosen || !hideChoose) {
                drawPrivateReaction(canvas, counts.get(1).reaction, 1, 1f, 1f);
            }
        }
        if (counts.size() == 1) {
            if (counts.get(0).count > 1) {
                tempRect.left += privateDoubleWidth;
                tempRect.right += privateDoubleWidth;
                if (!counts.get(0).chosen || !hideChoose) {
                    drawPrivateReaction(canvas, counts.get(0).reaction, 0, 1f, 1f);
                }
                drawPrivateReaction(canvas, counts.get(0).reaction, 1, 1f, 1f);
            } else {
                tempRect.right += privateSingleWidth;
                tempRect.left += privateSingleWidth;
                if (!counts.get(0).chosen || !hideChoose) {
                    drawPrivateReaction(canvas, counts.get(0).reaction, 0, 1f, 1f);
                }
            }
        }
    }

    public void drawPrivateReactions(final Canvas canvas, ArrayList<TLRPC.TL_reactionCount> counts, final float timeX, final float timeY, float alpha, float scale) {
        tempRect.right = timeX;
        tempRect.left = tempRect.right - privateReactionSize;
        tempRect.top = timeY;
        tempRect.bottom = tempRect.top + privateReactionSize;
        if (counts.size() > 1) {
            tempRect.left += privateDoubleWidth;
            tempRect.right += privateDoubleWidth;
            if (counts.get(0).chosen) {
                drawPrivateReaction(canvas, counts.get(0).reaction, 0, alpha, scale);
            }
        }
        if (counts.size() == 1) {
            if (counts.get(0).count > 1) {
                tempRect.left += privateDoubleWidth;
                tempRect.right += privateDoubleWidth;

                if (counts.get(0).chosen) {
                    drawPrivateReaction(canvas, counts.get(0).reaction, 0, alpha, scale);
                }
            } else {
                tempRect.right += privateSingleWidth;
                tempRect.left += privateSingleWidth;
                if (counts.get(0).chosen) {
                    drawPrivateReaction(canvas, counts.get(0).reaction, 0, alpha, scale);
                }
            }
        }
    }

    public int getPrivateReactionsCount(final MessageObject object) {
        if (object == null || !DialogObject.isUserDialog(object.getDialogId())) {
            return 0;
        }
        ArrayList<TLRPC.TL_reactionCount> counts = object.getReactionCounts();
        if (counts == null) {
            return 0;
        }
        if (counts.size() > 1) {
            return 2;
        }
        if (counts.size() == 1) {
            return counts.get(0).count;
        }
        return 0;
    }

    public void drawPrivateReaction(final Canvas canvas, final String reaction, final int i, float alpha, float scale) {
        float offset = i * (privateReactionSize);
        tempRect.left -= offset;
        tempRect.right -= offset;
        int restoreCount = -1;
        if (scale != 1f) {
            restoreCount = canvas.save();
            canvas.scale(scale, scale, tempRect.centerX(), tempRect.centerY());
        }
        ImageReceiver imageReceiver = getImage(reaction);
        imageReceiver.setImageCoords(tempRect.left, tempRect.top, tempRect.width(), tempRect.height());
        imageReceiver.setAlpha(alpha);
        imageReceiver.draw(canvas);
        if (restoreCount != -1) {
            canvas.restoreToCount(restoreCount);
        }
    }

    private static final Map<String, ImageReceiver> images = new HashMap<>();

    private static ImageReceiver getImage(String reaction) {
        ImageReceiver imageReceiver = images.get(reaction);
        if (imageReceiver == null) {
            images.put(reaction, imageReceiver = new ImageReceiver());
        }
        TLRPC.Document document = AccountInstance.getInstance(UserConfig.selectedAccount).getReactionsController().getStaticIcon(reaction);
        if (document != null) {
            setDocument(imageReceiver, document);
        }
        return imageReceiver;
    }

    private static void setDocument(ImageReceiver imageReceiver, TLRPC.Document document) {
        imageReceiver.setImage(ImageLocation.getForDocument(document), "32_32", null, "webp",null, 0);
    }

    public static void updateDocument(String reaction, TLRPC.Document document) {
        ImageReceiver imageReceiver = images.get(reaction);
        if (imageReceiver != null) {
            images.put(reaction, imageReceiver = new ImageReceiver());
            setDocument(imageReceiver, document);
        }
    }
}
