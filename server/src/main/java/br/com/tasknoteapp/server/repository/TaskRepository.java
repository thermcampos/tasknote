package br.com.tasknoteapp.server.repository;

import br.com.tasknoteapp.server.entity.TaskEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** This interface represents a task repository, for database access. */
public interface TaskRepository extends JpaRepository<TaskEntity, Long> {

  List<TaskEntity> findAllByUser_id(Long userId);

  Optional<TaskEntity> findByIdAndUser_id(Long id, Long userId);

  @Query(
      """
      select distinct t
      from TaskEntity t
      left join TaskUrlEntity tu on tu.id.taskId = t.id
      left join t.tags tg
      where (
        upper(t.description) like upper(concat('%', :searchTerm, '%')) or
        upper(tg.name) like upper(concat('%', :searchTerm, '%')) or
        upper(tu.id.url) like upper(concat('%', :searchTerm, '%'))
        ) and t.user.id = :userId and t.completed = false
      """)
  List<TaskEntity> findAllBySearchTerm(
      @Param("searchTerm") String searchTerm, @Param("userId") Long userId);
}
