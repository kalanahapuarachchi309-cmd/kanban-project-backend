package com.kalana.kanbanBoard.util;

import com.kalana.kanbanBoard.dto.WorkItemFilterRequest;
import com.kalana.kanbanBoard.entity.WorkItem;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class WorkItemSpecification {

    public static Specification<WorkItem> withFilter(Long projectId, WorkItemFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("project").get("id"), projectId));

            if (filter == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            if (filter.getType() != null) {
                predicates.add(cb.equal(root.get("type"), filter.getType()));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getPriority() != null) {
                predicates.add(cb.equal(root.get("priority"), filter.getPriority()));
            }
            if (filter.getAssignedTo() != null) {
                predicates.add(cb.equal(root.get("assignedTo").get("id"), filter.getAssignedTo()));
            }
            if (filter.getCreatedBy() != null) {
                predicates.add(cb.equal(root.get("createdBy").get("id"), filter.getCreatedBy()));
            }
            if (filter.getCreatedFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getCreatedFrom()));
            }
            if (filter.getCreatedTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getCreatedTo()));
            }
            if (filter.getDueFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dueAt"), filter.getDueFrom()));
            }
            if (filter.getDueTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dueAt"), filter.getDueTo()));
            }
            if (filter.getClientReviewStatus() != null) {
                predicates.add(cb.equal(root.get("clientReviewStatus"), filter.getClientReviewStatus()));
            }
            if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
                String like = "%" + filter.getSearch().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("description")), like)));
            }

            // Sorting
            if (filter.getSortBy() != null) {
                query.orderBy(switch (filter.getSortBy()) {
                    case "oldest" -> cb.asc(root.get("createdAt"));
                    case "due_soon" -> cb.asc(root.get("dueAt"));
                    case "priority" -> cb.desc(root.get("priority"));
                    default -> cb.desc(root.get("createdAt")); // newest
                });
            } else {
                query.orderBy(cb.desc(root.get("createdAt")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
