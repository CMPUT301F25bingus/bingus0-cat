//package com.example.eventmaster.ui.entrant;
//
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//
//import com.example.eventmaster.R;
//import com.example.eventmaster.data.firestore.InvitationServiceFs;
//import com.example.eventmaster.data.firestore.RegistrationServiceFs;
//import com.example.eventmaster.model.Invitation;
//import com.google.android.material.button.MaterialButton;
//
//public class EventDetailsFragment extends Fragment {
//
//    public static final String ARG_EVENT_ID = "eventId";
//    public static final String ARG_USER_ID  = "userId";
//
//    private String eventId;
//    private String userId;
//
//    private InvitationServiceFs invitationService;
//    private RegistrationServiceFs registrationService;
//
//    // invitation include
//    private View inviteRoot;
//    private TextView inviteStatusText;
//    private MaterialButton btnAccept, btnDecline;
//
//    public static EventDetailsFragment newInstance(String eventId, String userId) {
//        EventDetailsFragment f = new EventDetailsFragment();
//        Bundle b = new Bundle();
//        b.putString(ARG_EVENT_ID, eventId);
//        b.putString(ARG_USER_ID, userId);
//        f.setArguments(b);
//        return f;
//    }
//
//    @Override
//    public @Nullable View onCreateView(@NonNull LayoutInflater inflater,
//                                       @Nullable ViewGroup container,
//                                       @Nullable Bundle savedInstanceState) {
//        View v = inflater.inflate(R.layout.fragment_event_details, container, false);
//
//        eventId = requireArguments().getString(ARG_EVENT_ID);
//        userId  = requireArguments().getString(ARG_USER_ID);
//
//        invitationService   = new InvitationServiceFs();
//        registrationService = new RegistrationServiceFs();
//
//        // find include views
//        inviteRoot      = v.findViewById(R.id.invitation_actions_root);
//        inviteStatusText= v.findViewById(R.id.invite_status_text);
//        btnAccept       = v.findViewById(R.id.btnAccept);
//        btnDecline      = v.findViewById(R.id.btnDecline);
//
//        bindInvitationUi();
//
//        // ... bind the rest of your event UI (title, poster, etc.) here ...
//
//        return v;
//    }
//
//    private void bindInvitationUi() {
//        // default hidden
//        inviteRoot.setVisibility(View.GONE);
//        invitationService.getMyInvitation(eventId, userId, inv -> {
//            if (inv == null) {
//                inviteRoot.setVisibility(View.GONE);
//                return;
//            }
//            switch (String.valueOf(inv.getStatus())) {
//                case "PENDING":
//                    inviteRoot.setVisibility(View.VISIBLE);
//                    wireButtons(inv);
//                    break;
//                case "ACCEPTED":
//                    inviteRoot.setVisibility(View.VISIBLE);
//                    inviteStatusText.setText("You're enrolled 🎉");
//                    inviteStatusText.setVisibility(View.VISIBLE);
//                    btnAccept.setEnabled(false);
//                    btnDecline.setEnabled(false);
//                    break;
//                case "DECLINED":
//                default:
//                    inviteRoot.setVisibility(View.VISIBLE);
//                    inviteStatusText.setText("Invitation declined");
//                    inviteStatusText.setVisibility(View.VISIBLE);
//                    btnAccept.setEnabled(false);
//                    btnDecline.setEnabled(false);
//                    break;
//            }
//        }, this::toast);
//    }
//
//    private void wireButtons(Invitation inv) {
//        btnAccept.setEnabled(true);
//        btnDecline.setEnabled(true);
//
//        btnAccept.setOnClickListener(v -> {
//            setButtonsEnabled(false);
//            invitationService.accept(inv.getId(), eventId, userId, ok -> {
//                // ensure Registration exists/enrolled
//                registrationService.enroll(eventId, userId, r -> {
//                    inviteStatusText.setText("You're enrolled 🎉");
//                    inviteStatusText.setVisibility(View.VISIBLE);
//                }, err -> {
//                    toast(err);
//                    setButtonsEnabled(true);
//                });
//            }, err -> {
//                toast(err);
//                setButtonsEnabled(true);
//            });
//        });
//
//        btnDecline.setOnClickListener(v -> {
//            setButtonsEnabled(false);
//            invitationService.decline(inv.getId(), eventId, userId, ok -> {
//                inviteStatusText.setText("Invitation declined");
//                inviteStatusText.setVisibility(View.VISIBLE);
//            }, err -> {
//                toast(err);
//                setButtonsEnabled(true);
//            });
//        });
//    }
//
//    private void setButtonsEnabled(boolean enabled) {
//        btnAccept.setEnabled(enabled);
//        btnDecline.setEnabled(enabled);
//    }
//
//    private void toast(Throwable t) { toast(t.getMessage()); }
//    private void toast(String msg) {
//        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
//    }
//}
