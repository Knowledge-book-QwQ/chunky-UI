# ChunkUI
    
    A graphical interface for [Chunky](https://github.com/pop4959/Chunky), providing visual control over Minecraft chunk pregeneration.
    
    ## Supported Platforms
    
    - NeoForge 1.21.1 (primary)
    - Forge 1.20.1
    - Fabric 1.21.1
    
    ## Prerequisites
    
    [Chunky](https://modrinth.com/plugin/chunky) must be installed. This mod communicates with Chunky through its public API at runtime.
    
    ## Usage
    
    Open the interface via the ESC menu (a button is added to the pause screen) or by entering `/chunkui` in chat. Configure the target world, shape, center coordinates, and radius, then start pregeneration. Progress is displayed in real time.
    
    ## Architecture
    
    The project is organized as a multi-module Gradle build with platform-specific implementations:
    
    ```
    common/     Shared code: bridge interface, data models, fallback engine interfaces
    neoforge/   NeoForge 1.21.1 implementation: screen, Chunky bridge, Fluent Design UI
    forge/      Forge 1.20.1 compatibility stub
    fabric/     Fabric 1.21.1 compatibility stub
    ```
    
    The `ChunkyBridge` interface abstracts communication with Chunky. The NeoForge implementation uses reflection to locate and invoke `ChunkyAPI`, falling back to command execution if the API is unavailable. Progress events from Chunky are forwarded to the UI via registered listeners.
    
    ## Building
    
    Requires JDK 21 and Gradle 8.x.
    
    ```
    ./gradlew :neoforge:build
    ```
    
    ## License
    
    MIT. Chunky is GPL-3.0; this mod communicates with it via public API at runtime as an independent program.
    