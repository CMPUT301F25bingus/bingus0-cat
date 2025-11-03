package com.example.eventmaster.model;

import androidx.annotation.Nullable;

public class Profile {
    @Nullable private String id;
    private String name;
    private String email;
    @Nullable private String phone;
    @Nullable private String role;

    public Profile(@Nullable String id, String name, String email,
                   @Nullable String phone, @Nullable String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
    }


    public Profile(String name, String email, @Nullable String phone) {
        this(null, name, email, phone, null);
    }

    @Nullable public String getId()   { return id; }
    public String getName()           { return name; }
    public String getEmail()          { return email; }
    @Nullable public String getPhone(){ return phone; }
    @Nullable public String getRole() { return role; }

    public void setId(@Nullable String id)     { this.id = id; }
    public void setName(String name)           { this.name = name; }
    public void setEmail(String email)         { this.email = email; }
    public void setPhone(@Nullable String p)   { this.phone = p; }
    public void setRole(@Nullable String role) { this.role = role; }
}
