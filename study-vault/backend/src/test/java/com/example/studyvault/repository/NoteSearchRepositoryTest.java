package com.example.studyvault.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.studyvault.entity.Note;
import com.example.studyvault.entity.NoteTag;
import com.example.studyvault.entity.Tag;
import com.example.studyvault.entity.User;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Autowired;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:searchdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class NoteSearchRepositoryTest {
    private final NoteRepository notes;
    private final UserRepository users;
    private final TagRepository tags;
    private final NoteTagRepository noteTags;
    private User alice;
    private User bob;

    @Autowired
    NoteSearchRepositoryTest(NoteRepository notes, UserRepository users, TagRepository tags, NoteTagRepository noteTags) {
        this.notes = notes;
        this.users = users;
        this.tags = tags;
        this.noteTags = noteTags;
    }

    @BeforeEach
    void setUp() {
        alice = users.save(user("alice", "alice@example.com"));
        bob = users.save(user("bob", "bob@example.com"));
        Tag math = tags.save(tag(alice, "Math"));
        tags.save(tag(alice, "History"));

        Note matching = notes.save(note(alice, "Graph algorithms", "Needle content", "active", true));
        notes.save(note(alice, "Other", "Unrelated", "active", false));
        notes.save(note(alice, "Trashed needle", "Needle content", "trash", true));
        Note bobNote = notes.save(note(bob, "Graph algorithms", "Needle content", "active", true));
        noteTags.save(new NoteTag(matching, math));
        noteTags.save(new NoteTag(bobNote, tags.save(tag(bob, "Math"))));
    }

    @Test
    void combinesQueryTagFavoriteStatusPagingAndSort() {
        var page = notes.findAll(NoteSpecifications.search(alice, "needle", "math", true, "active"),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "updatedAt")));

        assertEquals(1, page.getTotalElements());
        assertEquals("Graph algorithms", page.getContent().get(0).getTitle());
    }

    @Test
    void ownershipPredicateExcludesAnotherUsersMatchingNote() {
        var page = notes.findAll(NoteSpecifications.search(alice, "needle", null, null, "active"), PageRequest.of(0, 20));

        assertEquals(1, page.getTotalElements());
        org.junit.jupiter.api.Assertions.assertTrue(page.getContent().stream().noneMatch(note -> note.getUser() != alice));
    }

    private static User user(String username, String email) {
        User user = new User();
        user.setUsername(username); user.setEmail(email); user.setPasswordHash("hash");
        return user;
    }

    private static Tag tag(User user, String name) {
        Tag tag = new Tag(); tag.setUser(user); tag.setName(name); return tag;
    }

    private static Note note(User user, String title, String content, String status, boolean favorite) {
        Note note = new Note(); note.setUser(user); note.setTitle(title); note.setContent(content); note.setStatus(status); note.setFavorite(favorite); return note;
    }
}
