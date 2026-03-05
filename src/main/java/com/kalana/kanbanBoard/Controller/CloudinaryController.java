package com.kalana.kanbanBoard.Controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/cloudinary")
@RequiredArgsConstructor
public class CloudinaryController {

    private final Cloudinary cloudinary;

    /**
     * Returns a Cloudinary upload signature for client-side direct uploads.
     * The frontend uses this to upload directly to Cloudinary, then POSTs
     * the resulting URL + public_id back to /api/work-items/{id}/attachments.
     */
    @GetMapping("/signature")
    public ResponseEntity<Map<String, Object>> getSignature(
            @RequestParam(defaultValue = "kanban_board") String folder) {
        try {
            long timestamp = System.currentTimeMillis() / 1000;
            Map<String, Object> params = Map.of(
                    "timestamp", timestamp,
                    "folder", folder);

            String signature = cloudinary.apiSignRequest(params, cloudinary.config.apiSecret);

            return ResponseEntity.ok(Map.of(
                    "signature", signature,
                    "timestamp", timestamp,
                    "apiKey", cloudinary.config.apiKey,
                    "cloudName", cloudinary.config.cloudName,
                    "folder", folder));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
