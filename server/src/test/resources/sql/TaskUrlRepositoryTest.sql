-- Create test user
insert into users (email, password, admin, created_at, inactivated_at, last_password_change)
select 'test@domain.com', 'a1b2c3d4f5g6', false, current_timestamp, null, current_timestamp
where not exists (select 1 from users where email = 'test@domain.com');

-- Create tags
insert into tags (name, user_id)
select 'linux', (select id from users where email='test@domain.com')
where not exists (select 1 from tags where name = 'linux' and user_id = (select id from users where email='test@domain.com'));

insert into tags (name, user_id)
select 'health', (select id from users where email='test@domain.com')
where not exists (select 1 from tags where name = 'health' and user_id = (select id from users where email='test@domain.com'));

-- Create a task
insert into tasks (description, completed, user_id, last_update, due_date, high_priority)
select 'Install Debian', false, (select id from users where email='test@domain.com'), current_timestamp, '2025-12-12', false
where not exists (select 1 from tasks where description = 'Install Debian' and user_id = (select id from users where email='test@domain.com'));

-- Associate tag with task
insert into task_tags (task_id, tag_id)
select t.id, tg.id
from tasks t
join tags tg on tg.name = 'linux' and tg.user_id = t.user_id
where t.description = 'Install Debian' and t.user_id = (select id from users where email='test@domain.com')
and not exists (select 1 from task_tags where task_id = t.id and tag_id = tg.id);

-- Create a task_url
insert into task_url (task_id, url)
select (select id from tasks where description = 'Install Debian'), 'debian.org'
where not exists (select 1 from task_url where url = 'debian.org');

-- Create another task
insert into tasks (description, completed, user_id, last_update, due_date, high_priority)
select 'Workout', false, (select id from users where email='test@domain.com'), current_timestamp, '2025-12-12', false
where not exists (select 1 from tasks where description = 'Workout' and user_id = (select id from users where email='test@domain.com'));

-- Associate tag with task
insert into task_tags (task_id, tag_id)
select t.id, tg.id
from tasks t
join tags tg on tg.name = 'health' and tg.user_id = t.user_id
where t.description = 'Workout' and t.user_id = (select id from users where email='test@domain.com')
and not exists (select 1 from task_tags where task_id = t.id and tag_id = tg.id);

-- Create a task_url
insert into task_url (task_id, url)
select (select id from tasks where description = 'Workout'), 'healthier.com'
where not exists (select 1 from task_url where url = 'healthier.com');
