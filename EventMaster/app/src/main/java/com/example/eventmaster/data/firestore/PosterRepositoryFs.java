/**
 * PosterRepositoryFs
 *
 * Role:
 *  - Firebase Storage–backed repository for uploading event poster images.
 *
 * Design Pattern:
 *  - Repository pattern; provides an abstraction layer for cloud storage access.
 *
 * Storage Path:
 *  - events/{eventId}/poster.jpg (JPEG; public read, write requires Firebase Auth)
 *
 * Contract:
 *  - upload(eventId, localPoster) → Task<String> returning the poster’s public URL.
 *
 * Notes:
 *  - Caller must ensure FirebaseAuth is initialized (anonymous is fine).
 *  - Consider resizing/compressing on-device if the image is very large.
 *
 * Outstanding Issues:
 *  - None known.
 */

package com.example.eventmaster.data.firestore;

import android.net.Uri;
import com.example.eventmaster.data.api.PosterRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

/**
 * Firebase Storage implementation of {@link PosterRepository}.
 * Handles poster uploads for events and resolves to a public URL.
 */
public class PosterRepositoryFs implements PosterRepository {

    private final StorageReference root;

    /**
     * Default constructor for production use.
     * Initializes {@link FirebaseStorage#getReference()} as root.
     */
    public PosterRepositoryFs() {
        this.root = FirebaseStorage.getInstance().getReference();
    }

    /**
     * Alternate constructor for dependency injection in tests.
     *
     * @param root mock or custom {@link StorageReference} root
     */
    public PosterRepositoryFs(StorageReference root) {
        this.root = root;
    }

    /**
     * Uploads a local poster image to Firebase Storage and returns a task
     * that resolves to its public download URL upon success.
     *
     * @param eventId     Firestore event identifier used for the storage path
     * @param localPoster content {@link Uri} of the selected poster image
     * @return {@link Task} resolving to the poster’s public download URL
     */
    @Override
    public Task<String> upload(String eventId, Uri localPoster) {
        try {
            StorageReference ref = root.child("events/" + eventId + "/poster.jpg");
            UploadTask uploadTask = ref.putFile(localPoster);

            return uploadTask
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) throw task.getException();
                        return ref.getDownloadUrl();
                    })
                    .continueWith(urlTask -> {
                        if (!urlTask.isSuccessful()) throw urlTask.getException();
                        return urlTask.getResult().toString();
                    });

        } catch (Exception e) {
            return Tasks.forException(e);
        }
    }
}
