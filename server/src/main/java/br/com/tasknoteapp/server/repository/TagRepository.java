package br.com.tasknoteapp.server.repository;

import br.com.tasknoteapp.server.entity.TagEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** This interface represents a tag repository, for database access. */
public interface TagRepository extends JpaRepository<TagEntity, Long> {

  Optional<TagEntity> findByNameAndUser_id(String name, Long userId);

  List<TagEntity> findAllByUser_idOrderByNameAsc(Long userId);

  @Modifying
  @Query(
      """
      delete from TagEntity t
      where t.user.id = :userId
      and not exists (select 1 from TaskEntity tk where t member of tk.tags)
      and not exists (select 1 from NoteEntity n where t member of n.tags)
      """)
  void deleteOrphanedTags(@Param("userId") Long userId);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from TagEntity t where t.user.id = :userId")
  void deleteAllForUser(@Param("userId") Long userId);
}
