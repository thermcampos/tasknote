package br.com.tasknoteapp.server.service;

import static org.mockito.Mockito.when;

import br.com.tasknoteapp.server.entity.TagEntity;
import br.com.tasknoteapp.server.entity.UserEntity;
import br.com.tasknoteapp.server.repository.TagRepository;
import br.com.tasknoteapp.server.response.TaskResponse;
import br.com.tasknoteapp.server.util.AuthUtil;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

  @Mock private TaskService taskService;

  @Mock private NoteService noteService;

  @Mock private TagRepository tagRepository;

  @Mock private AuthUtil authUtil;

  @Mock private AuthService authService;

  private HomeService homeService;

  @BeforeEach
  void setUp() {
    homeService = new HomeService(taskService, noteService, tagRepository, authService, authUtil);
  }

  private UserEntity mockUser() {
    UserEntity user = new UserEntity();
    user.setId(1L);
    user.setEmail("user@test.com");
    when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of("user@test.com"));
    when(authService.findByEmail("user@test.com")).thenReturn(Optional.of(user));
    return user;
  }

  @Test
  @DisplayName("Get tasks tags should return all tags ordered alphabetically")
  void getTopTasksTag_shouldReturnAllTagsAlphabetically() {
    UserEntity user = mockUser();
    TagEntity tag1 = new TagEntity("tag1", user);
    TagEntity tag2 = new TagEntity("tag2", user);
    TagEntity tag3 = new TagEntity("tag3", user);
    TagEntity tag4 = new TagEntity("tag4", user);
    TagEntity tag5 = new TagEntity("tag5", user);
    TagEntity tag6 = new TagEntity("tag6", user);

    when(tagRepository.findAllByUser_idOrderByNameAsc(user.getId()))
        .thenReturn(List.of(tag1, tag2, tag3, tag4, tag5, tag6));

    TaskResponse task1 =
        new TaskResponse(1L, false, "Task 1", false, null, null, null, List.of("tag1"), List.of());
    when(taskService.getTasksByFilter("all")).thenReturn(List.of(task1));
    when(noteService.getAllNotes()).thenReturn(List.of());

    List<String> tags = homeService.getTopTasksTag();

    Assertions.assertNotNull(tags);
    Assertions.assertEquals(6, tags.size());
    Assertions.assertEquals(List.of("tag1", "tag2", "tag3", "tag4", "tag5", "tag6"), tags);
  }

  @Test
  @DisplayName("Get top tasks tag with no tags should return empty list")
  void getTopTasksTag_noTags_shouldReturnEmptyList() {
    UserEntity user = mockUser();
    when(tagRepository.findAllByUser_idOrderByNameAsc(user.getId())).thenReturn(List.of());
    when(taskService.getTasksByFilter("all")).thenReturn(List.of());
    when(noteService.getAllNotes()).thenReturn(List.of());

    List<String> topTags = homeService.getTopTasksTag();

    Assertions.assertNotNull(topTags);
    Assertions.assertTrue(topTags.isEmpty());
  }

  @Test
  @DisplayName("Get top tasks tag with untagged tasks/notes should include 'untagged'")
  void getTopTasksTag_withUntagged_shouldIncludeUntagged() {
    UserEntity user = mockUser();
    TagEntity tag1 = new TagEntity("tag1", user);
    when(tagRepository.findAllByUser_idOrderByNameAsc(user.getId())).thenReturn(List.of(tag1));

    TaskResponse task1 =
        new TaskResponse(1L, false, "Task 1", false, null, null, null, List.of(), List.of());
    when(taskService.getTasksByFilter("all")).thenReturn(List.of(task1));
    when(noteService.getAllNotes()).thenReturn(List.of());

    List<String> topTags = homeService.getTopTasksTag();

    Assertions.assertNotNull(topTags);
    Assertions.assertEquals(2, topTags.size());
    Assertions.assertTrue(topTags.contains("untagged"));
    Assertions.assertTrue(topTags.contains("tag1"));
    Assertions.assertEquals(List.of("tag1", "untagged"), topTags);
  }
}

