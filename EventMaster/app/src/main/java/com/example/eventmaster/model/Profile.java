package com.example.eventmaster.model;

/**
 * Domain model for a user profile stored under /profiles/{id}.
 *
 * Fields:
 * - id: Firestore doc id (also acts as user id)
 * - name/email/phone: contact information
 * - role: "entrant" | "organizer" | "admin"
 * - banned: whether the account is banned by an admin
 * - active: soft-delete flag
 */
public class Profile {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String role;
    private Boolean banned;
    private Boolean active;

    /** No-arg constructor required by Firestore. */
    public Profile() {}
    /**
     * Convenience constructor for quickly creating a profile shell.
     * @param id    profile/user id (used as Firestore doc id)
     * @param name  display name
     * @param email contact email
     * @param phone contact phone (optional; may be empty)
     */
    public Profile(String id, String name, String email, String phone) {
        this.id = id; this.name = name; this.email = email; this.phone = phone;
        this.role = "entrant"; this.banned = false; this.active = true;
    }


    public Profile(String id, String name, String email, String phone, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = (role == null ? "entrant" : role);
        this.banned = false;
        this.active = true;
    }

    /** @return Firestore document id (user id). */
    public String getId(){ return id; }

    /** Sets the Firestore document id. */
    public void setId(String id){ this.id = id; }

    /** @return display name (may be empty, never null). */
    public String getName(){ return name; }

    /** Sets display name. */
    public void setName(String name){ this.name = name; }

    /** @return contact email (may be empty). */
    public String getEmail(){ return email; }

    /** Sets contact email. */
    public void setEmail(String email){ this.email = email; }

    /** @return contact phone (may be empty). */
    public String getPhone(){ return phone; }

    /** Sets contact phone. */
    public void setPhone(String phone){ this.phone = phone; }

    /** @return role string; default "entrant" if unset. */
    public String getRole(){ return role; }

    /** Sets role ("entrant" | "organizer" | "admin"). */
    public void setRole(String role){ this.role = role; }

    /** @return ban status; defaults to false if null. */
    public Boolean getBanned(){ return banned == null ? false : banned; }

    /** Sets ban status. */
    public void setBanned(Boolean banned){ this.banned = banned; }

    /** @return active flag; defaults to true if null. */
    public Boolean getActive(){ return active == null ? true : active; }

    /** Sets active flag. */
    public void setActive(Boolean active){ this.active = active; }
}
