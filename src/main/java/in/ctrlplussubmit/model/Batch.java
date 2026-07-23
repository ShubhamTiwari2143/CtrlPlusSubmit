package in.ctrlplussubmit.model;

import java.time.LocalDateTime;

/**
 * Batch — Updated POJO
 *
 * Two new fields added to support the Faculty Dashboard batch cards:
 *   activeTasks    — tasks currently assigned to this batch
 *   upcomingCount  — active tasks with deadline in the next 7 days
 *
 * Both are "joined" fields — populated only by BatchDAO.findByFacultyWithStats().
 * They are not columns in the `batches` table.
 */
public class Batch {

    private int           id;
    private String        batchName;
    private String        description;
    private int           facultyId;
    private String        facultyName;
    private boolean       isActive;
    private int           studentCount;
    private int           activeTasks;      // NEW — populated by findByFacultyWithStats()
    private int           upcomingCount;    // NEW — tasks due within next 7 days
    private LocalDateTime createdAt;

    // -------------------------------------------------------
    //  Constructors
    // -------------------------------------------------------

    public Batch() {}

    public Batch(String batchName, String description, int facultyId) {
        this.batchName   = batchName;
        this.description = description;
        this.facultyId   = facultyId;
        this.isActive    = true;
    }

    // -------------------------------------------------------
    //  Getters & Setters
    // -------------------------------------------------------

    public int getId()                          { return id;           }
    public void setId(int id)                   { this.id = id;        }

    public String getBatchName()                       { return batchName;             }
    public void setBatchName(String batchName)         { this.batchName = batchName;   }

    public String getDescription()                     { return description;            }
    public void setDescription(String description)     { this.description = description; }

    public int getFacultyId()                          { return facultyId;              }
    public void setFacultyId(int facultyId)            { this.facultyId = facultyId;    }

    public String getFacultyName()                     { return facultyName;            }
    public void setFacultyName(String facultyName)     { this.facultyName = facultyName; }

    public boolean isActive()                          { return isActive;               }
    public void setActive(boolean active)              { this.isActive = active;        }

    public int getStudentCount()                       { return studentCount;           }
    public void setStudentCount(int studentCount)      { this.studentCount = studentCount; }

    /** Active tasks assigned to this batch (joined field, not in DB column). */
    public int getActiveTasks()                        { return activeTasks;            }
    public void setActiveTasks(int activeTasks)        { this.activeTasks = activeTasks; }

    /** Active tasks due within 7 days (joined field, not in DB column). */
    public int getUpcomingCount()                      { return upcomingCount;          }
    public void setUpcomingCount(int upcomingCount)    { this.upcomingCount = upcomingCount; }

    public LocalDateTime getCreatedAt()                    { return createdAt;              }
    public void setCreatedAt(LocalDateTime createdAt)      { this.createdAt = createdAt;    }

    @Override
    public String toString() {
        return "Batch{id=" + id + ", batchName='" + batchName +
               "', students=" + studentCount + ", activeTasks=" + activeTasks + '}';
    }
}
