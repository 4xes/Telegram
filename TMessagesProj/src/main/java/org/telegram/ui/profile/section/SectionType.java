package org.telegram.ui.profile.section;

import org.telegram.messenger.R;

public enum SectionType {
    Stop(
            R.drawable.profile_block,
            R.string.Stop
    ),
    CallViaTelegram(
            R.drawable.profile_call,
            R.string.Call,
            R.string.CallViaTelegram
    ),
    Gift(
            R.drawable.profile_gift,
            R.string.ProfileGift
    ),
    Join(
            R.drawable.profile_join,
            R.string.ProfileJoin
    ),
    Leave(
            R.drawable.profile_leave,
            R.string.ProfileLeave
    ),
    LiveStream(
            R.drawable.profile_live_stream,
            R.string.ProfileLiveStream
    ),
    Message(
            R.drawable.profile_message,
            R.string.Message,
            R.string.AccDescrOpenChat
    ),
    Discuss(
            R.drawable.profile_message,
            R.string.Discussion,
            R.string.ViewDiscussion
    ),
    Mute(
            R.drawable.profile_mute,
            R.string.Mute
    ),
    Report(
            R.drawable.profile_report,
            R.string.ProfileReport
    ),
    Share(
            R.drawable.profile_share,
            R.string.ProfileShare
    ),
    Story(
            R.drawable.profile_story,
            R.string.Story
    ),
    Unmute(
            R.drawable.profile_unmute,
            R.string.Unmute
    ),
    VideoCall(
            R.drawable.profile_video,
            R.string.ProfileVideo,
            R.string.VideoCallViaTelegram
    ),
    ChangeProfilePicture(
            R.drawable.profile_camera,
            R.string.AccDescrChangeProfilePicture,
            R.string.AccDescrChangeProfilePicture,
            R.raw.camera_outline
    );

    public final int iconRes;
    public final int text;
    public final int rawAnimation;
    public final int contentDescription;

    SectionType(int iconRes, int text) {
        this.iconRes = iconRes;
        this.text = text;
        this.contentDescription = 0;
        this.rawAnimation = 0;
    }

    SectionType(int iconRes, int text, int contentDescription) {
        this.iconRes = iconRes;
        this.text = text;
        this.contentDescription = contentDescription;
        this.rawAnimation = 0;
    }

    SectionType(int iconRes, int text, int contentDescription, int rawAnimation) {
        this.iconRes = iconRes;
        this.text = text;
        this.contentDescription = contentDescription;
        this.rawAnimation = rawAnimation;
    }
}
