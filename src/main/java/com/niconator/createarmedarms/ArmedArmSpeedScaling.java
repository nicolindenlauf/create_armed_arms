package com.niconator.createarmedarms;

final class ArmedArmSpeedScaling {
    private static final float MAX_EFFECTIVE_SPEED = 256.0f;

    private ArmedArmSpeedScaling() {
    }

    static float baseRpm() {
        return (float) CreateArmedArmsConfig.ANIMATION_BASE_RPM.get().doubleValue();
    }

    static int adjustedTickCount(int baseTicks, float speed, int minimumTicks) {
        float safeSpeed = Math.max(1.0f, Math.abs(speed));
        int scaledTicks = Math.round(baseTicks * baseRpm() / safeSpeed);
        return Math.max(minimumTicks, scaledTicks);
    }

    static float progressStep(float speed) {
        float clampedSpeed = Math.min(MAX_EFFECTIVE_SPEED, Math.abs(speed));
        return clampedSpeed / (baseRpm() * 32.0f);
    }

    static double poseChaseSpeed(float speed) {
        float clampedSpeed = Math.min(MAX_EFFECTIVE_SPEED, Math.abs(speed));
        return Math.max(0.05, clampedSpeed / (baseRpm() * 16.0f));
    }
}
