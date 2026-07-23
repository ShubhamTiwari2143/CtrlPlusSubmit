package in.ctrlplussubmit.controller.faculty;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import in.ctrlplussubmit.util.JsonUtil;
import in.ctrlplussubmit.util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * FileDownloadServlet — Streams uploaded files to the browser for download
 *
 * URL: GET /faculty/download?file=submissions/uuid_file.pdf&name=MyAssignment.pdf
 *
 * Security:
 *   - User must be authenticated (any role)
 *   - Path traversal attacks blocked (no ".." allowed in file param)
 *   - File must exist inside the configured UPLOAD_DIR
 *
 * Access: ALL authenticated roles (faculty downloads submissions,
 *         students download task attachments via same servlet)
 */
@WebServlet("/faculty/download")
public class FileDownloadServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Must be authenticated — any role can download
        if (!SessionUtil.requireLogin(request, response)) return;

        String fileParam  = request.getParameter("file");  // relative path stored in DB
        String nameParam  = request.getParameter("name");  // original filename for Content-Disposition

        // Validate parameters
        if (fileParam == null || fileParam.isBlank()) {
            JsonUtil.sendBadRequest(response, "Missing file parameter.");
            return;
        }

        // Security: block path traversal
        if (fileParam.contains("..") || fileParam.contains("\\")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file path.");
            return;
        }

        String uploadDir = getServletContext().getInitParameter("UPLOAD_DIR");
        if (uploadDir == null || uploadDir.isBlank()) {
            JsonUtil.sendServerError(response, "Server configuration error: UPLOAD_DIR not set.");
            return;
        }

        // Resolve full path and verify it stays within uploadDir
        Path uploadRoot = Paths.get(uploadDir).toRealPath();
        Path filePath;
        try {
        	filePath = Paths.get(uploadDir, fileParam).toAbsolutePath().normalize();
            // Ensure file is inside uploadDir (prevents symlink escape)
            if (!filePath.startsWith(uploadRoot)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
                return;
            }
        } catch (IOException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
            return;
        }

        File file = filePath.toFile();
        if (!file.exists() || !file.isFile()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                "The requested file no longer exists on the server.");
            return;
        }

        // Determine MIME type
        String mimeType = getServletContext().getMimeType(file.getName());
        if (mimeType == null) mimeType = "application/octet-stream";

        // Safe download filename
        String downloadName = (nameParam != null && !nameParam.isBlank())
            ? nameParam.trim()
            : file.getName();
        // RFC 5987 encoding for non-ASCII filenames
        String encodedName = URLEncoder.encode(downloadName, StandardCharsets.UTF_8)
                                       .replace("+", "%20");

        // Set response headers
        response.setContentType(mimeType);
        response.setContentLengthLong(file.length());
        response.setHeader("Content-Disposition",
            "attachment; filename=\"" + downloadName + "\"; filename*=UTF-8''" + encodedName);
        response.setHeader("Cache-Control", "no-cache");

        // Stream file bytes to response
        try (InputStream in  = new BufferedInputStream(new FileInputStream(file));
             OutputStream out = new BufferedOutputStream(response.getOutputStream())) {
            byte[] buffer = new byte[8192];
            int    bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
}

