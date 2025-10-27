package com.example.eventmaster.data.firestore;

import android.net.Uri;
import com.example.eventmaster.data.api.PosterRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class PosterRepositoryFs implements PosterRepository {
    private final StorageReference root = FirebaseStorage.getInstance().getReference();

    @Override
    public Task<String> upload(String eventId, Uri localPoster) {
        try {
            StorageReference ref = root.child("posters/" + eventId + ".jpg");
            return ref.putFile(localPoster)
                    .continueWithTask(t -> {
                        if (!t.isSuccessful()) return Tasks.forException(t.getException());
                        return ref.getDownloadUrl();
                    })
                    .continueWith(t -> {
                        if (!t.isSuccessful()) throw t.getException();
                        return t.getResult().toString();
                    });
        } catch (Exception e) {
            return Tasks.forException(e);
        }
    }
}
