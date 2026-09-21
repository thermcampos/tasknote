-- Create test user
insert into tasknote.users (email, password, admin, created_at, inactivated_at, email_uuid, reset_token, last_password_change)
select 'testuuid@domain.com', 'a1b2c3d4f5g6', false, current_timestamp, null, 'cc2b5506-83ed-5764-985e-611ad4ce8050', 'abc123456', current_timestamp
where not exists (select 1 from tasknote.users where email = 'testuuid@domain.com');
