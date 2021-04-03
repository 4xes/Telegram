package org.telegram.ui.Animations;

import static org.telegram.ui.Animations.InterpolatorData.*;

public enum Parameter {
    X("X Position", false),
    Y("Y Position", false),
    Bubble("Bubble shape", false),
    Scale("scale", false),
    Color("Color change", false),
    TimeAppears("Time appears", false),
    SendMsg("Send Message",true),
    OpenChat("Open Chat",true),
    JumpToMsg("Jump to Message",true);

    final String title;
    final boolean hasDuration;

    Parameter(String title, boolean hasDuration) {
        this.title = title;
        this.hasDuration = hasDuration;
    }

    public float defaultProgressionTop(AnimationType type) {
        return DEFAULT_PROGRESSION_TOP;
    }

    public float defaultProgressionBottom(AnimationType type) {
        if (type == AnimationType.Background) {
            if (this == OpenChat) {
                return 0.16f;
            }
        }
        return DEFAULT_PROGRESSION_BOTTOM;
    }

    public float defaultTimeStart(AnimationType type) {
        if (type == AnimationType.Emoji) {
            if (this == Scale || this == TimeAppears) {
                return 0.17f;
            }
        }
        return DEFAULT_TIME_START;
    }

    public float defaultTimeEnd(AnimationType type) {
        if (type == AnimationType.Background) {
            if (this == OpenChat) {
                return 1f;
            }
        }
        if (this == Parameter.Y) {
            return 1f;
        }
        return DEFAULT_TIME_END;
    }

}
