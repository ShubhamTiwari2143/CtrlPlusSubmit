package in.ctrlplussubmit.util;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * JsonUtil — Gson-based JSON Response Helper
 *
 * Provides a consistent JSON response format for all servlet endpoints:
 *
 *   Success:  { "success": true,  "message": "Done",   "data": { ... } }
 *   Error:    { "success": false, "message": "Failed",  "data": null    }
 *
 * Usage in a Servlet:
 *
 *   // Send a success response with data
 *   JsonUtil.sendSuccess(response, "Login successful", userMap);
 *
 *   // Send an error response
 *   JsonUtil.sendError(response, HttpServletResponse.SC_401, "Invalid credentials");
 */
public class JsonUtil {

    // Gson instance — pretty printing disabled for bandwidth efficiency
    // Use GsonBuilder if you need date formatting or field exclusion strategies
	private static final Gson GSON = new GsonBuilder()
	        .serializeNulls()
	        .setPrettyPrinting()
	        .registerTypeAdapter(java.time.LocalDate.class, new LocalDateAdapter())
	        .registerTypeAdapter(java.time.LocalTime.class, new LocalTimeAdapter())
	        .registerTypeAdapter(java.time.LocalDateTime.class, new LocalDateTimeAdapter())
	        .create();

    // Private constructor — static utility class
    private JsonUtil() {}

    // -------------------------------------------------------
    //  CORE RESPONSE SENDER
    // -------------------------------------------------------

    /**
     * Writes a JSON string to the HttpServletResponse.
     * Sets Content-Type to application/json and UTF-8 encoding.
     *
     * @param response   HttpServletResponse
     * @param statusCode HTTP status code (200, 400, 401, 403, 500, etc.)
     * @param jsonString The JSON string to write
     */
    public static void sendJson(HttpServletResponse response,
                                int statusCode,
                                String jsonString) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Prevent caching of API responses
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");

        PrintWriter out = response.getWriter();
        out.print(jsonString);
        out.flush();
    }

    // -------------------------------------------------------
    //  SUCCESS RESPONSES
    // -------------------------------------------------------

    /**
     * Sends HTTP 200 with { "success": true, "message": "...", "data": { ... } }
     *
     * @param data  any Object — will be serialized to JSON (can be Map, List, POJO, etc.)
     */
    public static void sendSuccess(HttpServletResponse response,
                                   String message,
                                   Object data) throws IOException {
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("message", message);
        body.put("data", data);
        sendJson(response, HttpServletResponse.SC_OK, GSON.toJson(body));
    }

    /**
     * Sends HTTP 200 with { "success": true, "message": "..." } (no data payload)
     */
    public static void sendSuccess(HttpServletResponse response,
                                   String message) throws IOException {
        sendSuccess(response, message, null);
    }

    // -------------------------------------------------------
    //  ERROR RESPONSES
    // -------------------------------------------------------

    /**
     * Sends an error response with a custom HTTP status code.
     *
     * @param statusCode  e.g. 400, 401, 403, 404, 500
     * @param message     human-readable error description
     */
    public static void sendError(HttpServletResponse response,
                                 int statusCode,
                                 String message) throws IOException {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", message);
        body.put("data", null);
        sendJson(response, statusCode, GSON.toJson(body));
    }

    /** HTTP 400 Bad Request */
    public static void sendBadRequest(HttpServletResponse response,
                                      String message) throws IOException {
        sendError(response, HttpServletResponse.SC_BAD_REQUEST, message);
    }

    /** HTTP 401 Unauthorized */
    public static void sendUnauthorized(HttpServletResponse response,
                                        String message) throws IOException {
        sendError(response, HttpServletResponse.SC_UNAUTHORIZED, message);
    }

    /** HTTP 403 Forbidden */
    public static void sendForbidden(HttpServletResponse response,
                                     String message) throws IOException {
        sendError(response, HttpServletResponse.SC_FORBIDDEN, message);
    }

    /** HTTP 404 Not Found */
    public static void sendNotFound(HttpServletResponse response,
                                    String message) throws IOException {
        sendError(response, HttpServletResponse.SC_NOT_FOUND, message);
    }

    /** HTTP 500 Internal Server Error */
    public static void sendServerError(HttpServletResponse response,
                                       String message) throws IOException {
        sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message);
    }

    // -------------------------------------------------------
    //  UTILITY — serialize any object
    // -------------------------------------------------------

    /**
     * Converts any Java object to a JSON string.
     * Useful when you need to build the JSON string manually.
     */
    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    /**
     * Parses a JSON string into a Java object.
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }
}
