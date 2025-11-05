package com.example.eventmaster.ui.utils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for time-related operations.
 * Provides methods for formatting timestamps in human-readable formats.
 */
public class TimeUtils {

    /**
     * Converts a timestamp to a relative time string (e.g., "2h ago", "3d ago").
     * 
     * @param timestamp The timestamp to convert
     * @return Human-readable relative time string
     */
    public static String getRelativeTimeString(Date timestamp) {
        if (timestamp == null) {
            return "Unknown";
        }

        long currentTime = System.currentTimeMillis();
        long timestampMillis = timestamp.getTime();
        long diff = currentTime - timestampMillis;

        // If in the future, return "Just now"
        if (diff < 0) {
            return "Just now";
        }

        long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        long days = TimeUnit.MILLISECONDS.toDays(diff);

        if (seconds < 60) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + "m ago";
        } else if (hours < 24) {
            return hours + "h ago";
        } else if (days < 7) {
            return days + "d ago";
        } else if (days < 30) {
            long weeks = days / 7;
            return weeks + "w ago";
        } else if (days < 365) {
            long months = days / 30;
            return months + "mo ago";
        } else {
            long years = days / 365;
            return years + "y ago";
        }
    }

    /**
     * Converts a timestamp to a relative time string with full text.
     * 
     * @param timestamp The timestamp to convert
     * @return Human-readable relative time string with full words
     */
    public static String getRelativeTimeStringFull(Date timestamp) {
        if (timestamp == null) {
            return "Unknown";
        }

        long currentTime = System.currentTimeMillis();
        long timestampMillis = timestamp.getTime();
        long diff = currentTime - timestampMillis;

        if (diff < 0) {
            return "Just now";
        }

        long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        long days = TimeUnit.MILLISECONDS.toDays(diff);

        if (seconds < 60) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        } else if (hours < 24) {
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        } else if (days < 7) {
            return days + (days == 1 ? " day ago" : " days ago");
        } else if (days < 30) {
            long weeks = days / 7;
            return weeks + (weeks == 1 ? " week ago" : " weeks ago");
        } else if (days < 365) {
            long months = days / 30;
            return months + (months == 1 ? " month ago" : " months ago");
        } else {
            long years = days / 365;
            return years + (years == 1 ? " year ago" : " years ago");
        }
    }
}

