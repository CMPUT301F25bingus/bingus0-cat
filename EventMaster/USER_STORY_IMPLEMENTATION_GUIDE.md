# User Story Implementation Guide

This guide provides step-by-step instructions to fix partially implemented and missing user stories.

---

## 📋 **Table of Contents**

1. [US 01.04.03 - Opt out of receiving notifications](#1-us-010403---opt-out-of-receiving-notifications)
2. [US 01.01.04 - Filter events by interest or availability](#2-us-010104---filter-events-by-interest-or-availability)
3. [US 01.05.05 - View criteria for lottery selection](#3-us-010505---view-criteria-for-lottery-selection)
4. [US 01.05.01 - Get another chance if selected user declines](#4-us-010501---get-another-chance-if-selected-user-declines)

---

## 1. **US 01.04.03 - Opt out of receiving notifications**

### **Status:** ⚠️ Backend works, UI missing

### **What to do:**
Add a toggle switch in `EditProfileActivity` to enable/disable notifications.

### **Step 1: Update `activity_edit_profile.xml`**

Add a Switch widget after the phone field:

```xml
<!-- Add this after the Phone TextInputLayout (around line 45) -->
<com.google.android.material.switchmaterial.SwitchMaterial
    android:id="@+id/switchNotifications"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="16dp"
    android:text="Enable notifications"
    android:textSize="16sp"
    android:padding="12dp" />
```

### **Step 2: Update `EditProfileActivity.java`**

```java
package com.example.eventmaster.ui.profile;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.R;
import com.example.eventmaster.data.firestore.ProfileRepositoryFs;
import com.example.eventmaster.model.Profile;
import com.google.android.material.switchmaterial.SwitchMaterial;

/**
 * Edit Profile screen: lets a user update name, email, phone, and notification preferences.
 * Uses ProfileRepositoryFs (Firestore) to load and upsert the profile.
 */
public class EditProfileActivity extends AppCompatActivity {
    private final ProfileRepositoryFs repo = new ProfileRepositoryFs();
    private String profileId;

    private EditText etName, etEmail, etPhone;
    private SwitchMaterial switchNotifications;
    private Button btnSave;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        profileId = getIntent().getStringExtra("profileId");
        etName  = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        switchNotifications = findViewById(R.id.switchNotifications);
        btnSave = findViewById(R.id.btnSave);

        // Load current profile data
        repo.get(profileId, p -> {
            etName.setText(p.getName());
            etEmail.setText(p.getEmail());
            etPhone.setText(p.getPhone());
            // Set notification toggle based on current preference
            switchNotifications.setChecked(p.isNotificationsEnabled());
        }, e -> {
            // Handle error - maybe show a toast
        });

        btnSave.setOnClickListener(v -> {
            Profile p = new Profile(
                    profileId,
                    etName.getText().toString().trim(),
                    etEmail.getText().toString().trim(),
                    etPhone.getText().toString().trim()
            );
            
            // Set notification preference
            p.setNotificationsEnabled(switchNotifications.isChecked());

            repo.upsert(p)
                    .addOnSuccessListener(x -> {
                        // Show success message
                        finish();
                    })
                    .addOnFailureListener(err -> {
                        // Show error message
                        android.widget.Toast.makeText(this, 
                                "Failed to save: " + err.getMessage(), 
                                android.widget.Toast.LENGTH_LONG).show();
                    });
        });
    }
}
```

### **Step 3: Test**
1. Open Edit Profile
2. Toggle notifications on/off
3. Save and verify the setting persists
4. Check that notifications are filtered correctly (backend already does this)

---

## 2. **US 01.01.04 - Filter events by interest or availability**

### **Status:** ⚠️ Search works, filter dialog missing

### **What to do:**
Implement a filter dialog with options for date, price, location, and availability.

### **Step 1: Create `dialog_filter_events.xml`**

Create new file: `app/src/main/res/layout/dialog_filter_events.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="24dp">

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Filter Events"
        android:textSize="20sp"
        android:textStyle="bold"
        android:layout_marginBottom="16dp" />

    <!-- Sort by Date -->
    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Sort by Date"
        android:textSize="14sp"
        android:layout_marginTop="8dp" />
    
    <RadioGroup
        android:id="@+id/radioGroupSort"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">
        <RadioButton
            android:id="@+id/radioSortNewest"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Newest First"
            android:checked="true" />
        <RadioButton
            android:id="@+id/radioSortOldest"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Oldest First" />
    </RadioGroup>

    <!-- Price Range -->
    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Price Range"
        android:textSize="14sp"
        android:layout_marginTop="16dp" />
    
    <com.google.android.material.textfield.TextInputLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Max Price ($)"
        android:layout_marginTop="8dp">
        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/editMaxPrice"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="numberDecimal" />
    </com.google.android.material.textfield.TextInputLayout>

    <!-- Location Filter -->
    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Location"
        android:textSize="14sp"
        android:layout_marginTop="16dp" />
    
    <com.google.android.material.textfield.TextInputLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Filter by location"
        android:layout_marginTop="8dp">
        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/editLocation"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="text" />
    </com.google.android.material.textfield.TextInputLayout>

    <!-- Availability Filter -->
    <com.google.android.material.checkbox.MaterialCheckBox
        android:id="@+id/checkAvailableSpots"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Show only events with available spots"
        android:layout_marginTop="16dp" />

    <!-- Action Buttons -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:layout_marginTop="24dp"
        android:gravity="end">
        
        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnClearFilters"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Clear"
            style="@style/Widget.Material3.Button.TextButton"
            android:layout_marginEnd="8dp" />
        
        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnApplyFilters"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Apply" />
    </LinearLayout>
</LinearLayout>
```

### **Step 2: Update `EventListFragment.java`**

Add filter state variables and implement the filter dialog:

```java
// Add these fields to EventListFragment class
private List<Event> filteredEvents = new ArrayList<>();
private String sortOrder = "newest"; // "newest" or "oldest"
private Double maxPrice = null;
private String locationFilter = null;
private boolean onlyAvailableSpots = false;

// Replace the showFilterDialog() method with this:
private void showFilterDialog() {
    View dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_filter_events, null);
    
    RadioGroup radioGroupSort = dialogView.findViewById(R.id.radioGroupSort);
    RadioButton radioSortNewest = dialogView.findViewById(R.id.radioSortNewest);
    RadioButton radioSortOldest = dialogView.findViewById(R.id.radioSortOldest);
    EditText editMaxPrice = dialogView.findViewById(R.id.editMaxPrice);
    EditText editLocation = dialogView.findViewById(R.id.editLocation);
    MaterialCheckBox checkAvailableSpots = dialogView.findViewById(R.id.checkAvailableSpots);
    MaterialButton btnClearFilters = dialogView.findViewById(R.id.btnClearFilters);
    MaterialButton btnApplyFilters = dialogView.findViewById(R.id.btnApplyFilters);
    
    // Set current filter values
    if (sortOrder.equals("newest")) {
        radioSortNewest.setChecked(true);
    } else {
        radioSortOldest.setChecked(true);
    }
    if (maxPrice != null) {
        editMaxPrice.setText(String.valueOf(maxPrice));
    }
    if (locationFilter != null) {
        editLocation.setText(locationFilter);
    }
    checkAvailableSpots.setChecked(onlyAvailableSpots);
    
    // Create dialog
    AlertDialog dialog = new AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create();
    
    // Apply filters button
    btnApplyFilters.setOnClickListener(v -> {
        // Get filter values
        int selectedRadioId = radioGroupSort.getCheckedRadioButtonId();
        if (selectedRadioId == R.id.radioSortNewest) {
            sortOrder = "newest";
        } else {
            sortOrder = "oldest";
        }
        
        String priceText = editMaxPrice.getText().toString().trim();
        maxPrice = priceText.isEmpty() ? null : Double.parseDouble(priceText);
        
        locationFilter = editLocation.getText().toString().trim();
        if (locationFilter.isEmpty()) locationFilter = null;
        
        onlyAvailableSpots = checkAvailableSpots.isChecked();
        
        // Apply filters
        applyFilters();
        dialog.dismiss();
    });
    
    // Clear filters button
    btnClearFilters.setOnClickListener(v -> {
        sortOrder = "newest";
        maxPrice = null;
        locationFilter = null;
        onlyAvailableSpots = false;
        applyFilters();
        dialog.dismiss();
    });
    
    dialog.show();
}

// Add this new method to apply filters
private void applyFilters() {
    filteredEvents = new ArrayList<>(allEvents);
    
    // Filter by price
    if (maxPrice != null) {
        filteredEvents.removeIf(event -> event.getPrice() > maxPrice);
    }
    
    // Filter by location
    if (locationFilter != null && !locationFilter.isEmpty()) {
        String locationLower = locationFilter.toLowerCase();
        filteredEvents.removeIf(event -> 
            event.getLocation() == null || 
            !event.getLocation().toLowerCase().contains(locationLower)
        );
    }
    
    // Filter by available spots (if capacity is set and waiting list is full)
    if (onlyAvailableSpots) {
        // This would require checking waiting list count vs capacity
        // For now, we'll just filter events that have capacity > 0
        filteredEvents.removeIf(event -> event.getCapacity() <= 0);
    }
    
    // Sort by date
    filteredEvents.sort((e1, e2) -> {
        Date date1 = e1.getEventDate();
        Date date2 = e2.getEventDate();
        
        if (date1 == null && date2 == null) return 0;
        if (date1 == null) return 1;
        if (date2 == null) return -1;
        
        int comparison = date1.compareTo(date2);
        return sortOrder.equals("newest") ? -comparison : comparison;
    });
    
    // Update adapter
    adapter.setEvents(filteredEvents);
    updateEmptyState();
}
```

### **Step 3: Update imports in `EventListFragment.java`**

Add these imports:
```java
import android.view.LayoutInflater;
import androidx.appcompat.app.AlertDialog;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import com.google.android.material.checkbox.MaterialCheckBox;
```

### **Step 4: Test**
1. Click Filter button
2. Set filters (price, location, availability)
3. Apply filters and verify events are filtered correctly
4. Clear filters and verify all events show again

---

## 3. **US 01.05.05 - View criteria for lottery selection**

### **Status:** ❌ Missing

### **What to do:**
Add a section in `EventDetailsFragment` that displays lottery selection criteria.

### **Step 1: Update `fragment_event_details.xml`**

Add this section after the waiting list count text (around line 217):

```xml
<!-- Lottery Criteria Section -->
<androidx.cardview.widget.CardView
    android:id="@+id/lottery_criteria_card"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="16dp"
    app:cardCornerRadius="8dp"
    app:cardElevation="4dp"
    app:cardBackgroundColor="#F5F5F5">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Lottery Selection Criteria"
            android:textSize="18sp"
            android:textStyle="bold"
            android:layout_marginBottom="8dp" />

        <TextView
            android:id="@+id/lottery_criteria_text"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="• Random selection from waiting list\n• All entrants have equal chance\n• Selected entrants receive invitations"
            android:textSize="14sp"
            android:lineSpacingExtra="4dp" />
    </LinearLayout>
</androidx.cardview.widget.CardView>
```

### **Step 2: Update `EventDetailsFragment.java`**

Add field and update display:

```java
// Add to UI Elements section
private View lotteryCriteriaCard;
private TextView lotteryCriteriaText;

// In onCreateView(), add:
lotteryCriteriaCard = view.findViewById(R.id.lottery_criteria_card);
lotteryCriteriaText = view.findViewById(R.id.lottery_criteria_text);

// Add this method to display lottery criteria
private void displayLotteryCriteria(Event event) {
    if (lotteryCriteriaCard == null || lotteryCriteriaText == null) return;
    
    // Build criteria text based on event properties
    StringBuilder criteria = new StringBuilder();
    criteria.append("• Selection Method: Random lottery\n");
    criteria.append("• All entrants on waiting list have equal chance\n");
    criteria.append("• Selected entrants will receive invitations\n");
    
    if (event.getCapacity() > 0) {
        criteria.append("• Event capacity: ").append(event.getCapacity()).append(" spots\n");
    }
    
    if (event.getWaitingListLimit() != null) {
        criteria.append("• Waiting list limit: ").append(event.getWaitingListLimit()).append(" entrants\n");
    }
    
    criteria.append("• Organizer runs lottery when registration opens");
    
    lotteryCriteriaText.setText(criteria.toString());
    
    // Show the card
    lotteryCriteriaCard.setVisibility(View.VISIBLE);
}

// Call this in displayEventDetails() method, after setting other event details:
displayLotteryCriteria(event);
```

### **Step 3: Test**
1. Open an event details page
2. Verify "Lottery Selection Criteria" card is visible
3. Check that criteria text is displayed correctly

---

## 4. **US 01.05.01 - Get another chance if selected user declines**

### **Status:** ❌ Missing (backend exists, UI missing)

### **What to do:**
Add UI for entrants to request another chance after declining an invitation, and show when replacement lottery is available.

### **Step 1: Update `fragment_event_details.xml`**

Add this section in the invitation include section (around line 189):

```xml
<!-- Replacement Lottery Section (shown after declining) -->
<LinearLayout
    android:id="@+id/replacement_lottery_section"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="16dp"
    android:background="#FFF3E0"
    android:layout_marginTop="8dp"
    android:visibility="gone">

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Another Chance Available"
        android:textSize="16sp"
        android:textStyle="bold"
        android:layout_marginBottom="8dp" />

    <TextView
        android:id="@+id/replacement_lottery_text"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="A replacement lottery is available. Would you like to join?"
        android:textSize="14sp"
        android:layout_marginBottom="12dp" />

    <com.google.android.material.button.MaterialButton
        android:id="@+id/btnJoinReplacementLottery"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Join Replacement Lottery"
        android:textAllCaps="false" />
</LinearLayout>
```

### **Step 2: Update `EventDetailsFragment.java`**

Add fields and logic:

```java
// Add to UI Elements section
private View replacementLotterySection;
private TextView replacementLotteryText;
private MaterialButton btnJoinReplacementLottery;

// In onCreateView(), add:
replacementLotterySection = view.findViewById(R.id.replacement_lottery_section);
replacementLotteryText = view.findViewById(R.id.replacement_lottery_text);
btnJoinReplacementLottery = view.findViewById(R.id.btnJoinReplacementLottery);

// Add this method to check for replacement lottery eligibility
private void checkReplacementLotteryEligibility() {
    // Check if user has declined an invitation for this event
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    db.collection("events")
            .document(eventId)
            .collection("invitations")
            .whereEqualTo("entrantId", userId)
            .whereEqualTo("status", "DECLINED")
            .limit(1)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (!querySnapshot.isEmpty()) {
                    // User has declined, check if replacement lottery is available
                    // (This would check if organizer has run a replacement lottery)
                    checkIfReplacementLotteryAvailable();
                } else {
                    replacementLotterySection.setVisibility(View.GONE);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking replacement lottery eligibility", e);
            });
}

private void checkIfReplacementLotteryAvailable() {
    // Check if there's an active replacement lottery or if user can rejoin waiting list
    // For now, we'll show the option if user declined and can rejoin waiting list
    replacementLotterySection.setVisibility(View.VISIBLE);
    replacementLotteryText.setText(
        "You declined your invitation. You can rejoin the waiting list for another chance!"
    );
    
    btnJoinReplacementLottery.setOnClickListener(v -> {
        // Rejoin waiting list
        handleJoinWaitingList();
        replacementLotterySection.setVisibility(View.GONE);
    });
}

// Update showInvitationInclude() method to check for replacement lottery when declined:
private void showInvitationInclude(@NonNull Invitation inv) {
    // ... existing code ...
    
    default: // DECLINED case
        inviteStatusText.setText("Invitation declined");
        inviteStatusText.setVisibility(View.VISIBLE);
        btnAccept.setEnabled(false);
        btnDecline.setEnabled(false);
        
        // Check for replacement lottery eligibility
        checkReplacementLotteryEligibility();
        break;
}

// Also call checkReplacementLotteryEligibility() in decideInviteOrJoin() 
// when no invitation is found but user might have declined before
```

### **Step 3: Add notification when replacement lottery runs**

Update `LotteryServiceFs.java` to notify declined users when replacement lottery runs:

```java
// In LotteryServiceFs.drawLottery(), after processing winners,
// add logic to notify users who declined invitations:

// Notify declined users about replacement lottery opportunity
db.collection("events")
        .document(eventId)
        .collection("invitations")
        .whereEqualTo("status", "DECLINED")
        .get()
        .addOnSuccessListener(declinedSnapshots -> {
            for (QueryDocumentSnapshot doc : declinedSnapshots) {
                String declinedUserId = doc.getString("entrantId");
                
                Map<String, Object> notification = new HashMap<>();
                notification.put("eventId", eventId);
                notification.put("recipientId", declinedUserId);
                notification.put("type", "REPLACEMENT_LOTTERY_AVAILABLE");
                notification.put("title", "Another Chance Available!");
                notification.put("message", "A replacement lottery is running. Rejoin the waiting list for another chance!");
                notification.put("isRead", false);
                notification.put("createdAt", Timestamp.now());
                
                db.collection("notifications").add(notification);
            }
        });
```

### **Step 4: Test**
1. Decline an invitation for an event
2. Verify "Another Chance Available" section appears
3. Click "Join Replacement Lottery" button
4. Verify user rejoins waiting list
5. Check that notification is sent when replacement lottery runs

---

## ✅ **Summary Checklist**

- [ ] **US 01.04.03:** Added notification toggle to `EditProfileActivity`
- [ ] **US 01.01.04:** Created filter dialog and implemented filtering logic
- [ ] **US 01.05.05:** Added lottery criteria card to event details
- [ ] **US 01.05.01:** Added replacement lottery UI and notification logic

---

## 🧪 **Testing Tips**

1. Test each feature independently
2. Test edge cases (null values, empty lists, etc.)
3. Verify Firebase data is updated correctly
4. Test UI responsiveness and error handling
5. Check that filters persist or reset appropriately

---

## 📝 **Notes**

- All backend services already exist, so you're mainly adding UI components
- Make sure to handle null checks and error cases
- Follow the existing code style and patterns
- Test on both emulator and physical device if possible

Good luck! 🚀




