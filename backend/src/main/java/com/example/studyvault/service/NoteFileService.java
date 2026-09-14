package com.example.studyvault.service;

import com.example.studyvault.dto.NoteCreateRequest;
import com.example.studyvault.dto.NoteResponse;
import com.example.studyvault.entity.Note;
import com.example.studyvault.entity.User;
import com.example.studyvault.exception.FileTooLargeException;
import com.example.studyvault.exception.InvalidMarkdownFileException;
import com.example.studyvault.repository.NoteRepository;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Owns Markdown serialization and import rules; HTTP concerns stay in the controller. */
@Service
public class NoteFileService {
  public static final long MAX_IMPORT_BYTES = 5 * 1024 * 1024;
  private final NoteRepository notes;
  private final NoteService noteService;

  public NoteFileService(NoteRepository notes, NoteService noteService) {
    this.notes = notes;
    this.noteService = noteService;
  }

  @Transactional(readOnly = true)
  public String markdown(User user, Long id) {
    Note note = owned(user, id);
    return markdownFor(note);
  }

  @Transactional(readOnly = true)
  public String filename(User user, Long id) {
    Note note = owned(user, id);
    return safeFilename(note.getTitle(), note.getId());
  }

  @Transactional(readOnly = true)
  public void writeMarkdown(User user, Long id, OutputStream output) throws IOException {
    output.write(markdown(user, id).getBytes(StandardCharsets.UTF_8));
  }

  @Transactional(readOnly = true)
  public void writeZip(User user, OutputStream output) throws IOException {
    List<Note> ownedNotes = notes.findAllByUserOrderByUpdatedAtDesc(user);
    try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
      for (Note note : ownedNotes) {
        // The repository query is owner-scoped; keep this guard as a second line of defense
        // before writing user data into a downloadable archive.
        if (!belongsTo(note, user)) continue;
        zip.putNextEntry(new ZipEntry(safeFilename(note.getTitle(), note.getId())));
        zip.write(markdownFor(note).getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
      }
      zip.finish();
    }
  }

  @Transactional
  public NoteResponse importMarkdown(User user, MultipartFile file) {
    validateFile(file);
    final byte[] bytes;
    try {
      bytes = file.getBytes();
    } catch (IOException ex) {
      throw new InvalidMarkdownFileException("Unable to read the Markdown file");
    }
    if (bytes.length > MAX_IMPORT_BYTES)
      throw new FileTooLargeException("Markdown file must be at most 5 MB");
    String markdown = new String(bytes, StandardCharsets.UTF_8);
    ParsedMarkdown parsed = parse(markdown, file.getOriginalFilename());
    return noteService.create(user, new NoteCreateRequest(parsed.title(), parsed.content()));
  }

  static String safeFilename(String title, Long id) {
    String normalized = Normalizer.normalize(title == null ? "" : title, Normalizer.Form.NFKC);
    normalized =
        normalized
            .replaceAll("[\\\\/\\p{Cntrl}]+", " ")
            .replaceAll("[^A-Za-z0-9._ -]", "_")
            .replaceAll("\\s+", " ")
            .trim()
            .replaceAll("^[. ]+|[. ]+$", "");
    if (normalized.isBlank() || normalized.equals(".") || normalized.equals(".."))
      normalized = "note";
    if (normalized.length() > 80) normalized = normalized.substring(0, 80).trim();
    return normalized + "-" + id + ".md";
  }

  private Note owned(User user, Long id) {
    return notes
        .findByIdAndUser(id, user)
        .orElseThrow(() -> new com.example.studyvault.exception.NoteNotFoundException(id));
  }

  private boolean belongsTo(Note note, User user) {
    if (note == null || note.getUser() == null || user == null) return false;
    if (user.getId() != null && note.getUser().getId() != null)
      return Objects.equals(user.getId(), note.getUser().getId());
    return note.getUser() == user;
  }

  private String markdownFor(Note note) {
    return "# " + note.getTitle() + "\n\n" + note.getContent() + "\n";
  }

  private void validateFile(MultipartFile file) {
    if (file == null || file.isEmpty())
      throw new InvalidMarkdownFileException("A non-empty Markdown file is required");
    if (file.getSize() > MAX_IMPORT_BYTES)
      throw new FileTooLargeException("Markdown file must be at most 5 MB");
    String filename = file.getOriginalFilename();
    if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".md"))
      throw new InvalidMarkdownFileException("Only .md Markdown files are supported");
    String contentType = file.getContentType();
    if (contentType != null
        && !contentType.isBlank()
        && !contentType.equalsIgnoreCase("text/markdown")
        && !contentType.equalsIgnoreCase("text/plain")
        && !contentType.equalsIgnoreCase("application/octet-stream"))
      throw new InvalidMarkdownFileException("The uploaded file must have a Markdown content type");
  }

  private ParsedMarkdown parse(String source, String filename) {
    String[] lines = source.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
    String title = null;
    int contentStart = 0;
    if (lines.length > 0 && lines[0].matches("^#\\s+.+$")) {
      title = lines[0].substring(1).trim();
      contentStart = 1;
      while (contentStart < lines.length && lines[contentStart].isBlank()) contentStart++;
    }
    if (title == null || title.isBlank()) {
      String name = filename == null ? "Imported note" : filename.replaceFirst("(?i)\\.md$", "");
      name = name.replaceAll("[\\\\/\\p{Cntrl}]", " ").trim();
      title = name.isBlank() ? "Imported note" : name;
    }
    if (title.length() > 255)
      throw new InvalidMarkdownFileException("The Markdown title must be at most 255 characters");
    StringBuilder body = new StringBuilder();
    for (int i = contentStart; i < lines.length; i++) {
      if (body.length() > 0) body.append('\n');
      body.append(lines[i]);
    }
    String content = body.toString().trim();
    if (content.isBlank())
      throw new InvalidMarkdownFileException("The Markdown file must contain note content");
    if (content.length() > 1_000_000)
      throw new InvalidMarkdownFileException("Note content must be at most 1000000 characters");
    return new ParsedMarkdown(title, content);
  }

  private record ParsedMarkdown(String title, String content) {}
}
