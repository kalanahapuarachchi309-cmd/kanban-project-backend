package com.kalana.kanbanBoard.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.kalana.kanbanBoard.dto.AttachmentDto;
import com.kalana.kanbanBoard.dto.AttachmentUploadRequest;
import com.kalana.kanbanBoard.entity.Attachment;
import com.kalana.kanbanBoard.entity.WorkItem;
import com.kalana.kanbanBoard.exception.ResourceNotFoundException;
import com.kalana.kanbanBoard.repository.AttachmentRepository;
import com.kalana.kanbanBoard.repository.WorkItemRepository;
import com.kalana.kanbanBoard.util.Mapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentService {

    private final Cloudinary cloudinary;
    private final AttachmentRepository attachmentRepository;
    private final WorkItemRepository workItemRepository;

    /**
     * Upload a file directly from the backend to Cloudinary and save the
     * attachment.
     */
    @Transactional
    public AttachmentDto uploadAndSave(Long workItemId, MultipartFile file) throws IOException {
        WorkItem workItem = workItemRepository.findById(workItemId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkItem not found: " + workItemId));

        String originalName = file.getOriginalFilename();
        String contentType = file.getContentType();
        String resourceType = resolveResourceType(contentType);

        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "resource_type", resourceType,
                "folder", "kanban_board/work_items/" + workItemId));

        String url = (String) uploadResult.get("secure_url");
        String publicId = (String) uploadResult.get("public_id");

        Attachment attachment = Attachment.builder()
                .workItem(workItem)
                .url(url)
                .publicId(publicId)
                .fileType(contentType)
                .originalName(originalName)
                .build();

        return Mapper.toAttachmentDto(attachmentRepository.save(attachment));
    }

    /**
     * Save an attachment record from a client-side direct Cloudinary upload.
     */
    @Transactional
    public AttachmentDto saveFromClientUpload(Long workItemId, AttachmentUploadRequest request) {
        WorkItem workItem = workItemRepository.findById(workItemId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkItem not found: " + workItemId));

        Attachment attachment = Attachment.builder()
                .workItem(workItem)
                .url(request.getUrl())
                .publicId(request.getPublicId())
                .fileType(request.getFileType())
                .originalName(request.getOriginalName())
                .build();

        return Mapper.toAttachmentDto(attachmentRepository.save(attachment));
    }

    public List<AttachmentDto> getAttachments(Long workItemId) {
        return attachmentRepository.findAllByWorkItemId(workItemId).stream()
                .map(Mapper::toAttachmentDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteAttachment(Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found: " + attachmentId));

        try {
            String resourceType = resolveResourceType(attachment.getFileType());
            cloudinary.uploader().destroy(attachment.getPublicId(), ObjectUtils.asMap(
                    "resource_type", resourceType));
        } catch (Exception e) {
            log.warn("Failed to delete from Cloudinary: {}", e.getMessage());
        }

        attachmentRepository.delete(attachment);
    }

    private String resolveResourceType(String contentType) {
        if (contentType == null)
            return "raw";
        if (contentType.startsWith("image/"))
            return "image";
        if (contentType.equals("application/pdf"))
            return "raw";
        if (contentType.startsWith("video/"))
            return "video";
        return "raw";
    }
}
