package com.example.watacrab.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.example.watacrab.R;

public class WaterProgressView extends View {
    private Paint wavePaint;
    private Path wavePath;
    private float waveOffset = 0;
    private float progress = 0;
    private ValueAnimator waveAnimator;

    public WaterProgressView(Context context) {
        super(context);
        init();
    }

    public WaterProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WaterProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        wavePaint = new Paint();
        wavePaint.setColor(getResources().getColor(R.color.blue1));
        wavePaint.setStyle(Paint.Style.FILL);
        wavePaint.setAntiAlias(true);

        wavePath = new Path();

        // Start wave animation
        startWaveAnimation();
    }

    private void startWaveAnimation() {
        waveAnimator = ValueAnimator.ofFloat(0, 1);
        waveAnimator.setDuration(2000);
        waveAnimator.setRepeatCount(ValueAnimator.INFINITE);
        waveAnimator.setInterpolator(new LinearInterpolator());
        waveAnimator.addUpdateListener(animation -> {
            waveOffset = (float) animation.getAnimatedValue() * getWidth();
            invalidate();
        });
        waveAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        float waterHeight = height * (1 - progress);

        // Draw background
        canvas.drawColor(Color.parseColor("#E0E0E0"));

        // Draw wave
        wavePath.reset();
        wavePath.moveTo(0, waterHeight);

        float waveLength = width / 2;
        float amplitude = 20;
        for (int i = 0; i <= width; i++) {
            float x = i;
            float y = waterHeight + (float) (amplitude * Math.sin((x + waveOffset) * 2 * Math.PI / waveLength));
            wavePath.lineTo(x, y);
        }

        wavePath.lineTo(width, height);
        wavePath.lineTo(0, height);
        wavePath.close();

        canvas.drawPath(wavePath, wavePaint);
    }

    public void setProgress(float progress) {
        this.progress = Math.max(0, Math.min(1, progress));
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (waveAnimator != null) {
            waveAnimator.cancel();
        }
    }
} 