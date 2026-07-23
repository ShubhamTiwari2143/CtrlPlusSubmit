package in.ctrlplussubmit.model;


import java.time.LocalDateTime;

/**
 * User — POJO (Plain Old Java Object) / Model Bean
 *
 * Represents a row in the `users` table.
 * Used by:
 *   - UserDAO  (read from / write to DB)
 *   - SessionUtil (stored in HttpSession after login)
 *   - Servlets  (passed between controller and view)
 *
 * Roles: ADMIN | FACULTY | STUDENT
 *
 * IMPORTANT: passwordHash is NEVER sent to the frontend.
 *            Strip it out before serializing to JSON.
 */
public class User {

    private int           id;
    private String        fullName;
    private String        email;
    private String        passwordHash;   // BCrypt — never expose this in JSON responses
    private String        role;           // "ADMIN", "FACULTY", "STUDENT"
    private boolean       isActive;
    private String        profilePic;     // relative path in /uploads/profiles/
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // -------------------------------------------------------
    //  Constructors
    // -------------------------------------------------------

    public User() {}

    /** Constructor for creating a new user (before DB insert) */
    public User(String fullName, String email, String passwordHash, String role) {
        this.fullName     = fullName;
        this.email        = email;
        this.passwordHash = passwordHash;
        this.role         = role;
        this.isActive     = true;
    }

    /** Full constructor (used when reading from DB) */
    public User(int id, String fullName, String email, String passwordHash,
                String role, boolean isActive, String profilePic,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id           = id;
        this.fullName     = fullName;
        this.email        = email;
        this.passwordHash = passwordHash;
        this.role         = role;
        this.isActive     = isActive;
        this.profilePic   = profilePic;
        this.createdAt    = createdAt;
        this.updatedAt    = updatedAt;
    }

    // -------------------------------------------------------
    //  Role check helpers — used in SessionUtil and views
    // -------------------------------------------------------

    public boolean isAdmin()   { return "ADMIN".equals(this.role);   }
    public boolean isFaculty() { return "FACULTY".equals(this.role); }
    public boolean isStudent() { return "STUDENT".equals(this.role); }

    /**
     * Returns a safe version of this User without the password hash.
     * Use this when converting a User to JSON for frontend responses.
     */
    public User toSafeUser() {
        User safe = new User();
        safe.id         = this.id;
        safe.fullName   = this.fullName;
        safe.email      = this.email;
        safe.role       = this.role;
        safe.isActive   = this.isActive;
        safe.profilePic = this.profilePic;
        safe.createdAt  = this.createdAt;
        // passwordHash intentionally omitted
        return safe;
    }

    // -------------------------------------------------------
    //  Getters and Setters
    // -------------------------------------------------------

    public int getId()                      { return id;           }
    public void setId(int id)               { this.id = id;        }

    public String getFullName()                  { return fullName;          }
    public void setFullName(String fullName)     { this.fullName = fullName; }

    public String getEmail()                     { return email;             }
    public void setEmail(String email)           { this.email = email;       }

    public String getPasswordHash()                        { return passwordHash;              }
    public void setPasswordHash(String passwordHash)       { this.passwordHash = passwordHash; }

    public String getRole()                      { return role;              }
    public void setRole(String role)             { this.role = role;         }

    public boolean isActive()                    { return isActive;           }
    public void setActive(boolean active)        { this.isActive = active;    }

    public String getProfilePic()                      { return profilePic;               }
    public void setProfilePic(String profilePic)       { this.profilePic = profilePic;    }

    public LocalDateTime getCreatedAt()                    { return createdAt;              }
    public void setCreatedAt(LocalDateTime createdAt)      { this.createdAt = createdAt;    }

    public LocalDateTime getUpdatedAt()                    { return updatedAt;              }
    public void setUpdatedAt(LocalDateTime updatedAt)      { this.updatedAt = updatedAt;    }

    @Override
    public String toString() {
        return "User{id=" + id +
               ", fullName='" + fullName + '\'' +
               ", email='" + email + '\'' +
               ", role='" + role + '\'' +
               ", isActive=" + isActive + '}';
    }
}
