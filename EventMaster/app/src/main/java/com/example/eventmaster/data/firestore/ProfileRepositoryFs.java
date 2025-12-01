package com.example.eventmaster.data.firestore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.eventmaster.model.Profile;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Firestore repository for /profiles.
 *
 * Conventions:
 * - Collection: "profiles"
 * - Document ID = user id
 * - Uses unified Profile model (id/userId aliases, phone/phoneNumber aliases).
 * - Defaults on read:
 *      active: true if missing
 *      banned: false if missing
 *      role:   "entrant" if missing
 */
public class ProfileRepositoryFs {

    private static final String COLL = "profiles";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // ---------- Mapping helpers ----------

    private static Map<String, Object> toMap(@NonNull Profile p, boolean includeId) {
        Map<String, Object> m = new HashMap<>();

        if (includeId && p.getUserId() != null) m.put("userId", p.getUserId()); // not required, but harmless
        putIfNotNull(m, "deviceId", p.getDeviceId());
        putIfNotNull(m, "name", p.getName());
        putIfNotNull(m, "email", p.getEmail());
        putIfNotNull(m, "phoneNumber", p.getPhoneNumber());
        putIfNotNull(m, "profileImageUrl", p.getProfileImageUrl());
        putIfNotNull(m, "fcmToken", p.getFcmToken());

        m.put("notificationsEnabled", p.isNotificationsEnabled());
        putIfNotNull(m, "role", p.getRole());
        m.put("banned", p.getBanned());   // defaults applied by getters
        m.put("active", p.getActive());

        return m;
    }

    private static void putIfNotNull(Map<String, Object> m, String key, @Nullable Object val) {
        if (val == null) return;
        if (val instanceof String && ((String) val).isEmpty()) return;
        m.put(key, val);
    }

    private static Profile fromDoc(@NonNull DocumentSnapshot doc) {
        Profile p = doc.toObject(Profile.class);
        if (p == null) p = new Profile();

        // Ensure ID is set - check both userId and userID (case variations)
        if (p.getUserId() == null || p.getUserId().isEmpty()) {
            String userId = doc.getString("userId");
            if (userId == null || userId.isEmpty()) {
                userId = doc.getString("userID"); // Try uppercase ID variant
            }
            if (userId != null && !userId.isEmpty()) {
                p.setUserId(userId);
            } else {
                p.setUserId(doc.getId()); // Fallback to document ID
            }
        }

        // Apply null-safe defaults using raw fields
        Boolean active = doc.getBoolean("active");
        if (active == null) p.setActive(true);
        Boolean banned = doc.getBoolean("banned");
        if (banned == null) p.setBanned(false);

        String role = doc.getString("role");
        if (role == null || role.isEmpty()) p.setRole("entrant");

        // Legacy "phone" -> phoneNumber mapping
        if (p.getPhoneNumber() == null || p.getPhoneNumber().isEmpty()) {
            String legacyPhone = doc.getString("phone");
            if (legacyPhone != null && !legacyPhone.isEmpty()) {
                p.setPhoneNumber(legacyPhone);
            }
        }

        // Ensure profileImageUrl is loaded (Firestore toObject should handle it, but be explicit for safety)
        String imageUrl = doc.getString("profileImageUrl");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            p.setProfileImageUrl(imageUrl);
        }

        return p;
    }

    // ---------- Create / Update / Delete ----------

    /** Create or overwrite a profile with the model. */
    public Task<Void> upsert(@NonNull Profile p) {
        if (p.getUserId() == null || p.getUserId().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("userId is required"));
        }
        Map<String, Object> data = toMap(p, /*includeId*/ false);
        return db.collection(COLL).document(p.getUserId()).set(data);
    }

    /** Overload used by some screens: simple fields. */
    public Task<Void> upsert(@NonNull String userId,
                             @NonNull String name,
                             @NonNull String email,
                             @Nullable String phone) {
        Profile p = new Profile(userId, name, email, phone);
        return upsert(p);
    }

    /** Overload: upsert with arbitrary field map (doc created if absent). */
    public Task<Void> upsert(@NonNull String userId, @NonNull Map<String, Object> fields) {
        // ensure defaults for booleans if caller omitted them
        if (!fields.containsKey("active")) fields.put("active", true);
        if (!fields.containsKey("banned")) fields.put("banned", false);
        if (fields.containsKey("phone")) {
            // normalize legacy "phone" to "phoneNumber"
            Object v = fields.remove("phone");
            if (v != null) fields.put("phoneNumber", v);
        }
        return db.collection(COLL).document(userId).set(fields);
    }

    /** Partial update (merge). */
    public Task<Void> update(@NonNull String userId, @NonNull Map<String, Object> fields) {
        if (fields.containsKey("phone")) {
            Object v = fields.remove("phone");
            if (v != null) fields.put("phoneNumber", v);
        }
        return db.collection(COLL).document(userId).update(fields);
    }

    /** Set banned flag (Task-returning). */
    public Task<Void> setBanned(@NonNull String userId, boolean banned) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("banned", banned);
        return update(userId, fields);
    }

    /** Set banned flag (callback style to match callsites). */
    public void setBanned(@NonNull String userId,
                          boolean banned,
                          @NonNull OnSuccessListener<? super Void> ok,
                          @NonNull OnFailureListener err) {
        setBanned(userId, banned).addOnSuccessListener(ok).addOnFailureListener(err);
    }

    /** Hard delete (used by Admin remove). */
    public void delete(@NonNull String userId,
                       @NonNull OnSuccessListener<? super Void> ok,
                       @NonNull OnFailureListener err) {
        db.collection(COLL).document(userId)
                .delete()
                .addOnSuccessListener(ok)
                .addOnFailureListener(err);
    }

    // ---------- Reads (Task-based) ----------

    public Task<Profile> get(@NonNull String userId) {
        return db.collection(COLL).document(userId)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        Exception e = task.getException();
                        throw new IllegalStateException("Failed to get profile for userId: " + userId + ", Error: " + (e != null ? e.getMessage() : "unknown"));
                    }
                    DocumentSnapshot doc = task.getResult();
                    if (doc == null || !doc.exists()) {
                        throw new IllegalStateException("Profile not found for userId: " + userId + ". Document ID must exactly match the UID from Firebase Authentication.");
                    }
                    return fromDoc(doc);
                });
    }

    public Task<List<Profile>> getByRole(@NonNull String role) {
        return db.collection(COLL).whereEqualTo("role", role)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    List<Profile> out = new ArrayList<>();
                    QuerySnapshot snap = task.getResult();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Profile p = fromDoc(doc);
                        if (p.getActive() && !p.getBanned()) {
                            out.add(p);
                        }
                    }
                    return out;
                });
    }

    /** Get profile by device ID (for entrants to find existing profile on same device). 
     * Returns null if not found (use addOnCompleteListener and check result). */
    public Task<Profile> getByDeviceId(@NonNull String deviceId) {
        return db.collection(COLL)
                .whereEqualTo("deviceId", deviceId)
                .whereEqualTo("role", "entrant")
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    QuerySnapshot snap = task.getResult();
                    if (snap != null && !snap.isEmpty()) {
                        // Return the first matching profile
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            Profile p = fromDoc(doc);
                            if (p.getActive() && !p.getBanned()) {
                                return p;
                            }
                        }
                    }
                    // Return null if not found (this is normal for first-time users)
                    return null;
                });
    }

    public Task<List<Profile>> getEntrants() { return getByRole("entrant"); }
    public Task<List<Profile>> getOrganizers() { return getByRole("organizer"); }
    
    /** Get profile by email (for checking duplicates). 
     * Returns null if not found. */
    public Task<Profile> getByEmail(@NonNull String email) {
        return db.collection(COLL)
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    QuerySnapshot snap = task.getResult();
                    if (snap != null && !snap.isEmpty()) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            Profile p = fromDoc(doc);
                            if (p.getActive() && !p.getBanned()) {
                                return p;
                            }
                        }
                    }
                    return null;
                });
    }

    // ---------- Reads (callback overloads) ----------

    public void get(@NonNull String userId,
                    @NonNull OnSuccessListener<? super Profile> ok,
                    @NonNull OnFailureListener err) {
        get(userId).addOnSuccessListener(ok).addOnFailureListener(err);
    }

    // ---------- Live listeners ----------

    /** Live entrants list (active & not banned). */
    public ListenerRegistration listenEntrants(@NonNull Consumer<List<Profile>> onChange,
                                               @NonNull Consumer<Exception> onError) {
        return db.collection(COLL)
                .whereEqualTo("role", "entrant")
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        onError.accept(e);
                        return;
                    }
                    List<Profile> out = new ArrayList<>();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            Profile p = fromDoc(doc);
                            if (p.getActive() && !p.getBanned()) out.add(p);
                        }
                    }
                    onChange.accept(out);
                });
    }

    /** Live organizers list (active & not banned). */
    public ListenerRegistration listenOrganizers(@NonNull Consumer<List<Profile>> onChange,
                                                 @NonNull Consumer<Exception> onError) {
        return db.collection(COLL)
                .whereEqualTo("role", "organizer")
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        onError.accept(e);
                        return;
                    }
                    List<Profile> out = new ArrayList<>();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            Profile p = fromDoc(doc);
                            if (p.getActive() && !p.getBanned()) out.add(p);
                        }
                    }
                    onChange.accept(out);
                });
    }

    /** Optional: live single profile. */
    public ListenerRegistration listenProfile(@NonNull String userId,
                                              @NonNull Consumer<Profile> onChange,
                                              @NonNull Consumer<Exception> onError) {
        return db.collection(COLL).document(userId)
                .addSnapshotListener((doc, e) -> {
                    if (e != null) {
                        onError.accept(e);
                        return;
                    }
                    if (doc == null || !doc.exists()) {
                        onError.accept(new IllegalStateException("Profile not found: " + userId));
                        return;
                    }
                    onChange.accept(fromDoc(doc));
                });
    }
}
