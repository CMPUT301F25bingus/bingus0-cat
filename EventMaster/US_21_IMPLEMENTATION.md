# User Story #21 Implementation Documentation

## User Story: US 01.06.02 - Sign up from event details (3 SP)

**Description:** As an entrant, I want to be able to sign up for an event from the event details page by joining the waiting list.

**Status:** ✅ **COMPLETED**

---

## Implementation Overview

This user story has been fully implemented with:
- ✅ Event details display (US 01.06.01)
- ✅ Join waiting list functionality (US 01.06.02)
- ✅ Firebase Firestore integration
- ✅ Device-based user identification (US 01.07.01)
- ✅ Waiting list count display (US 01.05.04)
- ✅ Registration period validation
- ✅ Duplicate entry prevention

---

## Files Created/Modified

### Model Classes
- `app/src/main/java/com/example/eventmaster/model/Event.java` - Event data model with Javadoc
- `app/src/main/java/com/example/eventmaster/model/WaitingListEntry.java` - Waiting list entry model
- `app/src/main/java/com/example/eventmaster/model/Profile.java` - User profile model

### Data Layer (Repository Pattern)
- `app/src/main/java/com/example/eventmaster/data/api/EventRepository.java` - Event repository interface
- `app/src/main/java/com/example/eventmaster/data/api/WaitingListRepository.java` - Waiting list repository interface
- `app/src/main/java/com/example/eventmaster/data/firestore/EventRepositoryFs.java` - Firestore implementation
- `app/src/main/java/com/example/eventmaster/data/firestore/WaitingListRepositoryFs.java` - Firestore implementation

### UI Layer
- `app/src/main/java/com/example/eventmaster/ui/entrant/EventDetailsFragment.java` - Main fragment for event details
- `app/src/main/java/com/example/eventmaster/ui/entrant/EventDetailsActivity.java` - Host activity
- `app/src/main/res/layout/fragment_event_details.xml` - UI layout

### Utilities
- `app/src/main/java/com/example/eventmaster/utils/DeviceUtils.java` - Device ID utility
- `app/src/main/java/com/example/eventmaster/utils/TestDataHelper.java` - Test data seeding

### Testing
- `app/src/main/java/com/example/eventmaster/MainActivity.java` - Updated for testing
- `app/src/test/java/com/example/eventmaster/model/EventTest.java` - Unit tests
- `app/src/test/java/com/example/eventmaster/model/WaitingListEntryTest.java` - Unit tests
- `app/src/test/java/com/example/eventmaster/model/ProfileTest.java` - Unit tests
- `app/src/androidTest/java/com/example/eventmaster/EventDetailsActivityTest.java` - Intent tests

### Configuration
- `app/build.gradle.kts` - Added Firebase Firestore and Espresso Intents dependencies
- `app/src/main/AndroidManifest.xml` - Registered EventDetailsActivity

---

## Features Implemented

### 1. Event Details Display (US 01.06.01)
- Event name, description, location
- Event date and time
- Organizer information
- Price
- Registration period
- Event poster (placeholder for image loading)
- Waiting list count

### 2. Join Waiting List (US 01.06.02)
- "Join Waiting List" button
- Registration period validation (won't allow joining if registration is closed or not yet open)
- Duplicate entry prevention (checks if user is already in waiting list)
- Real-time feedback with Toast messages
- Button state management (disabled after joining)
- Firestore integration to persist waiting list entries

### 3. Additional Features
- Device-based identification (no username/password required)
- Waiting list count display
- Date formatting for user-friendly display
- Error handling with user feedback
- Support for optional geolocation (prepared but not yet implemented)

---

## How to Test

### Step 1: Setup Firebase
Ensure your Firebase project is properly configured with the `google-services.json` file in the `app/` directory.

### Step 2: Seed Test Data
1. Run the app on an emulator or device
2. On the main screen, tap **"Seed Test Data"**
3. This creates 3 sample events in Firestore:
   - **Swimming Lessons** (registration open)
   - **Piano Lessons** (registration not yet open)
   - **Dance Class** (registration open)

### Step 3: Test Event Details & Join Waiting List
1. Tap any of the test event buttons (e.g., "Test: Swimming Lessons")
2. Verify event details are displayed correctly:
   - Event name, description, location
   - Dates and price
   - Organizer name
   - Waiting list count
3. Tap **"Join Waiting List"** button
4. Verify success message appears
5. Verify button changes to "Already in Waiting List" and is disabled
6. Verify waiting list count increases

### Step 4: Test Registration Period Validation
1. Try joining "Piano Lessons" (registration not yet open)
2. Verify you get a message that registration hasn't opened yet

### Step 5: Test Duplicate Prevention
1. After joining a waiting list, close and reopen the event details
2. Verify the button is disabled and shows "Already in Waiting List"

### Step 6: Run Unit Tests
```bash
./gradlew test
```

This will run:
- `EventTest` - Tests Event model class
- `WaitingListEntryTest` - Tests WaitingListEntry model
- `ProfileTest` - Tests Profile model

### Step 7: Run Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

This will run:
- `EventDetailsActivityTest` - Tests UI and intent handling

---

## Firebase Firestore Structure

### Collections

#### `events`
```json
{
  "test_event_1": {
    "eventId": "test_event_1",
    "name": "Swimming Lessons for Beginners",
    "description": "Learn the basics of swimming...",
    "location": "Local Recreation Centre Pool",
    "eventDate": "2025-11-18T...",
    "registrationStartDate": "2025-11-03T...",
    "registrationEndDate": "2025-11-06T...",
    "organizerId": "org_001",
    "organizerName": "City Recreation Centre",
    "capacity": 20,
    "price": 60.00,
    "geolocationRequired": false
  }
}
```

#### `waitingList`
```json
{
  "entry_uuid": {
    "entryId": "uuid",
    "eventId": "test_event_1",
    "userId": "device_id",
    "joinedDate": "2025-11-04T...",
    "status": "waiting",
    "latitude": null,
    "longitude": null
  }
}
```

---

## Architecture & Design Patterns

### Repository Pattern
- Separates data access logic from business logic
- `EventRepository` and `WaitingListRepository` interfaces define contracts
- `EventRepositoryFs` and `WaitingListRepositoryFs` implement Firestore-specific logic
- Easy to swap implementations (e.g., for testing with mock data)

### Fragment-based UI
- `EventDetailsFragment` is reusable and can be embedded in different activities
- Follows Android best practices for modern app architecture

### Callback Pattern
- Asynchronous Firestore operations use callback interfaces
- `OnEventListener`, `OnWaitingListOperationListener`, etc.
- Clear success/failure handling

---

## Code Documentation

All model classes include:
- ✅ Javadoc class-level documentation
- ✅ Javadoc method documentation for public methods
- ✅ Parameter descriptions
- ✅ File purpose comments

Example:
```java
/**
 * Represents an event in the Event Lottery System.
 * Contains event details like name, description, dates, organizer info, and capacity limits.
 */
public class Event {
    /**
     * Creates a new Event with the specified details.
     *
     * @param eventId Unique identifier for the event
     * @param name Name of the event
     * ...
     */
    public Event(String eventId, String name, ...) {
        // implementation
    }
}
```

---

## Known Issues / TODO

1. **Image Loading**: Event poster images are not yet loaded. Need to integrate an image loading library like Glide or Picasso.
2. **Geolocation**: Optional geolocation tracking is prepared but not yet implemented (requires location permissions and GPS access).
3. **Offline Support**: App requires network connection to access Firestore. Could add offline caching in future.

---

## Related User Stories

This implementation also partially supports:
- ✅ **US 01.06.01** - View event details via QR code (display implemented, QR scanning pending)
- ✅ **US 01.07.01** - Device-based identification (implemented)
- ✅ **US 01.05.04** - View waiting list count (implemented)
- ✅ **US 01.01.01** - Join waiting list (implemented)

---

## Next Steps

For complete functionality, the following should be implemented:
1. **QR Code Scanning** (US 01.06.01) - Integrate ZXing or Google ML Kit for QR scanning
2. **Event List View** (US 01.01.03) - Display list of all available events
3. **Leave Waiting List** (US 01.01.02) - Allow users to remove themselves
4. **Geolocation** (US 02.02.02, US 02.02.03) - Optional location tracking

---

## Testing Summary

✅ **Unit Tests**: 3 test classes, covering all model classes
✅ **Instrumented Tests**: 1 test class with 3 test cases
✅ **Manual Testing**: All functionality verified manually
✅ **Firebase Integration**: Successfully reads and writes to Firestore
✅ **Edge Cases Handled**: 
  - Registration period validation
  - Duplicate entry prevention
  - Network error handling
  - Loading states

---

## Contributors
- Owner B (Ipsa) - Implementation of US 01.06.01, US 01.06.02

---

**Date Completed:** November 4, 2025
**Sprint:** Part 3 - Half-Way Checkpoint



