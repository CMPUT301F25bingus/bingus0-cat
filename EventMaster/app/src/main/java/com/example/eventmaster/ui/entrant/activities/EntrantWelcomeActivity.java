package com.example.eventmaster.ui.entrant.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.R;

/**
 * Welcome screen for entrants with animated welcome message.
 * After animation completes, automatically navigates to Browse Events page.
 */
public class EntrantWelcomeActivity extends AppCompatActivity {

    private static final long DELAY_BEFORE_NAVIGATION = 500; // 0.5 seconds after animation

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entrant_activity_welcome);

        View welcomeContainer = findViewById(R.id.welcomeContainer);
        View textWelcome = findViewById(R.id.textWelcome);
        View textEntrant = findViewById(R.id.textEntrant);
        View iconPerson = findViewById(R.id.iconPerson);

        // Start animation after a brief delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startWelcomeAnimation(welcomeContainer, textWelcome, textEntrant, iconPerson);
        }, 300);
    }

    private void startWelcomeAnimation(View container, View welcomeText, View entrantText, View icon) {
        // Scale animation for container (pulse effect)
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(container, "scaleX", 0.8f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(container, "scaleY", 0.8f, 1.0f);
        scaleX.setDuration(600);
        scaleY.setDuration(600);
        scaleX.setInterpolator(new DecelerateInterpolator());
        scaleY.setInterpolator(new DecelerateInterpolator());

        // Fade in and slide up for "WELCOME" text
        ObjectAnimator welcomeAlpha = ObjectAnimator.ofFloat(welcomeText, "alpha", 0f, 1f);
        ObjectAnimator welcomeY = ObjectAnimator.ofFloat(welcomeText, "translationY", 30f, 0f);
        welcomeAlpha.setDuration(500);
        welcomeY.setDuration(500);
        welcomeAlpha.setStartDelay(200);
        welcomeY.setStartDelay(200);
        welcomeAlpha.setInterpolator(new DecelerateInterpolator());
        welcomeY.setInterpolator(new DecelerateInterpolator());

        // Fade in and slide up for "Entrant!" text
        ObjectAnimator entrantAlpha = ObjectAnimator.ofFloat(entrantText, "alpha", 0f, 1f);
        ObjectAnimator entrantY = ObjectAnimator.ofFloat(entrantText, "translationY", 30f, 0f);
        entrantAlpha.setDuration(500);
        entrantY.setDuration(500);
        entrantAlpha.setStartDelay(400);
        entrantY.setStartDelay(400);
        entrantAlpha.setInterpolator(new DecelerateInterpolator());
        entrantY.setInterpolator(new DecelerateInterpolator());

        // Scale and fade in for icon
        ObjectAnimator iconAlpha = ObjectAnimator.ofFloat(icon, "alpha", 0f, 1f);
        ObjectAnimator iconScaleX = ObjectAnimator.ofFloat(icon, "scaleX", 0.5f, 1.0f);
        ObjectAnimator iconScaleY = ObjectAnimator.ofFloat(icon, "scaleY", 0.5f, 1.0f);
        iconAlpha.setDuration(600);
        iconScaleX.setDuration(600);
        iconScaleY.setDuration(600);
        iconAlpha.setStartDelay(600);
        iconScaleX.setStartDelay(600);
        iconScaleY.setStartDelay(600);
        iconAlpha.setInterpolator(new DecelerateInterpolator());
        iconScaleX.setInterpolator(new DecelerateInterpolator());
        iconScaleY.setInterpolator(new DecelerateInterpolator());

        // Play all animations together
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(
                scaleX, scaleY,
                welcomeAlpha, welcomeY,
                entrantAlpha, entrantY,
                iconAlpha, iconScaleX, iconScaleY
        );

        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Navigate to Browse Events after animation completes
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    navigateToBrowseEvents();
                }, DELAY_BEFORE_NAVIGATION);
            }
        });

        animatorSet.start();
    }

    private void navigateToBrowseEvents() {
        Intent intent = new Intent(this, EventListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
