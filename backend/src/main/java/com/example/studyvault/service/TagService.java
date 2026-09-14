package com.example.studyvault.service;

import com.example.studyvault.dto.TagCreateRequest;
import com.example.studyvault.dto.TagResponse;
import com.example.studyvault.dto.TagUpdateRequest;
import com.example.studyvault.entity.Tag;
import com.example.studyvault.entity.User;
import com.example.studyvault.exception.TagAlreadyExistsException;
import com.example.studyvault.exception.TagNotFoundException;
import com.example.studyvault.repository.TagRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TagService {
  private final TagRepository tags;

  public TagService(TagRepository tags) {
    this.tags = tags;
  }

  @Transactional(readOnly = true)
  public List<TagResponse> list(User user) {
    return tags.findAllByUserOrderByNameAsc(user).stream().map(TagResponse::from).toList();
  }

  @Transactional
  public TagResponse create(User user, TagCreateRequest request) {
    if (tags.findByUserAndName(user, request.name()).isPresent())
      throw new TagAlreadyExistsException();
    Tag tag = new Tag();
    tag.setUser(user);
    tag.setName(request.name());
    tag.setColor(request.color());
    return TagResponse.from(tags.save(tag));
  }

  @Transactional
  public TagResponse update(User user, Long id, TagUpdateRequest request) {
    Tag tag = findOwned(user, id);
    if (!tag.getName().equals(request.name())
        && tags.findByUserAndName(user, request.name()).isPresent())
      throw new TagAlreadyExistsException();
    tag.setName(request.name());
    tag.setColor(request.color());
    return TagResponse.from(tags.save(tag));
  }

  @Transactional
  public void delete(User user, Long id) {
    tags.delete(findOwned(user, id));
  }

  Tag findOwned(User user, Long id) {
    return tags.findByIdAndUser(id, user).orElseThrow(() -> new TagNotFoundException(id));
  }
}
