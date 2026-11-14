-- Align note_urls with task_url: composite primary key, no surrogate id.
-- Duplicated rows are removed first so the new primary key can be created.

delete from tasknote.note_urls a
 using tasknote.note_urls b
 where a.note_id = b.note_id
   and a.url = b.url
   and a.id > b.id;

alter table tasknote.note_urls
  drop constraint note_url_pk;

alter table tasknote.note_urls
  drop column id;

alter table tasknote.note_urls
  add constraint note_url_pk primary key (note_id, url);
