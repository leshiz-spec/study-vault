package com.example.studyvault.repository;

import com.example.studyvault.entity.Note;
import com.example.studyvault.entity.NoteTag;
import com.example.studyvault.entity.Tag;
import com.example.studyvault.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Database predicates used by note search; no notes are loaded before filtering. */
public final class NoteSpecifications {
    private NoteSpecifications() { }

    public static Specification<Note> search(User user, String queryText, String tag, Boolean favorite, String status) {
        return (root, query, cb) -> {
            query.distinct(true);
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.equal(root.get("user"), user));

            if (queryText != null && !queryText.isBlank()) {
                String pattern = "%" + queryText.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("content")), pattern)));
            }
            if (favorite != null) predicates.add(cb.equal(root.get("favorite"), favorite));
            if (status != null && !status.isBlank()) predicates.add(cb.equal(root.get("status"), status.trim()));
            if (tag != null && !tag.isBlank()) predicates.add(hasOwnedTag(root, query, cb, user, tag.trim()));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate hasOwnedTag(Root<Note> note, jakarta.persistence.criteria.CriteriaQuery<?> query,
                                         jakarta.persistence.criteria.CriteriaBuilder cb, User user, String tagValue) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<NoteTag> noteTag = subquery.from(NoteTag.class);
        Join<NoteTag, Tag> tag = noteTag.join("tag");
        Predicate tagMatch = cb.equal(cb.lower(tag.get("name")), tagValue.toLowerCase(Locale.ROOT));
        try {
            tagMatch = cb.or(tagMatch, cb.equal(tag.get("id"), Long.valueOf(tagValue)));
        } catch (NumberFormatException ignored) {
            // A non-numeric tag parameter is treated as a tag name.
        }
        subquery.select(cb.literal(1L)).where(
                cb.equal(noteTag.get("note"), note),
                cb.equal(tag.get("user"), user),
                tagMatch);
        return cb.exists(subquery);
    }
}
