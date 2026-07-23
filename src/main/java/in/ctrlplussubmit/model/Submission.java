package in.ctrlplussubmit.model;


import java.time.LocalDateTime;

/**
 * Submission — POJO for the `submissions` table
 *
 * One row per student per task.
 * Created automatically when a task is assigned;
 * starts with status=PENDING until the student submits.
 *
 * Status flow:
 *   PENDING → SUBMITTED / LATE → REVIEWED → APPROVED / NEEDS_IMPROVEMENT / REJECTED
 */
public class Submission {

    private int           id;
    private int           taskId;
    private String        taskTitle;       // Joined field
    private int           studentId;
    private String        studentName;     // Joined field
    private String        studentEmail;    // Joined field
    private String        filePath;        // Relative path stored in DB
    private String        fileName;        // Original file name for display
    private String        comments;        // Student note on submission
    private String        status;          // PENDING | SUBMITTED | LATE | REVIEWED | APPROVED | NEEDS_IMPROVEMENT | REJECTED
    private Integer       marks;           // Nullable — set after review
    private String        facultyRemarks;  // Reviewer note
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private int           resubmitCount;
    private LocalDateTime updatedAt;
    private int fileSizeKB;

    // Extra joined fields for faculty review view
    private LocalDateTime taskDeadline;
    private int           maxMarks;

    // -------------------------------------------------------
    //  Constructors
    // -------------------------------------------------------

    public Submission() {}

    /** Constructor used when creating a new pending submission row */
    public Submission(int taskId, int studentId) {
        this.taskId    = taskId;
        this.studentId = studentId;
        this.status    = "PENDING";
        this.resubmitCount = 0;
    }

    // -------------------------------------------------------
    //  Helpers
    // -------------------------------------------------------

    public boolean isSubmitted() {
        return "SUBMITTED".equals(status) || "LATE".equals(status)
               || "REVIEWED".equals(status) || "APPROVED".equals(status)
               || "NEEDS_IMPROVEMENT".equals(status) || "REJECTED".equals(status);
    }

    public boolean isPending() { return "PENDING".equals(status); }
    public boolean isLate()    { return "LATE".equals(status);    }
    public boolean isReviewed(){ return "REVIEWED".equals(status) || "APPROVED".equals(status)
                                        || "NEEDS_IMPROVEMENT".equals(status) || "REJECTED".equals(status); }
    public boolean hasFile()   { return filePath != null && !filePath.isBlank(); }

    // -------------------------------------------------------
    //  Getters & Setters
    // -------------------------------------------------------

    public int getId()                        { return id;            }
    public void setId(int id)                 { this.id = id;         }

    public int getTaskId()                    { return taskId;         }
    public void setTaskId(int taskId)         { this.taskId = taskId; }

    public String getTaskTitle()                   { return taskTitle;            }
    public void setTaskTitle(String taskTitle)     { this.taskTitle = taskTitle;  }

    public int getStudentId()                 { return studentId;           }
    public void setStudentId(int studentId)   { this.studentId = studentId; }

    public String getStudentName()                   { return studentName;            }
    public void setStudentName(String studentName)   { this.studentName = studentName; }

    public String getStudentEmail()                    { return studentEmail;             }
    public void setStudentEmail(String studentEmail)   { this.studentEmail = studentEmail; }

    public String getFilePath()                  { return filePath;           }
    public void setFilePath(String filePath)     { this.filePath = filePath;  }

    public String getFileName()                  { return fileName;           }
    public void setFileName(String fileName)     { this.fileName = fileName;  }


    public String getComments()                  { return comments;           }
    public void setComments(String comments)     { this.comments = comments;  }

    public String getStatus()                    { return status;           }
    public void setStatus(String status)         { this.status = status;    }

    public Integer getMarks()                    { return marks;          }
    public void setMarks(Integer marks)          { this.marks = marks;    }

    public String getFacultyRemarks()                      { return facultyRemarks;              }
    public void setFacultyRemarks(String facultyRemarks)   { this.facultyRemarks = facultyRemarks; }

    public LocalDateTime getSubmittedAt()                    { return submittedAt;              }
    public void setSubmittedAt(LocalDateTime submittedAt)    { this.submittedAt = submittedAt;  }

    public LocalDateTime getReviewedAt()                     { return reviewedAt;               }
    public void setReviewedAt(LocalDateTime reviewedAt)      { this.reviewedAt = reviewedAt;    }


    public int getResubmitCount()                    { return resubmitCount;              }
    public void setResubmitCount(int resubmitCount)  { this.resubmitCount = resubmitCount; }


    public LocalDateTime getUpdatedAt()                  { return updatedAt;            }
    public void setUpdatedAt(LocalDateTime updatedAt)    { this.updatedAt = updatedAt;  }

    public LocalDateTime getTaskDeadline()                   { return taskDeadline;             }
    public void setTaskDeadline(LocalDateTime taskDeadline)  { this.taskDeadline = taskDeadline; }

    public int getMaxMarks()                     { return maxMarks;           }
    public void setMaxMarks(int maxMarks)        { this.maxMarks = maxMarks;  }
    
    public int getFileSizeKB() {
        return fileSizeKB;
    }

    public void setFileSizeKB(int fileSizeKB) {
        this.fileSizeKB = fileSizeKB;
    }

    @Override
    public String toString() {
        return "Submission{id=" + id + ", taskId=" + taskId +
               ", studentId=" + studentId + ", status='" + status + "'}";
    }
}
