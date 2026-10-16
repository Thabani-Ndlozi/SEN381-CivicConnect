package com.civicconnect.media;

import com.civicconnect.audit.application.AuditService;
import com.civicconnect.request.application.CreateRequestCommand;
import com.civicconnect.request.application.ChangeStatusCommand;
import com.civicconnect.request.application.RequestService;
import com.civicconnect.request.application.RequestView;
import com.civicconnect.request.domain.ServiceRequest;
import com.civicconnect.request.persistence.ServiceRequestRepository;
import com.civicconnect.shared.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RequestMediaService {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private final JdbcTemplate jdbc;
    private final RequestService requests;
    private final ServiceRequestRepository repository;
    private final AuditService audit;

    public RequestMediaService(JdbcTemplate jdbc, RequestService requests,
                               ServiceRequestRepository repository, AuditService audit) {
        this.jdbc = jdbc;
        this.requests = requests;
        this.repository = repository;
        this.audit = audit;
    }

    public record MediaView(UUID id, UUID requestId, String kind, String comment,
                            String uploaderId, Instant createdAt, String fileName,
                            String contentType, boolean hasImage) {}
    public record ImageFile(byte[] bytes, String contentType, String fileName) {}
    record ValidatedFile(byte[] bytes, String contentType, String fileName) {}

    @Transactional
    public RequestView createWithImage(String requester, CreateRequestCommand command, MultipartFile image) {
        // Validate the entire upload BEFORE creating the request; transactional rollback on failures.
        ValidatedFile checked = validateImage(image);
        RequestView created = requests.create(requester, command);
        if (checked != null) {
            // RequestService uses JPA; flush before inserting the dependent row through JDBC.
            repository.flush();
            insert(created.id(), requester, "REQUEST_IMAGE", null, checked);
        }
        return created;
    }

    @Transactional
    public RequestView changeStatusWithImage(UUID requestId, String actor, ChangeStatusCommand command, MultipartFile image) {
        ValidatedFile checked = validateImage(image);
        RequestView updated = requests.changeStatus(requestId, actor, command);
        if (checked != null) {
            String note = command.comment() == null || command.comment().isBlank()
                    ? "Photo attached to status update" : command.comment().trim();
            insert(requestId, actor, "COMMENT", note, checked);
        }
        return updated;
    }

    @Transactional
    public MediaView addComment(UUID requestId, String actor, boolean manager, String text, MultipartFile image) {
        if (text == null || text.isBlank() || text.length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A comment (1–2000 characters) is required.");
        }
        ValidatedFile checked = validateImage(image);
        ServiceRequest request = repository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request not found."));
        if (!manager) request.assertOwnedBy(actor);
        UUID id = insert(requestId, actor, "COMMENT", text.trim(), checked);
        audit.record(requestId, actor, "COMMENT_ADDED", request.getStatus().name(),
                request.getStatus().name(), text.trim(), Instant.now());
        return getView(id);
    }

    @Transactional(readOnly = true)
    public List<MediaView> list(UUID requestId, String actor, boolean internal) {
        requireVisible(requestId, actor, internal);
        List<MediaView> rows = jdbc.query("""
            SELECT id, request_id, kind, comment, uploader_id, created_at,
                   original_name, content_type, (data IS NOT NULL) AS has_image
            FROM request_media WHERE request_id=? ORDER BY created_at ASC,id ASC
            """, (rs, n) -> toView(rs), requestId);
        // Do not reveal private staff/manager email addresses to requesters.
        if (internal) return rows;
        return rows.stream().map(row -> new MediaView(row.id(), row.requestId(), row.kind(),
                row.comment(), row.kind().equals("COMMENT") ? "Service team" : row.uploaderId(),
                row.createdAt(), row.fileName(), row.contentType(), row.hasImage())).toList();
    }

    @Transactional(readOnly = true)
    public ImageFile image(UUID requestId, UUID imageId, String actor, boolean internal) {
        requireVisible(requestId, actor, internal);
        return jdbc.query("""
                SELECT content_type, original_name, data FROM request_media
                WHERE id=? AND request_id=? AND data IS NOT NULL
                """, rs -> {
            if (!rs.next()) throw new NotFoundException("Image not found.");
            return new ImageFile(rs.getBytes("data"), rs.getString("content_type"), rs.getString("original_name"));
        }, imageId, requestId);
    }

    private MediaView getView(UUID id) {
        return jdbc.queryForObject("""
                SELECT id,request_id,kind,comment,uploader_id,created_at,original_name,content_type,
                       (data IS NOT NULL) AS has_image FROM request_media WHERE id=?
                """, (rs, n) -> toView(rs), id);
    }

    private static MediaView toView(ResultSet rs) throws SQLException {
        return new MediaView((UUID) rs.getObject("id"), (UUID) rs.getObject("request_id"),
                rs.getString("kind"), rs.getString("comment"), rs.getString("uploader_id"),
                rs.getTimestamp("created_at").toInstant(), rs.getString("original_name"),
                rs.getString("content_type"), rs.getBoolean("has_image"));
    }

    private void requireVisible(UUID requestId, String actor, boolean internal) {
        ServiceRequest request = repository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request not found."));
        if (!internal && !actor.equals(request.getRequesterId())) {
            throw new NotFoundException("Request not found.");
        }
    }

    private UUID insert(UUID requestId, String actor, String kind, String comment, ValidatedFile file) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
           INSERT INTO request_media(id,request_id,uploader_id,kind,comment,original_name,content_type,data)
           VALUES(?,?,?,?,?,?,?,?)
           """, id, requestId, actor, kind, comment,
                file == null ? null : file.fileName(), file == null ? null : file.contentType(),
                file == null ? null : file.bytes());
        return id;
    }

    // Ignore client-supplied content-type; recognize JPEG/PNG magic bytes and validate dimensions.
    static ValidatedFile validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) return null;
        if (image.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Image cannot exceed 5 MB.");
        }
        try {
            byte[] bytes = image.getBytes();
            if (bytes.length > MAX_BYTES) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Image cannot exceed 5 MB.");
            boolean png = bytes.length >= 8 && (bytes[0] & 255)==137 && bytes[1]==80 && bytes[2]==78 && bytes[3]==71 &&
                    bytes[4]==13 && bytes[5]==10 && bytes[6]==26 && bytes[7]==10;
            boolean jpeg = bytes.length >= 3 && (bytes[0] & 255)==255 && (bytes[1] & 255)==216 && (bytes[2] & 255)==255;
            if (!png && !jpeg) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload a genuine JPG or PNG image.");
            try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image cannot be decoded.");
                ImageReader reader = readers.next();
                try {
                    reader.setInput(input, true, true);
                    int width = reader.getWidth(0), height = reader.getHeight(0);
                    if (width < 1 || height < 1 || width > 8000 || height > 8000 || (long) width * height > 24_000_000L)
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image dimensions are too large.");
                    String format = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
                    if (png && !format.equals("png") || jpeg && !(format.equals("jpeg") || format.equals("jpg")))
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image format is invalid.");
                } finally { reader.dispose(); }
            }
            String raw = image.getOriginalFilename() == null ? "image" : image.getOriginalFilename();
            String safeName = raw.replaceAll("[^a-zA-Z0-9._-]", "_");
            safeName = safeName.substring(0, Math.min(160, safeName.length()));
            return new ValidatedFile(bytes, png ? "image/png" : "image/jpeg", safeName.isBlank() ? "image" : safeName);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read image.");
        }
    }
}
