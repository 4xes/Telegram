package com.blackfox.surface.renderer;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;


public class DustEffect {
    public static boolean supports() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static boolean startDustEffect(int id, View view, DustEffectDelegate delegate) {
        if (view == null || delegate == null) {
            return false;
        }

        if (supports()) {
            ViewGroup rootView = getRootView(view);
            if (rootView == null) {
                return false;
            }
            DustSurfaceView dustSurfaceView = rootView.findViewById(R.id.dust_renderer_view);
            if (dustSurfaceView == null) {
                dustSurfaceView = new DustSurfaceView(rootView.getContext());

                dustSurfaceView.setId(R.id.dust_renderer_view);
                rootView.addView(dustSurfaceView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
            dustSurfaceView.addViewRequest(view, delegate);
            return true;
        }
        return false;
    }

    private static ViewGroup getRootView(View view) {
        Activity activity = AndroidUtilities.findActivity(view.getContext());
        if (activity == null) {
            return null;
        }
        View rootView = activity.findViewById(android.R.id.content).getRootView();
        if (!(rootView instanceof ViewGroup)) {
            return null;
        }
        return (ViewGroup) rootView;
    }

}