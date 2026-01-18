# Hytale Plugin Template

A simple ready-to-use Gradle template for developing Hytale server plugins. This project provides a pre-configured environment with tasks for running a local server, debugging, and managing assets.

## 🚀 Getting Started

### 1. Clone the Repository
```bash
git clone <repository-url>
```
Open the project in **IntelliJ IDEA** (recommended for Hytale development).

### 2. Add Hytale Dependencies
Due to licensing, Hytale server files are not included. You must provide them manually:
1.  Navigate to the `lib/` directory.
2.  Place your **`HytaleServer.jar`** file here.
3.  Place your **`Assets.zip`** file here.

### 3. Generate Sources (Optional but Recommended)
To view the Hytale Server source code for debugging and reference:
1.  Run the Gradle task `genSources`.
2.  This will decompile `HytaleServer.jar` and create `HytaleServer-sources.jar` in the `lib/` folder.
3.  IntelliJ should automatically detect the sources via the Ivy repository configuration.

## ⚙️ Configuration

Before starting your own plugin, update the project metadata:

1.  **`gradle.properties`**: Edit this file to set your plugin's details:
    *   `maven_group`: Your organization's package (e.g., `com.myname`).
    *   `plugin_name`: The name of your plugin. This also sets the project name in `settings.gradle`.
    *   `plugin_version`: Your plugin's version.
    *   `plugin_main`: The fully qualified name of your main class.
2.  **Refactor Package**: Rename the `com.example.hytaleplugin` package in `src/main/java` to match your `maven_group`.

## 🛠️ Gradle Tasks

This template includes custom tasks to streamline development:

| Task | Description |
| :--- | :--- |
| **`runServer`** | Launches a local Hytale server instance with your plugin loaded. It automatically syncs assets before starting and saves changes after stopping. |
| **`genSources`** | Decompiles `HytaleServer.jar` using Vineflower to generate readable Java sources (`HytaleServer-sources.jar`) in the `lib/` folder. |
| **`syncSrcAssets`** | Copies assets from `src/main/resources` to the server's runtime folder. Runs automatically before `runServer`. |
| **`syncBuildAssets`** | Copies assets from the server's runtime folder back to `src/main/resources`. Runs automatically after `runServer` stops to save changes made via the in-game Asset Editor. |

## 📝 Notes
*   **Java Version**: This project is configured for **Java 25** (Preview). Ensure your IDE and environment are set up accordingly.
*   **Asset Editor**: Changes made in the Hytale Asset Editor are saved to the build directory. The `syncBuildAssets` task ensures these changes are persisted back to your source code.