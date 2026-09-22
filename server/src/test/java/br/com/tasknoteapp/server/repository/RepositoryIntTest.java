package br.com.tasknoteapp.server.repository;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;

/**
 * Composed annotation for repository integration tests running against H2 with JDBC named
 * parameter templates. Registers the JDBC test slice and imports the repository beans, which are
 * not picked up by component scanning in this slice.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@JdbcTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({
  NoteRepository.class,
  NoteUrlRepository.class,
  TagRepository.class,
  TaskRepository.class,
  TaskUrlRepository.class,
  UserPwdLimitRepository.class,
  UserRepository.class
})
@interface RepositoryIntTest {}
