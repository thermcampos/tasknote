-- Create test user
insert into users (email, password, admin, created_at, inactivated_at, last_password_change)
select 'test@domain.com', 'a1b2c3d4f5g6', false, current_timestamp, null, current_timestamp
where not exists (select 1 from users where email = 'test@domain.com');

-- Create tags
insert into tags (name, user_id)
select 'refactoring', (select id from users where email='test@domain.com')
where not exists (select 1 from tags where name = 'refactoring' and user_id = (select id from users where email='test@domain.com'));

insert into tags (name, user_id)
select 'cleaning', (select id from users where email='test@domain.com')
where not exists (select 1 from tags where name = 'cleaning' and user_id = (select id from users where email='test@domain.com'));

-- Create some tasks
insert into tasks (description, completed, user_id, last_update, due_date, high_priority)
select 'Refactor', false, (select id from users where email='test@domain.com'), current_timestamp, '2025-12-12', false
where not exists (select 1 from tasks where description = 'Refactor' and user_id = (select id from users where email='test@domain.com'));

insert into tasks (description, completed, user_id, last_update, due_date, high_priority)
select 'Cleanup', false, (select id from users where email='test@domain.com'), current_timestamp, '2025-12-12', false
where not exists (select 1 from tasks where description = 'Cleanup' and user_id = (select id from users where email='test@domain.com'));

-- Associate tags with tasks
insert into task_tags (task_id, tag_id)
select t.id, tg.id
from tasks t
join tags tg on tg.name = 'refactoring' and tg.user_id = t.user_id
where t.description = 'Refactor' and t.user_id = (select id from users where email='test@domain.com')
and not exists (select 1 from task_tags where task_id = t.id and tag_id = tg.id);

insert into task_tags (task_id, tag_id)
select t.id, tg.id
from tasks t
join tags tg on tg.name = 'cleaning' and tg.user_id = t.user_id
where t.description = 'Cleanup' and t.user_id = (select id from users where email='test@domain.com')
and not exists (select 1 from task_tags where task_id = t.id and tag_id = tg.id);
