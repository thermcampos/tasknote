package br.com.tasknoteapp.server.repository;

import br.com.tasknoteapp.server.entity.Task;
import br.com.tasknoteapp.server.entity.TaskUrl;
import br.com.tasknoteapp.server.entity.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Sql(scripts = {"classpath:sql/TaskUrlRepositoryTest.sql"})
class TaskUrlRepositoryIntTest {

  @Autowired TaskUrlRepository taskUrlRepository;

  @Autowired TaskRepository taskRepository;

  @Autowired UserRepository userRepository;

  private static final String USER_EMAIL = "test@domain.com";

  @Test
  @DisplayName("Delete all task url by task id")
  void deleteAllById_taskIdIntTest() {
    Optional<User> user = userRepository.findByEmail(USER_EMAIL);
    Assertions.assertFalse(user.isEmpty());

    Long userId = user.get().getId();
    List<Task> tasks = taskRepository.findAllByUserId(userId);
    Assertions.assertFalse(tasks.isEmpty());

    Optional<Task> debianTask =
        tasks.stream().filter(t -> t.getDescription().equals("Install Debian")).findFirst();
    Assertions.assertFalse(debianTask.isEmpty());

    Long taskId = debianTask.get().getId();

    taskUrlRepository.deleteAllById_taskId(taskId);

    List<TaskUrl> taskUrls = taskUrlRepository.findAllById_taskId(taskId);
    Assertions.assertTrue(taskUrls.isEmpty());
  }
}
