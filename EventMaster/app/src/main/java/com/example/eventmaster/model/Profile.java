package com.example.eventmaster.model;

public class Profile {
    private String id;          // Firestore document id == userId
    private String name;
    private String email;
    private String phone;
    private String role;        // "entrant" | "organizer" | "admin"
    private Boolean banned;     // null or false by default
    private Boolean active;     // optional, default true

    public Profile() {} // Firestore needs no-arg

    public Profile(String id, String name, String email, String phone) {
        this.id = id; this.name = name; this.email = email; this.phone = phone;
        this.role = "entrant"; this.banned = false; this.active = true;
    }

    // 5-arg constructor to support seeding with role
    public Profile(String id, String name, String email, String phone, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = (role == null ? "entrant" : role);
        this.banned = false;
        this.active = true;
    }

    // getters/setters
    public String getId(){ return id; }
    public void setId(String id){ this.id = id; }
    public String getName(){ return name; }
    public void setName(String name){ this.name = name; }
    public String getEmail(){ return email; }
    public void setEmail(String email){ this.email = email; }
    public String getPhone(){ return phone; }
    public void setPhone(String phone){ this.phone = phone; }
    public String getRole(){ return role; }
    public void setRole(String role){ this.role = role; }
    public Boolean getBanned(){ return banned == null ? false : banned; }
    public void setBanned(Boolean banned){ this.banned = banned; }
    public Boolean getActive(){ return active == null ? true : active; }
    public void setActive(Boolean active){ this.active = active; }
}
