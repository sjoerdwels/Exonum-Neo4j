# Neo4j Transaction manager

The Neo4j transaction manager for Exonum allows Neo4j transactions to be verified and executed.
For the first, the queries in the provided will be executed in a Neo4j transaction and be rolled back afterwards. The latter
commits the transaction such that the changes are final. In both cases, a list of database changes will be provided.

Please note that the transaction manager does work with other Neo4j Transaction Event Handlers.


### Setup

1. Create a read only user
CALL dbms.security.createUser('readOnly', 'readonly', false)

2. Set user role to reader
CALL dbms.security.addRoleToUser('reader', 'readOnly')

3. List all users
CALL dbms.security.listUsers()
