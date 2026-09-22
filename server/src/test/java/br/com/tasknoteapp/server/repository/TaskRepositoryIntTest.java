package br.com.tasknoteapp.server.repository;

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
@Sql(scripts = {"classpath:sql/TaskRepositoryTest.sql"})
class TaskRepositoryIntTest {

  @Autowired private TaskRepository taskRepository;

  @Autowired private UserRepository userRepository;

  @Test
  @DisplayName("Find all tasks by user id")
  void findAllByUser_idIntTest() {
    List<Task> entities = taskRepository.findAllByUserId(getUserId());

    Assertions.assertFalse(entities.isEmpty());
    Assertions.assertEquals(2, entities.size());
    Assertions.assertEquals("Refactor", entities.get(0).description());
    Assertions.assertEquals("Cleanup", entities.get(1).description());
  }

  @Test
  @DisplayName("Find all tasks by search term")
  void findAllBySearchTerm_intTest() {
    List<Task> entities = taskRepository.findAllBySearchTerm("refactor", getUserId());

    Assertions.assertFalse(entities.isEmpty());
    Assertions.assertEquals(1, entities.size());
    Assertions.assertEquals("Refactor", entities.get(0).description());
  }

  private Long getUserId() {
    Optional<User> user = userRepository.findByEmail("test@domain.com");
    if (user.isPresent()) {
      return user.get().getId();
    }
    return 1L;
  }
}
