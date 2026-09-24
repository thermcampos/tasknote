package br.com.tasknoteapp.server.service;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import br.com.tasknoteapp.server.entity.User;
import br.com.tasknoteapp.server.repository.TagRepository;
import br.com.tasknoteapp.server.response.HomeItemsResponse;
import br.com.tasknoteapp.server.response.NoteResponse;
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
import org.mockito.Mockito;
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

  private User mockUser() {
    User user = new User();
    user.setId(1L);
    user.setEmail("user@test.com");
    lenient().when(authUtil.getCurrentUserEmail()).thenReturn(Optional.of("user@test.com"));
    lenient().when(authService.findByEmail("user@test.com")).thenReturn(Optional.of(user));
    return user;
  }

  @Test
  @DisplayName("Get tasks tags should return all tags ordered alphabetically")
  void getTopTasksTag_shouldReturnAllTagsAlphabetically() {
    User user = mockUser();
    when(tagRepository.findAllTagNamesByUserId(user.getId()))
        .thenReturn(List.of("tag1", "tag2", "atag", "dev"));
    when(tagRepository.userHasUntaggedItems(user.getId())).thenReturn(false);

    List<String> homeTags = homeService.getTopTasksTag();

    Assertions.assertNotNull(homeTags);
    Assertions.assertEquals(List.of("atag", "dev", "tag1", "tag2"), homeTags);
  }

  @Test
  @DisplayName("Get top tasks tag with no tags should return empty list")
  void getTopTasksTag_noTags_shouldReturnEmptyList() {
    User user = mockUser();
    when(tagRepository.findAllTagNamesByUserId(user.getId())).thenReturn(List.of());
    when(tagRepository.userHasUntaggedItems(user.getId())).thenReturn(false);

    List<String> topTags = homeService.getTopTasksTag();

    Assertions.assertNotNull(topTags);
    Assertions.assertTrue(topTags.isEmpty());
  }

  @Test
  @DisplayName("Get top tasks tag with untagged tasks/notes should include 'untagged'")
  void getTopTasksTag_withUntagged_shouldIncludeUntagged() {
    User user = mockUser();
    when(tagRepository.findAllTagNamesByUserId(user.getId())).thenReturn(List.of());
    when(tagRepository.userHasUntaggedItems(user.getId())).thenReturn(true);

    List<String> topTags = homeService.getTopTasksTag();

    Assertions.assertNotNull(topTags);
    Assertions.assertEquals(List.of("untagged"), topTags);
  }

  @Test
  @DisplayName("Get home items with no filters should fetch the windowed payload")
  void getHomeItems_noFilters_shouldFetchWindowedPayload() {
    mockUser();
    TaskResponse task =
        new TaskResponse(1L, false, "Task 1", false, null, null, null, List.of(), List.of());
    NoteResponse note =
        new NoteResponse(1L, "Note 1", "content", null, null, List.of(), false, null, false);
    when(taskService.getHomeTasks(null, null, true)).thenReturn(List.of(task));
    when(noteService.getHomeNotes(null, null, true)).thenReturn(List.of(note));

    HomeItemsResponse response = homeService.getHomeItems(null, null, null);

    Assertions.assertEquals(1, response.tasks().size());
    Assertions.assertEquals(1, response.notes().size());
  }

  @Test
  @DisplayName("Get home items with search should run unbounded search")
  void getHomeItems_withSearch_shouldRunUnboundedSearch() {
    mockUser();
    when(taskService.getHomeTasks("foo", null, false)).thenReturn(List.of());
    when(noteService.getHomeNotes("foo", null, false)).thenReturn(List.of());

    HomeItemsResponse response = homeService.getHomeItems("foo", null, "all");

    Assertions.assertTrue(response.tasks().isEmpty());
    Assertions.assertTrue(response.notes().isEmpty());
    Mockito.verify(taskService).getHomeTasks("foo", null, false);
    Mockito.verify(noteService).getHomeNotes("foo", null, false);
  }

  @Test
  @DisplayName("Get home items with type tasks should skip notes")
  void getHomeItems_typeTasks_shouldSkipNotes() {
    mockUser();
    when(taskService.getHomeTasks(null, null, true)).thenReturn(List.of());

    HomeItemsResponse response = homeService.getHomeItems(null, null, "tasks");

    Assertions.assertTrue(response.notes().isEmpty());
    Mockito.verifyNoInteractions(noteService);
  }

  @Test
  @DisplayName("Get home items with type notes should skip tasks")
  void getHomeItems_typeNotes_shouldSkipTasks() {
    mockUser();
    when(noteService.getHomeNotes(null, null, true)).thenReturn(List.of());

    HomeItemsResponse response = homeService.getHomeItems(null, null, "notes");

    Assertions.assertTrue(response.tasks().isEmpty());
    Mockito.verifyNoInteractions(taskService);
  }

  @Test
  @DisplayName("Get home items with tag and search should compose params unbounded")
  void getHomeItems_tagAndSearch_shouldComposeUnbounded() {
    mockUser();
    when(taskService.getHomeTasks("foo", "work", false)).thenReturn(List.of());
    when(noteService.getHomeNotes("foo", "work", false)).thenReturn(List.of());

    HomeItemsResponse response = homeService.getHomeItems("foo", "work", null);

    Assertions.assertTrue(response.tasks().isEmpty());
    Assertions.assertTrue(response.notes().isEmpty());
    Mockito.verify(taskService).getHomeTasks("foo", "work", false);
    Mockito.verify(noteService).getHomeNotes("foo", "work", false);
  }
}
