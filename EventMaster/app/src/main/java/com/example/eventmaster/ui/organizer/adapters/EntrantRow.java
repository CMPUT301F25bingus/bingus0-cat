package com.example.eventmaster.ui.organizer.adapters;



/**
 * Represents a single entrant displayed in the organizer's entrant list UI.
 * Each {@code EntrantRow} corresponds to one participant in the event,
 * showing basic identifying information such as name, email, and phone number.
 * This model is used to populate each row of the RecyclerView in
 * {@link EntrantRowAdapter}.
 * This class supports user stories:
 *      #35 — US 02.06.02: View cancelled entrants
 *      #36 — US 02.06.03: View final enrolled list
 */
public class EntrantRow {
    public final String id;
    public final String name;
    public final String email;
    public final String phone;


    /**
     * Creates a new {@code EntrantRow} object containing basic entrant information.
     *
     * @param id    unique identifier for the entrant
     * @param name  entrant’s display name
     * @param email entrant’s email address
     * @param phone entrant’s phone number
     */
    public EntrantRow(String id, String name, String email, String phone) {
        this.id = id; this.name = name; this.email = email; this.phone = phone;
    }
}
