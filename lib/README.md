# 📂 Carpeta `lib`

## Español

La carpeta `lib` está destinada a contener las **dependencias necesarias** para el desarrollo de plugins/mods de Hytale:

- `HytaleServer.jar` → el servidor de Hytale (dependencia principal).
- `Assets.zip` → los recursos del juego (texturas, modelos, sonidos, etc.).

### Importante

- Los archivos dentro de `lib` están **ignorados en Git** (`.gitignore`), por lo que no se sincronizan en el
  repositorio.
- Cada desarrollador debe colocar manualmente los archivos correspondientes a la versión de Hytale que esté utilizando.

### Micro tutorial: ¿Dónde encontrar estos archivos?

Los archivos se extraen de la instalación del juego Hytale en tu sistema:

- **Windows**:  
  Normalmente en `C:\Program Files\Hytale\` o en la carpeta donde instalaste el juego.  
  Busca `HytaleServer.jar` y `Assets.zip` dentro de los directorios del juego.

- **macOS**:  
  Entra en la carpeta de la aplicación (`/Applications/Hytale.app`).  
  Haz clic derecho → *Mostrar contenido del paquete* → navega a `Contents/Resources/`.  
  Allí encontrarás los archivos necesarios.

- **Linux**:  
  Usualmente en `~/.local/share/Hytale/` o `/opt/hytale/`.  
  Los archivos del servidor y assets estarán en las carpetas de instalación del juego.

---

## English

The `lib` folder is meant to contain the **required dependencies** for Hytale plugin/mod development:

- `HytaleServer.jar` → the Hytale server (main dependency).
- `Assets.zip` → the game resources (textures, models, sounds, etc.).

### Important

- Files inside `lib` are **ignored by Git** (`.gitignore`), so they are not synchronized in the repository.
- Each developer must manually place the files corresponding to the Hytale version they are working with.

### Micro tutorial: Where to find these files?

You can extract them from the Hytale game installation on your system:

- **Windows**:  
  Usually located in `C:\Program Files\Hytale\` or wherever you installed the game.  
  Look for `HytaleServer.jar` and `Assets.zip` inside the game directories.

- **macOS**:  
  Go to the application folder (`/Applications/Hytale.app`).  
  Right-click → *Show Package Contents* → navigate to `Contents/Resources/`.  
  The required files are stored there.

- **Linux**:  
  Typically found in `~/.local/share/Hytale/` or `/opt/hytale/`.  
  The server and asset files are located within the game installation directories.

---