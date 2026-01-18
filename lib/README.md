# Library Directory

This folder is designated for local Hytale server dependencies.

## Required Files

You must manually place the following files in this directory:

*   **`HytaleServer.jar`**: The main Hytale Server executable. This is used as a compile-time dependency and to run the local test server.
*   **`Assets.zip`**: The archive containing game assets, required by the server at runtime.

## Generated Files

*   **`HytaleServer-sources.jar`**: This file is generated automatically by the `genSources` Gradle task. It contains the decompiled source code of the server, allowing for easier debugging and development within the IDE.

**Note:** Ensure `HytaleServer.jar` matches the version intended for development.