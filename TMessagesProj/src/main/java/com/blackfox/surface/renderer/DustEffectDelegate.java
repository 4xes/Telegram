package com.blackfox.surface.renderer;

import com.blackfox.surface.renderer.particle.DustRequest;

public interface DustEffectDelegate {
        void onStartEffect(DustRequest dustRequest);
        void onFinishedEffect(DustRequest dustRequest);
}