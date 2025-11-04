/**
 * PosterRepositoryFs
 * Role:
 *  - Firebase Storage–backed repository for uploading event poster images.
 * Storage Path:
 *  - events/{eventId}/poster.jpg  (JPEG, public read; write requires Firebase Auth)
 * Contract:
 *  - upload(eventId, localPoster) -> Task<String> returns the public download URL.
 * Notes:
 *  - Caller is responsible for ensuring the user is authenticated (anonymous is fine).
 *  - Consider resizing/compressing on-device if very large images are selected.
 */

package com.example.eventmaster.data.firestore;

import android.net.Uri;
import com.example.eventmaster.data.api.PosterRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class PosterRepositoryFs implements PosterRepository {

    private final StorageReference root;

    /**
     * Default constructor (used in production).
     * Uses FirebaseStorage.getInstance().getReference() as root.
     */
    public PosterRepositoryFs() {
        this.root = FirebaseStorage.getInstance().getReference();
    }

    /**
     * Test constructor (used in unit tests).
     * Allows injection of a mock StorageReference.
     */
    public PosterRepositoryFs(StorageReference root) {
        this.root = root;
    }

    /**
     * Uploads a local poster image to Firebase Storage and returns a Task that resolves
     * to its public download URL upon success.
     *
     * @param eventId      Firestore event identifier used to build the storage path.
     * @param localPoster  Content Uri of the selected poster image.
     * @return Task<String> resolving to the poster's download URL.
     */
    @Override
    public Task<String> upload(String eventId, Uri localPoster) {
        try {
            StorageReference ref = root.child("events/" + eventId + "/poster.jpg");
            UploadTask uploadTask = ref.putFile(localPoster);

            return uploadTask
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        return ref.getDownloadUrl();
                    })
                    .continueWith(urlTask -> {
                        if (!urlTask.isSuccessful()) {
                            throw urlTask.getException();
                        }
                        return urlTask.getResult().toString();
                    });

        } catch (Exception e) {
            return Tasks.forException(e);
        }
    }
}
