# Integration Test Setup

This contains an integration testing setup for a 4 node system running on Docker.

## Requirements
- Docker Toolbox
- newman

## Test execution on Windows

Currently configured for Docker Toolbox and requires a Docker Machine with ip 192.168.99.101
not 192.168.99.100 which is default for Docker Toolbox.

Script includes lengthy timeouts due to long Docker setup startup times on development machine.

1. Run following command to execute tests:

 ``` cmd
    .\run_integration_tests.bat
 ```
 
