# ShelvesServer

## A client/server project management system for small, loosely-organized projects

Created by Connor Dufault in 2020

This is the server component of the Shelves project. It runs an HTTP server that accepts requests from the [client application](https://github.com/cjdufault/ShelvesClient) and manages the database.

An admin password is used to protect access to functions that modify data in the database. The password is set when the server is started up if no password hash is found in a file called "auth.txt". To change the password, simply clear the data from or delete auth.txt and start the server. You will be prompted to enter a new password at that time.
