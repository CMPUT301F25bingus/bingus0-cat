package com.example.eventmaster.utils;

import java.util.Date;

/**
 * Utility class for time and date formatting operations.
 * 
 * Purpose:
 * - Provides helper methods for formatting dates and times
 * - Generates relative time strings (e.g., "5 minutes ago", "2 days ago")
 * 
 * Usage:
 * String relativeTime = TimeUtils.getRelativeTimeString(date);
 */
public final class TimeUtils {
    
    // Private constructor to prevent instantiation
    private TimeUtils() {
        throw new AssertionError("Cannot instantiate TimeUtils class");
    }
    
    /**
     * Converts a Date to a human-readable relative time string.
     * 
     * Examples:
     * - "Just now" (< 1 minute)
     * - "5 minutes ago"
     * - "2 hours ago"
     * - "3 days ago"
     * - "2 weeks ago"
     * - "1 month ago"
     * 
     * @param date The date to convert
     * @return A relative time string
     */
    public static String getRelativeTimeString(Date date) {
        if (date == null) {
            return "Unknown time";
        }
        
        long now = System.currentTimeMillis();
        long timestamp = date.getTime();
        long diff = now - timestamp;
        
        // Convert to seconds
        long seconds = diff / 1000;
        
        if (seconds < 60) {
            return "Just now";
        }
        
        // Convert to minutes
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes == 1 ? "1 minute ago" : minutes + " minutes ago";
        }
        
        // Convert to hours
        long hours = minutes / 60;
        if (hours < 24) {
            return hours == 1 ? "1 hour ago" : hours + " hours ago";
        }
        
        // Convert to days
        long days = hours / 24;
        if (days < 7) {
            return days == 1 ? "1 day ago" : days + " days ago";
        }
        
        // Convert to weeks
        long weeks = days / 7;
        if (weeks < 4) {
            return weeks == 1 ? "1 week ago" : weeks + " weeks ago";
        }
        
        // Convert to months (approximate)
        long months = days / 30;
        if (months < 12) {
            return months == 1 ? "1 month ago" : months + " months ago";
        }
        
        // Convert to years (approximate)
        long years = days / 365;
        return years == 1 ? "1 year ago" : years + " years ago";
    }
    
    /**
     * Formats a Date as a simple date string.
     * 
     * @param date The date to format
     * @return A formatted date string (e.g., "Jan 15, 2024")
     */
    public static String formatDate(Date date) {
        if (date == null) {
            return "Unknown date";
        }
        
        java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("MMM dd, yyyy");
        return format.format(date);
    }
    
    /**
     * Formats a Date as a date and time string.
     * 
     * @param date The date to format
     * @return A formatted date-time string (e.g., "Jan 15, 2024 3:30 PM")
     */
    public static String formatDateTime(Date date) {
        if (date == null) {
            return "Unknown date";
        }
        
        java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("MMM dd, yyyy h:mm a");
        return format.format(date);
    }
}

