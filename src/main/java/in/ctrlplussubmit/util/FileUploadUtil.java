package in.ctrlplussubmit.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.http.Part;

/**
 * FileUploadUtil — Multipart File Upload Handler
 *
 * Handles all file uploads across the application:
 *   - Task instruction attachments  → /uploads/tasks/
 *   - Student submission files      → /uploads/submissions/
 *   - Profile pictures              → /uploads/profiles/
 *
 * Features:
 *   ✅ File type validation (whitelist)
 *   ✅ Max file size enforcement
 *   ✅ UUID-based unique filename generation (prevents overwrite)
 *   ✅ Subdirectory auto-creation
 *   ✅ Original filename sanitization (removes special characters)
 *
 * Usage in a Servlet (must be annotated with @MultipartConfig):
 *
 *   Part filePart = request.getPart("submissionFile");
 *   String uploadDir = getServletContext().getInitParameter("UPLOAD_DIR");
 *
 *   FileUploadUtil.UploadResult result =
 *       FileUploadUtil.upload(filePart, uploadDir, FileUploadUtil.CONTEXT_SUBMISSIONS);
 *
 *   if (result.isSuccess()) {
 *       String savedPath = result.getStoredPath();  // save to DB
 *   } else {
 *       // show result.getErrorMessage() to user
 *   }
 */
public class FileUploadUtil {

    // -------------------------------------------------------
    //  Upload context subdirectories
    // -------------------------------------------------------
    public static final String CONTEXT_TASKS       = "tasks";
    public static final String CONTEXT_SUBMISSIONS = "submissions";
    public static final String CONTEXT_PROFILES    = "profiles";

    // -------------------------------------------------------
    //  File type whitelists (lowercase extensions)
    // -------------------------------------------------------
    /** Allowed types for student submissions */
    public static final Set<String> ALLOWED_SUBMISSION_TYPES = Set.of(
            "pdf", "doc", "docx", "zip", "rar", "jpg", "jpeg", "png"
    );

    /** Allowed types for task instruction attachments (faculty) */
    public static final Set<String> ALLOWED_TASK_TYPES = Set.of(
            "pdf", "doc", "docx", "txt", "png", "jpg", "jpeg", "zip"
    );

    /** Allowed types for profile pictures */
    public static final Set<String> ALLOWED_PROFILE_TYPES = Set.of(
            "jpg", "jpeg", "png", "webp"
    );

    // -------------------------------------------------------
    //  Size limits
    // -------------------------------------------------------
    /** Maximum file size in bytes (20 MB) */
    public static final long MAX_FILE_SIZE_BYTES = 20L * 1024 * 1024;

    /** Maximum profile picture size in bytes (2 MB) */
    public static final long MAX_PROFILE_SIZE_BYTES = 2L * 1024 * 1024;

    // Private constructor — static utility class
    private FileUploadUtil() {}

    // -------------------------------------------------------
    //  MAIN UPLOAD METHOD
    // -------------------------------------------------------

    /**
     * Validates and saves an uploaded file to the server's local directory.
     *
     * @param filePart    the Part object from request.getPart("fieldName")
     * @param uploadDir   the root upload directory (from web.xml UPLOAD_DIR param)
     * @param context     subdirectory — use CONTEXT_TASKS, CONTEXT_SUBMISSIONS, CONTEXT_PROFILES
     * @return            UploadResult containing success status, stored path, or error message
     */
    public static UploadResult upload(Part filePart, String uploadDir, String context) {

        // 1. Validate: Part must not be null
        if (filePart == null) {
            return UploadResult.failure("No file was uploaded.");
        }

        // 2. Extract original filename
        String originalFileName = getFileName(filePart);
        if (originalFileName == null || originalFileName.isBlank()) {
            return UploadResult.failure("File name is missing.");
        }

        // 3. Extract & validate file extension
        String extension = getExtension(originalFileName).toLowerCase();
        Set<String> allowedTypes = getAllowedTypes(context);

        if (!allowedTypes.contains(extension)) {
            return UploadResult.failure(
                "File type '." + extension + "' is not allowed. " +
                "Allowed types: " + String.join(", ", allowedTypes)
            );
        }

        // 4. Validate file size
        long maxSize = CONTEXT_PROFILES.equals(context) ? MAX_PROFILE_SIZE_BYTES : MAX_FILE_SIZE_BYTES;
        if (filePart.getSize() > maxSize) {
            long maxMB = maxSize / (1024 * 1024);
            return UploadResult.failure("File exceeds the maximum allowed size of " + maxMB + " MB.");
        }

        // 5. Generate unique filename: UUID_sanitizedOriginalName.ext
        String sanitizedName = sanitizeFileName(originalFileName);
        String storedFileName = UUID.randomUUID().toString() + "_" + sanitizedName;

        // 6. Ensure upload subdirectory exists
        String subDirPath = uploadDir + File.separator + context;
        File subDir = new File(subDirPath);
        if (!subDir.exists()) {
            boolean created = subDir.mkdirs();
            if (!created) {
                return UploadResult.failure("Server error: Could not create upload directory.");
            }
        }

        // 7. Save file to disk
        String fullPath = subDirPath + File.separator + storedFileName;
        try (InputStream inputStream = filePart.getInputStream()) {
            Files.copy(inputStream, Paths.get(fullPath), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            return UploadResult.failure("Server error: Failed to save the file. " + e.getMessage());
        }

        // 8. Return success result
        long fileSizeKB = filePart.getSize() / 1024;
        return UploadResult.success(
            originalFileName,
            storedFileName,
            context + "/" + storedFileName,  // relative path stored in DB
            fullPath,                          // absolute path on server
            extension,
            fileSizeKB
        );
    }

    // -------------------------------------------------------
    //  DELETE — remove a file from disk
    // -------------------------------------------------------

    /**
     * Deletes a previously uploaded file from disk.
     * Call this when a student replaces their submission file
     * or when a task is deleted.
     *
     * @param uploadDir   root upload directory
     * @param relativePath the path stored in DB (e.g. "submissions/uuid_file.pdf")
     * @return true if deleted successfully
     */
    public static boolean deleteFile(String uploadDir, String relativePath) {
        if (uploadDir == null || relativePath == null) return false;
        File file = new File(uploadDir + File.separator + relativePath);
        return file.exists() && file.delete();
    }

    // -------------------------------------------------------
    //  HELPER METHODS
    // -------------------------------------------------------

    /**
     * Extracts the file name from a multipart Part's Content-Disposition header.
     */
    public static String getFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        if (contentDisposition == null) return null;

        for (String token : contentDisposition.split(";")) {
            token = token.trim();
            if (token.startsWith("filename")) {
                String name = token.substring(token.indexOf('=') + 1).trim();
                // Remove surrounding quotes if present
                name = name.replace("\"", "");
                // Handle Windows paths submitted by IE/Edge (legacy)
                int lastSlash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
                return (lastSlash >= 0) ? name.substring(lastSlash + 1) : name;
            }
        }
        return null;
    }

    /**
     * Extracts the lowercase file extension from a filename.
     * Returns empty string if no extension is found.
     */
    public static String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    /**
     * Sanitizes a filename by removing characters that could cause
     * filesystem or security issues. Keeps letters, digits, dots, dashes, underscores.
     */
    public static String sanitizeFileName(String originalName) {
        // Replace spaces with underscores, strip everything else except safe chars
        return originalName
                .trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-zA-Z0-9._\\-]", "");
    }

    /**
     * Returns the allowed file types Set for a given upload context.
     */
    private static Set<String> getAllowedTypes(String context) {
        return switch (context) {
            case CONTEXT_PROFILES    -> ALLOWED_PROFILE_TYPES;
            case CONTEXT_TASKS       -> ALLOWED_TASK_TYPES;
            default                  -> ALLOWED_SUBMISSION_TYPES;
        };
    }

    // -------------------------------------------------------
    //  RESULT OBJECT — returned by upload()
    // -------------------------------------------------------

    /**
     * Encapsulates the result of a file upload operation.
     */
    public static class UploadResult {
        private final boolean  success;
        private final String   errorMessage;
        private final String   originalName;
        private final String   storedName;
        private final String   storedPath;      // relative — stored in DB
        private final String   absolutePath;    // absolute — used for file streaming
        private final String   fileType;        // extension e.g. "pdf"
        private final long     fileSizeKB;

        private UploadResult(boolean success, String errorMessage,
                             String originalName, String storedName,
                             String storedPath, String absolutePath,
                             String fileType, long fileSizeKB) {
            this.success      = success;
            this.errorMessage = errorMessage;
            this.originalName = originalName;
            this.storedName   = storedName;
            this.storedPath   = storedPath;
            this.absolutePath = absolutePath;
            this.fileType     = fileType;
            this.fileSizeKB   = fileSizeKB;
        }

        public static UploadResult success(String originalName, String storedName,
                                           String storedPath, String absolutePath,
                                           String fileType, long fileSizeKB) {
            return new UploadResult(true, null, originalName, storedName,
                                    storedPath, absolutePath, fileType, fileSizeKB);
        }

        public static UploadResult failure(String errorMessage) {
            return new UploadResult(false, errorMessage,
                                    null, null, null, null, null, 0);
        }

        public boolean  isSuccess()      { return success;      }
        public String   getErrorMessage(){ return errorMessage;  }
        public String   getOriginalName(){ return originalName;  }
        public String   getStoredName()  { return storedName;    }
        public String   getStoredPath()  { return storedPath;    }
        public String   getAbsolutePath(){ return absolutePath;  }
        public String   getFileType()    { return fileType;      }
        public long     getFileSizeKB()  { return fileSizeKB;    }
    }
}
