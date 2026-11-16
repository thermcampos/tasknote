-- Create test user
insert into tasknote.users (email, password, admin, created_at, inactivated_at, last_password_change)
select 'test@domain.com', 'a1b2c3d4f5g6', false, current_timestamp, null, current_timestamp
where not exists (select 1 from tasknote.users where email = 'test@domain.com');

-- Create a note with two urls
insert into tasknote.notes (user_id, description, title, last_update, shared, archived)
select (select id from tasknote.users where email = 'test@domain.com'),
       'Note with urls', 'Note', current_timestamp, false, false
where not exists (
  select 1 from tasknote.notes
  where description = 'Note with urls'
    and user_id = (select id from tasknote.users where email = 'test@domain.com')
);

insert into tasknote.note_urls (note_id, url)
select (select id from tasknote.notes
        where description = 'Note with urls'
          and user_id = (select id from tasknote.users where email = 'test@domain.com')),
       'debian.org'
where not exists (select 1 from tasknote.note_urls where url = 'debian.org');

insert into tasknote.note_urls (note_id, url)
select (select id from tasknote.notes
        where description = 'Note with urls'
          and user_id = (select id from tasknote.users where email = 'test@domain.com')),
       'postgresql.org'
where not exists (select 1 from tasknote.note_urls where url = 'postgresql.org');

-- Create a second note without urls
insert into tasknote.notes (user_id, description, title, last_update, shared, archived)
select (select id from tasknote.users where email = 'test@domain.com'),
       'Note without urls', 'Empty', current_timestamp, false, false
where not exists (
  select 1 from tasknote.notes
  where description = 'Note without urls'
    and user_id = (select id from tasknote.users where email = 'test@domain.com')
);
