package in.ctrlplussubmit.controller.faculty;


import in.ctrlplussubmit.dao.BatchDAO;
import in.ctrlplussubmit.dao.TaskDAO;
import in.ctrlplussubmit.model.Batch;
import in.ctrlplussubmit.model.User;
import in.ctrlplussubmit.util.JsonUtil;
import in.ctrlplussubmit.util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FacultyDashboardServlet — Updated to include batch stats
 *
 * GET /faculty/dashboard
 *
 * Response now includes:
 *   stats   : { totalTasks, pendingReview, reviewed }
 *   batches : [ { id, batchName, description, studentCount,
 *                 activeTasks, upcomingCount, isActive } ... ]
 *   faculty : { fullName, id }
 *
 * Access: FACULTY only
 */
@WebServlet("/faculty/dashboard")
public class FacultyDashboardServlet extends HttpServlet {

    private final TaskDAO  taskDAO  = new TaskDAO();
    private final BatchDAO batchDAO = new BatchDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!SessionUtil.requireRole(request, response, "FACULTY")) return;

        User faculty = SessionUtil.getLoggedInUser(request);
        int  fid     = faculty.getId();

        // ── Task stats (unchanged) ──────────────────────────
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTasks",    taskDAO.countByFaculty(fid));
        stats.put("pendingReview", taskDAO.countPendingReview(fid));
        stats.put("reviewed",      taskDAO.countReviewed(fid));

        // ── Batch list WITH stats (new) ──────────────────────
        List<Batch> batches = batchDAO.findByFacultyWithStats(fid);

        // Convert to safe maps for JSON (no unnecessary internal fields)
        List<Map<String, Object>> batchList = batches.stream()
            .map(b -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id",            b.getId());
                m.put("batchName",     b.getBatchName());
                m.put("description",   b.getDescription());
                m.put("studentCount",  b.getStudentCount());
                m.put("activeTasks",   b.getActiveTasks());
                m.put("upcomingCount", b.getUpcomingCount());
                m.put("isActive",      b.isActive());
                return m;
            })
            .collect(java.util.stream.Collectors.toList());

        // ── Faculty info ─────────────────────────────────────
        Map<String, Object> info = new HashMap<>();
        info.put("fullName", faculty.getFullName());
        info.put("id",       fid);

        // ── Final payload ────────────────────────────────────
        Map<String, Object> data = new HashMap<>();
        data.put("stats",   stats);
        data.put("batches", batchList);
        data.put("faculty", info);

        JsonUtil.sendSuccess(response, "Faculty dashboard loaded.", data);
    }
}
