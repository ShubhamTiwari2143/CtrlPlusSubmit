package in.ctrlplussubmit.model;


import java.time.LocalDateTime;

/**
 * AccountRequest — POJO for the `account_requests` table
 *
 * When a new user wants access, they submit a request.
 * Admin approves or rejects it from the Admin Panel.
 *
 * Status flow:  PENDING → APPROVED (user created) | REJECTED
 */
public class AccountRequest {

    private int           id;
    private String        fullName;
    private String        email;
    private String        requestedRole;   // "FACULTY" or "STUDENT"
    private String        message;         // Optional message from requester
    private String        status;          // "PENDING", "APPROVED", "REJECTED"
    private LocalDateTime reviewedAt;
    private LocalDateTime requestedAt;

    // -------------------------------------------------------
    //  Constructors
    // -------------------------------------------------------

    public AccountRequest() {}

    /** Constructor for creating a new request (before DB insert) */
    public AccountRequest(String fullName, String email,
                          String requestedRole, String message) {
        this.fullName      = fullName;
        this.email         = email;
        this.requestedRole = requestedRole;
        this.message       = message;
        this.status        = "PENDING";
    }

    // -------------------------------------------------------
    //  Getters & Setters
    // -------------------------------------------------------

    public int getId()                          { return id;            }
    public void setId(int id)                   { this.id = id;         }

    public String getFullName()                 { return fullName;      }
    public void setFullName(String fullName)    { this.fullName = fullName; }

    public String getEmail()                    { return email;         }
    public void setEmail(String email)          { this.email = email;   }

    public String getRequestedRole()                        { return requestedRole;              }
    public void setRequestedRole(String requestedRole)      { this.requestedRole = requestedRole; }

    public String getMessage()                  { return message;       }
    public void setMessage(String message)      { this.message = message; }

    public String getStatus()                   { return status;        }
    public void setStatus(String status)        { this.status = status; }

    public LocalDateTime getReviewedAt()                    { return reviewedAt;             }
    public void setReviewedAt(LocalDateTime reviewedAt)     { this.reviewedAt = reviewedAt;  }

    public LocalDateTime getRequestedAt()                   { return requestedAt;            }
    public void setRequestedAt(LocalDateTime requestedAt)   { this.requestedAt = requestedAt; }

    @Override
    public String toString() {
        return "AccountRequest{id=" + id +
               ", email='" + email + '\'' +
               ", role='" + requestedRole + '\'' +
               ", status='" + status + '\'' + '}';
    }
}

