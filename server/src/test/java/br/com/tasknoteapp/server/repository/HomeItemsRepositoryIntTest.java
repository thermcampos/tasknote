package br.com.tasknoteapp.server.repository;

import br.com.tasknoteapp.server.entity.Note;
import br.com.tasknoteapp.server.entity.Task;
import br.com.tasknoteapp.server.entity.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@RepositoryIntTest
@Sql(scripts = {"classpath:sql/HomeItemsRepositoryTest.sql"})
class HomeItemsRepositoryIntTest {

  @Autowired private TaskRepository taskRepository;

  @Autowired private NoteRepository noteRepository;

  @Autowired private TagRepository tagRepository;

  @Autowired private UserRepository userRepository;

  @Test
  @DisplayName("Windowed tasks should include recent and high-priority incomplete only")
  void findHomeTasks_windowed_shouldApplyWindowRules() {
    List<Task> tasks = taskRepository.findHomeTasks(getUserId(), null, null, true);

    List<String> descriptions = tasks.stream().map(Task::description).toList();

    Assertions.assertTrue(descriptions.contains("Recent task"));
    Assertions.assertTrue(descriptions.contains("Old HP incomplete task"));
    Assertions.assertTrue(descriptions.contains("Boundary recent completed task"));
    Assertions.assertFalse(descriptions.contains("Old task"));
    Assertions.assertFalse(descriptions.contains("Old HP completed task"));
    Assertions.assertFalse(descriptions.contains("Untagged old task"));
  }

  @Test
  @DisplayName("Windowed notes should include recent and just-archived notes only")
  void findHomeNotes_windowed_shouldApplyWindowRules() {
    List<Note> notes = noteRepository.findHomeNotes(getUserId(), null, null, true);

    List<String> titles = notes.stream().map(Note::title).toList();

    Assertions.assertTrue(titles.contains("Recent note"));
    Assertions.assertTrue(titles.contains("Just archived note"));
    Assertions.assertFalse(titles.contains("Old note"));
    Assertions.assertFalse(titles.contains("Old archived note"));
    Assertions.assertFalse(titles.contains("Untagged old note"));
  }

  @Test
  @DisplayName("Search should match description and include completed tasks")
  void findHomeTasks_searchTerm_shouldMatchDescriptionIncludingCompleted() {
    List<Task> tasks = taskRepository.findHomeTasks(getUserId(), "old hp", null, false);

    List<String> descriptions = tasks.stream().map(Task::description).toList();

    Assertions.assertEquals(2, descriptions.size());
    Assertions.assertTrue(descriptions.contains("Old HP incomplete task"));
    Assertions.assertTrue(descriptions.contains("Old HP completed task"));
  }

  @Test
  @DisplayName("Search should match task tags")
  void findHomeTasks_searchTerm_shouldMatchTags() {
    List<Task> tasks = taskRepository.findHomeTasks(getUserId(), "old-tag", null, false);

    Assertions.assertEquals(1, tasks.size());
    Assertions.assertEquals("Old task", tasks.getFirst().description());
  }

  @Test
  @DisplayName("Search should match task urls")
  void findHomeTasks_searchTerm_shouldMatchUrls() {
    List<Task> tasks =
        taskRepository.findHomeTasks(getUserId(), "searchable-task-url", null, false);

    Assertions.assertEquals(1, tasks.size());
    Assertions.assertEquals("Old task", tasks.getFirst().description());
  }

  @Test
  @DisplayName("Search should match note title, description, url and include archived")
  void findHomeNotes_searchTerm_shouldMatchFieldsIncludingArchived() {
    List<Note> byTitle = noteRepository.findHomeNotes(getUserId(), "old note", null, false);
    List<String> titles = byTitle.stream().map(Note::title).toList();
    Assertions.assertTrue(titles.contains("Old note"));
    Assertions.assertTrue(titles.contains("Old note with url"));
    Assertions.assertTrue(titles.contains("Untagged old note"));
    Assertions.assertTrue(titles.contains("Tagged old note"));

    List<Note> archived = noteRepository.findHomeNotes(getUserId(), "archived", null, false);
    List<String> archivedTitles = archived.stream().map(Note::title).toList();
    Assertions.assertTrue(archivedTitles.contains("Old archived note"));
    Assertions.assertTrue(archivedTitles.contains("Just archived note"));

    List<Note> byUrl =
        noteRepository.findHomeNotes(getUserId(), "searchable-note-url", null, false);
    Assertions.assertEquals(1, byUrl.size());
    Assertions.assertEquals("Old note with url", byUrl.getFirst().title());

    List<Note> byTag = noteRepository.findHomeNotes(getUserId(), "home-tag", null, false);
    Assertions.assertEquals(1, byTag.size());
    Assertions.assertEquals("Tagged old note", byTag.getFirst().title());
  }

  @Test
  @DisplayName("Tag filter should return only items with the given tag, unbounded")
  void findHomeItems_tagFilter_shouldMatchTag() {
    List<Task> tasks = taskRepository.findHomeTasks(getUserId(), null, "home-tag", false);
    Assertions.assertEquals(1, tasks.size());
    Assertions.assertEquals("Recent task", tasks.getFirst().description());

    List<Note> notes = noteRepository.findHomeNotes(getUserId(), null, "home-tag", false);
    Assertions.assertEquals(1, notes.size());
    Assertions.assertEquals("Tagged old note", notes.getFirst().title());
  }

  @Test
  @DisplayName("Untagged filter should return only items without tags")
  void findHomeItems_untaggedFilter_shouldMatchItemsWithoutTags() {
    List<Task> tasks = taskRepository.findHomeTasks(getUserId(), null, "untagged", false);
    List<String> descriptions = tasks.stream().map(Task::description).toList();
    Assertions.assertTrue(descriptions.contains("Old HP incomplete task"));
    Assertions.assertTrue(descriptions.contains("Old HP completed task"));
    Assertions.assertTrue(descriptions.contains("Boundary recent completed task"));
    Assertions.assertTrue(descriptions.contains("Untagged old task"));
    Assertions.assertFalse(descriptions.contains("Recent task"));
    Assertions.assertFalse(descriptions.contains("Old task"));

    List<Note> notes = noteRepository.findHomeNotes(getUserId(), null, "untagged", false);
    List<String> titles = notes.stream().map(Note::title).toList();
    Assertions.assertTrue(titles.contains("Untagged old note"));
    Assertions.assertTrue(titles.contains("Old note"));
    Assertions.assertFalse(titles.contains("Tagged old note"));
  }

  @Test
  @DisplayName("Composed search and tag params should apply AND semantics")
  void findHomeItems_composedParams_shouldApplyAnd() {
    List<Note> notes = noteRepository.findHomeNotes(getUserId(), "old", "home-tag", false);
    Assertions.assertEquals(1, notes.size());
    Assertions.assertEquals("Tagged old note", notes.getFirst().title());

    List<Task> tasks = taskRepository.findHomeTasks(getUserId(), "old", "home-tag", false);
    Assertions.assertTrue(tasks.isEmpty());
  }

  @Test
  @DisplayName("Tag names should come straight from the tags table")
  void findAllTagNamesByUserId_shouldReturnDistinctNames() {
    List<String> names = tagRepository.findAllTagNamesByUserId(getUserId());

    Assertions.assertTrue(names.contains("home-tag"));
    Assertions.assertTrue(names.contains("old-tag"));
  }

  @Test
  @DisplayName("Untagged detection should reflect items without tags")
  void userHasUntaggedItems_shouldDetectUntagged() {
    Assertions.assertTrue(tagRepository.userHasUntaggedItems(getUserId()));
    Assertions.assertFalse(tagRepository.userHasUntaggedItems(-1L));
  }

  private Long getUserId() {
    Optional<User> user = userRepository.findByEmail("home-items@domain.com");
    if (user.isPresent()) {
      return user.get().getId();
    }
    return 1L;
  }
}
