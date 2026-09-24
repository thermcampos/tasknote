-- Create test user
insert into tasknote.users (email, password, admin, created_at, inactivated_at, last_password_change)
select 'home-items@domain.com', 'a1b2c3d4f5g6', false, current_timestamp, null, current_timestamp
where not exists (select 1 from tasknote.users where email = 'home-items@domain.com');

-- Create tags
insert into tasknote.tags (name, user_id)
select 'home-tag', (select id from tasknote.users where email='home-items@domain.com')
where not exists (select 1 from tasknote.tags where name = 'home-tag'
  and user_id = (select id from tasknote.users where email='home-items@domain.com'));

insert into tasknote.tags (name, user_id)
select 'old-tag', (select id from tasknote.users where email='home-items@domain.com')
where not exists (select 1 from tasknote.tags where name = 'old-tag'
  and user_id = (select id from tasknote.users where email='home-items@domain.com'));

-- Tasks
insert into tasknote.tasks (description, completed, user_id, last_update, high_priority)
select 'Recent task', false, (select id from tasknote.users where email='home-items@domain.com'),
  current_timestamp, false
where not exists (select 1 from tasknote.tasks where description = 'Recent task'
  and user_id = (select id from tasknote.users where email='home-items@domain.com'));

insert into tasknote.tasks (description, completed, user_id, last_update, high_priority)
select 'Old task', false, (select id from tasknote.users where email='home-items@domain.com'),
  current_timestamp - interval '3 days', false
where not exists (select 1 from tasknote.tasks where description = 'Old task'
  and user_id = (select id from tasknote.users where email='home-items@domain.com'));

insert into tasknote.tasks (description, completed, user_id, last_update, high_priority)
select 'Old HP incomplete task', false,
  (select id from tasknote.users where email='home-items@domain.com'),
  current_timestamp - interval '3 days', true
where not exists (select 1 from tasknote.tasks where description = 'Old HP incomplete task'
  and user_id = (select id from tasknote.users where email='home-items@domain.com'));

insert into tasknote.tasks (description, completed, user_id, last_update, high_priority)
select 'Old HP completed task', true,
  (select id from tasknote.users where email='home-items@domain.com'),
  current_timestamp - interval '3 days', true
where not exists (select 1 from tasknote.tasks where description = 'Old HP completed task'
  and user_id = (select id from tasknote.users where email='home-items@domain.com'));

insert into tasknote.tasks (description, completed, user_id, last_update, high_priority)
select 'Boundary recent completed task', true,
  (select id from tasknote.users where email='home-items@domain.com'),
  current_timestamp - interval '23 hours', false
where not exists (select 1 from tasknote.tasks where description = 'Boundary recent completed task'
  and user_id = (select id from tasknote.users where email='home-items@domain.com'));

insert into tasknote.tasks (description, completed, user_id, last_update, high_priority)
select 'Untagged old task', false,
  (select id from tasknote.users where email='home-items@domain.com'),
  current_timestamp - interval '3 days', false
where not exists (select 1 from tasknote.tasks where description = 'Untagged old task'
  and user_id = (select id from tasknote.users where email='home-items@domain.com'));

-- Task tags
insert into tasknote.task_tags (task_id, tag_id)
select t.id, tg.id
from tasknote.tasks t
join tasknote.tags tg on tg.name = 'home-tag' and tg.user_id = t.user_id
where t.description = 'Recent task'
  and t.user_id = (select id from tasknote.users where email='home-items@domain.com')
  and not exists (select 1 from tasknote.task_tags where task_id = t.id and tag_id = tg.id);

insert into tasknote.task_tags (task_id, tag_id)
select t.id, tg.id
from tasknote.tasks t
join tasknote.tags tg on tg.name = 'old-tag' and tg.user_id = t.user_id
where t.description = 'Old task'
  and t.user_id = (select id from tasknote.users where email='home-items@domain.com')
  and not exists (select 1 from tasknote.task_tags where task_id = t.id and tag_id = tg.id);

-- Task urls
insert into tasknote.task_url (task_id, url)
select t.id, 'http://searchable-task-url.example.com'
from tasknote.tasks t
where t.description = 'Old task'
  and t.user_id = (select id from tasknote.users where email='home-items@domain.com')
  and not exists (select 1 from tasknote.task_url where task_id = t.id
    and url = 'http://searchable-task-url.example.com');

-- Notes
insert into tasknote.notes (title, description, user_id, last_update, shared, archived)
select 'Recent note', 'recent content',
  (select id from tasknote.users where email='home-items@domain.com'),
  current_timestamp, false, false
where not exists (select 1 from tasknote.notes where title = 'Recent note'
  and user_id = (select id from tasknote.users where email='home-items@domain.com'));

insert into tasknote.notes (title, description, user_id, last_update, shared, archived)
select 'Old note', 'old content',
  (select id from tasknote.users where email='home-items@domain.com'),
  current_timestamp - interval '3 days', false, false
where not exists (select 1 from tasknote.notes where title = 'Old note'
  and user_id = (select id from tasknote.users where email='home-items@domain.com'));

insert into tasknote.notes (title, description, user_id, last_update, shared, archived)
select 'Old archived note', 'old archived content',
  (select id from tasknote.users where email='home-items@domain.com'),
  current_timestamp - interval '3 days', false, true
where not exists (select 1 from tasknote.notes where title = 'Old archived note'
  and user_id = (select id from tasknote.users where email='home-items@domain.com'));

insert into tasknote.notes (title, description, user_id, last_update, shared, archived)
select 'Just archived note', 'just archived content',
  (select id from tasknote.users where email='home-items@domain.com'),
  current_timestamp, false, true
where not exists (select 1 from tasknote.notes where title = 'Just archived note'
  and user_id = (select id from tasknote.users where email='home-items@domain.com'));

insert into tasknote.notes (title, description, user_id, last_update, shared, archived)
select 'Old note with url', 'note with url content',
  (select id from tasknote.users where email='home-items@domain.com'),
  current_timestamp - interval '3 days', false, false
where not exists (select 1 from tasknote.notes where title = 'Old note with url'
  and user_id = (select id from tasknote.users where email='home-items@domain.com'));

insert into tasknote.notes (title, description, user_id, last_update, shared, archived)
select 'Untagged old note', 'untagged note content',
  (select id from tasknote.users where email='home-items@domain.com'),
  current_timestamp - interval '3 days', false, false
where not exists (select 1 from tasknote.notes where title = 'Untagged old note'
  and user_id = (select id from tasknote.users where email='home-items@domain.com'));

insert into tasknote.notes (title, description, user_id, last_update, shared, archived)
select 'Tagged old note', 'tagged note content',
  (select id from tasknote.users where email='home-items@domain.com'),
  current_timestamp - interval '3 days', false, false
where not exists (select 1 from tasknote.notes where title = 'Tagged old note'
  and user_id = (select id from tasknote.users where email='home-items@domain.com'));

-- Note tags
insert into tasknote.note_tags (note_id, tag_id)
select n.id, tg.id
from tasknote.notes n
join tasknote.tags tg on tg.name = 'home-tag' and tg.user_id = n.user_id
where n.title = 'Tagged old note'
  and n.user_id = (select id from tasknote.users where email='home-items@domain.com')
  and not exists (select 1 from tasknote.note_tags where note_id = n.id and tag_id = tg.id);

-- Note urls
insert into tasknote.note_urls (note_id, url)
select n.id, 'http://searchable-note-url.example.com'
from tasknote.notes n
where n.title = 'Old note with url'
  and n.user_id = (select id from tasknote.users where email='home-items@domain.com')
  and not exists (select 1 from tasknote.note_urls where note_id = n.id
    and url = 'http://searchable-note-url.example.com');
