 package in.ctrlplussubmit.model;

import java.time.LocalDateTime;

public class Task {

    private int id;
    private String title;
    private String description;

    private int facultyId;
    private String facultyName;

    private LocalDateTime deadline;

    // assign_type / batchId / studentId removed — those live in task_assignments

    private String attachFilePath;
    private String attachFileName;

    private int maxMarks;
    private boolean isActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Dashboard / joined stats
    private int totalAssigned;
    private int totalSubmitted;
    private int pendingReview;
    private String deadlineStatus;

    public Task() {}

    public Task(String title, String description, int facultyId,
                LocalDateTime deadline, int maxMarks) {
        this.title       = title;
        this.description = description;
        this.facultyId   = facultyId;
        this.deadline    = deadline;
        this.maxMarks    = maxMarks;
        this.isActive    = true;
    }

    public boolean isOverdue() {
        return deadline != null && LocalDateTime.now().isAfter(deadline);
    }

    public boolean hasAttachment() {
        return attachFilePath != null && !attachFilePath.isBlank();
    }

    // Getters & Setters ──────────────────────────────────────

    public int getId()                            { return id; }
    public void setId(int id)                     { this.id = id; }

    public String getTitle()                      { return title; }
    public void setTitle(String title)            { this.title = title; }

    public String getDescription()                { return description; }
    public void setDescription(String desc)       { this.description = desc; }

    public int getFacultyId()                     { return facultyId; }
    public void setFacultyId(int facultyId)       { this.facultyId = facultyId; }

    public String getFacultyName()                { return facultyName; }
    public void setFacultyName(String n)          { this.facultyName = n; }

    public LocalDateTime getDeadline()            { return deadline; }
    public void setDeadline(LocalDateTime dl)     { this.deadline = dl; }

    public String getAttachFilePath()             { return attachFilePath; }
    public void setAttachFilePath(String p)       { this.attachFilePath = p; }

    public String getAttachFileName()             { return attachFileName; }
    public void setAttachFileName(String n)       { this.attachFileName = n; }

    public int getMaxMarks()                      { return maxMarks; }
    public void setMaxMarks(int m)                { this.maxMarks = m; }

    public boolean isActive()                     { return isActive; }
    public void setActive(boolean active)         { this.isActive = active; }

    public LocalDateTime getCreatedAt()           { return createdAt; }
    public void setCreatedAt(LocalDateTime t)     { this.createdAt = t; }

    public LocalDateTime getUpdatedAt()           { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t)     { this.updatedAt = t; }

    public int getTotalAssigned()                 { return totalAssigned; }
    public void setTotalAssigned(int n)           { this.totalAssigned = n; }

    public int getTotalSubmitted()                { return totalSubmitted; }
    public void setTotalSubmitted(int n)          { this.totalSubmitted = n; }

    public int getPendingReview()                 { return pendingReview; }
    public void setPendingReview(int n)           { this.pendingReview = n; }

    public String getDeadlineStatus()             { return deadlineStatus; }
    public void setDeadlineStatus(String s)       { this.deadlineStatus = s; }

    @Override
    public String toString() {
        return "Task{id=" + id + ", title='" + title + "', deadline=" + deadline + '}';
    }
}