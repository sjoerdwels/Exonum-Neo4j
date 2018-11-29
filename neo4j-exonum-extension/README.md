# DB Setup

1. Create a read only user
CALL dbms.security.createUser('readOnly', 'readonly', false)

2. Set user role to reader
CALL dbms.security.addRoleToUser('reader', 'readOnly')

3. List all users
CALL dbms.security.listUsers()
