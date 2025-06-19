# AIChatRestApiTests
=======

# Welcome to AIChat Tests

Here you will find integration tests for AIChat Backend

Steps to build the project:
1) export CODEARTIFACT_AUTH_TOKEN=$(aws codeartifact get-authorization-token \
   --domain personal \
   --domain-owner 472156860409 \
   --query authorizationToken \
   --output text)
2) ./gradlew build