package in.ctrlplussubmit.model;


import java.time.LocalDateTime;

/**
 * TaskAssignment — POJO for the `task_assignments` table
 *
 * Represents the link between a Task and its assigned target:
 *   - STUDENT  → task assigned to one specific student
 *   - BATCH    → task assigned to all students in a batch
 *
 * When a BATCH assignment is created, the TaskAssignmentDAO
 * automatically expands it into individual submission rows
 * for every student in that batch, so each student sees the task.
 */
public class TaskAssignment {

    private int           id;
    private int           taskId;
    private String        assignmentType;  // "INDIVIDUAL" or "BATCH"
    private int           studentId;       // Set when assignmentType = INDIVIDUAL
    private int           batchId;         // Set when assignmentType = BATCH
    private String        studentName;     // Joined field
    private String        batchName;       // Joined field
    private LocalDateTime assignedAt;

    // -------------------------------------------------------
    //  Constructors
    // -------------------------------------------------------

    public TaskAssignment() {}

    /** Factory for student assignment */
    public static TaskAssignment forStudent(int taskId, int studentId) {
        TaskAssignment ta = new TaskAssignment();
        ta.taskId         = taskId;
        ta.assignmentType = "STUDENT";
        ta.studentId      = studentId;
        return ta;
    }

    /** Factory for batch assignment */
    public static TaskAssignment forBatch(int taskId, int batchId) {
        TaskAssignment ta = new TaskAssignment();
        ta.taskId         = taskId;
        ta.assignmentType = "BATCH";
        ta.batchId        = batchId;
        return ta;
    }

    // -------------------------------------------------------
    //  Getters & Setters
    // -------------------------------------------------------

    public int getId()                        { return id;             }
    public void setId(int id)                 { this.id = id;          }

    public int getTaskId()                    { return taskId;         }
    public void setTaskId(int taskId)         { this.taskId = taskId;  }

    public String getAssignmentType()                        { return assignmentType;                }
    public void setAssignmentType(String assignmentType)     { this.assignmentType = assignmentType; }

    public int getStudentId()                 { return studentId;           }
    public void setStudentId(int studentId)   { this.studentId = studentId; }

    public int getBatchId()                   { return batchId;           }
    public void setBatchId(int batchId)       { this.batchId = batchId;   }

    public String getStudentName()                   { return studentName;            }
    public void setStudentName(String studentName)   { this.studentName = studentName; }

    public String getBatchName()                     { return batchName;              }
    public void setBatchName(String batchName)       { this.batchName = batchName;    }

    public LocalDateTime getAssignedAt()                   { return assignedAt;             }
    public void setAssignedAt(LocalDateTime assignedAt)    { this.assignedAt = assignedAt;  }

    @Override
    public String toString() {
        return "TaskAssignment{id=" + id + ", taskId=" + taskId +
               ", type='" + assignmentType + "', studentId=" + studentId +
               ", batchId=" + batchId + '}';
    }
}

