package in.ctrlplussubmit.dao;

import in.ctrlplussubmit.model.Notification;
import in.ctrlplussubmit.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

	// =====================================================
	// CREATE
	// =====================================================

	public boolean create(Notification notif) {

		String sql = """
				INSERT INTO notifications
				    (user_id, title, message, type, is_read, related_task_id)
				VALUES (?, ?, ?, ?, 0, ?)
				""";

		try (Connection conn = DBConnection.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			ps.setInt(1, notif.getUserId());
			ps.setString(2, notif.getTitle());
			ps.setString(3, notif.getMessage());
			ps.setString(4, notif.getType());

			if (notif.getRelatedTaskId() != null) {
				ps.setInt(5, notif.getRelatedTaskId());
			} else {
				ps.setNull(5, Types.INTEGER);
			}

			if (ps.executeUpdate() > 0) {
				try (ResultSet keys = ps.getGeneratedKeys()) {
					if (keys.next())
						notif.setId(keys.getInt(1));
				}
				return true;
			}

		} catch (SQLException e) {
			System.err.println("[NotificationDAO] create error: " + e.getMessage());
		}

		return false;
	}

	public boolean notify(int userId, String title, String message, String type, Integer relatedTaskId) {
		return create(new Notification(userId, title, message, type, relatedTaskId));
	}

	// =====================================================
	// READ
	// =====================================================

	public List<Notification> getAll(int userId) {

		List<Notification> list = new ArrayList<>();

		String sql = """
				SELECT * FROM notifications
				WHERE user_id = ?
				ORDER BY created_at DESC
				""";

		try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setInt(1, userId);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					list.add(mapRow(rs));
			}

		} catch (SQLException e) {
			System.err.println("[NotificationDAO] getAll error: " + e.getMessage());
		}

		return list;
	}

	public List<Notification> getUnread(int userId) {

		List<Notification> list = new ArrayList<>();

		String sql = """
				SELECT * FROM notifications
				WHERE user_id = ? AND is_read = 0
				ORDER BY created_at DESC
				""";

		try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setInt(1, userId);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					list.add(mapRow(rs));
			}

		} catch (SQLException e) {
			System.err.println("[NotificationDAO] getUnread error: " + e.getMessage());
		}

		return list;
	}

	public int countUnread(int userId) {

		String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0";

		try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, userId);

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getInt(1);
				}
			}

		} catch (SQLException e) {
			System.err.println("[NotificationDAO] countUnread error: " + e.getMessage());
		}

		return 0;
	}

	// =====================================================
	// MARK READ
	// =====================================================

	public boolean markRead(int notifId, int userId) {

		String sql = "UPDATE notifications SET is_read = 1 WHERE id = ? AND user_id = ?";

		try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setInt(1, notifId);
			ps.setInt(2, userId);

			return ps.executeUpdate() > 0;

		} catch (SQLException e) {
			System.err.println("[NotificationDAO] markRead error: " + e.getMessage());
		}

		return false;
	}

	public boolean markAllRead(int userId) {

		String sql = "UPDATE notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0";

		try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setInt(1, userId);
			ps.executeUpdate();

			return true;

		} catch (SQLException e) {
			System.err.println("[NotificationDAO] markAllRead error: " + e.getMessage());
		}

		return false;
	}

	// =====================================================
	// MAPPER
	// =====================================================

	private Notification mapRow(ResultSet rs) throws SQLException {

		Notification n = new Notification();

		n.setId(rs.getInt("id"));
		n.setUserId(rs.getInt("user_id"));
		n.setTitle(rs.getString("title"));
		n.setMessage(rs.getString("message"));
		n.setType(rs.getString("type"));
		n.setRead(rs.getBoolean("is_read"));

		int taskId = rs.getInt("related_task_id");
		if (!rs.wasNull())
			n.setRelatedTaskId(taskId);

		Timestamp createdAt = rs.getTimestamp("created_at");
		if (createdAt != null)
			n.setCreatedAt(createdAt.toLocalDateTime());

		return n;
	}
}