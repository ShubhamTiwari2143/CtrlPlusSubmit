package in.ctrlplussubmit.model;


import java.time.LocalDateTime;

/**
 * Notification — POJO for the `notifications` table
 *
 * Notifications are created server-side when:
 *   - A task is assigned to the student (TASK_ASSIGNED)
 *   - A submission is reviewed by faculty (SUBMISSION_REVIEWED)
 *   - Admin sends a system message (SYSTEM)
 *
 * type: TASK_ASSIGNED | DEADLINE_REMINDER | SUBMISSION_REVIEWED | SYSTEM
 */
public class Notification {

    private int           id;
    private int           userId;
    private String        title;
    private String        message;
    private String        type;            // TASK_ASSIGNED | SUBMISSION_REVIEWED | SYSTEM
    private boolean       isRead;
    private Integer       relatedTaskId;   // Optional link back to a task
    private LocalDateTime createdAt;

    // -------------------------------------------------------
    //  Constructors
    // -------------------------------------------------------

    public Notification() {}

    /** Constructor for creating a new notification (before DB insert) */
    public Notification(int userId, String title, String message,
                        String type, Integer relatedTaskId) {
        this.userId        = userId;
        this.title         = title;
        this.message       = message;
        this.type          = type;
        this.relatedTaskId = relatedTaskId;
        this.isRead        = false;
    }

    // -------------------------------------------------------
    //  Getters & Setters
    // -------------------------------------------------------

    public int getId()                        { return id;           }
    public void setId(int id)                 { this.id = id;        }

    public int getUserId()                    { return userId;             }
    public void setUserId(int userId)         { this.userId = userId;      }

    public String getTitle()                  { return title;        }
    public void setTitle(String title)        { this.title = title;  }

    public String getMessage()                { return message;            }
    public void setMessage(String message)    { this.message = message;    }

    public String getType()                   { return type;         }
    public void setType(String type)          { this.type = type;    }

    public boolean isRead()                   { return isRead;             }
    public void setRead(boolean read)         { this.isRead = read;        }

    public Integer getRelatedTaskId()                      { return relatedTaskId;                }
    public void setRelatedTaskId(Integer relatedTaskId)    { this.relatedTaskId = relatedTaskId;  }

    public LocalDateTime getCreatedAt()                    { return createdAt;              }
    public void setCreatedAt(LocalDateTime createdAt)      { this.createdAt = createdAt;    }

    @Override
    public String toString() {
        return "Notification{id=" + id + ", userId=" + userId +
               ", type='" + type + "', isRead=" + isRead + '}';
    }
}
