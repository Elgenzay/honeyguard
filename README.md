# HoneyGuard

HoneyGuard is a Minecraft plugin that extends the functionality of honeycombs, allowing them to be used to protect a configurable set of blocks from interaction **(but not destruction!)**, 
ideal for securing decorative elements like trapdoors or levers, or to prevent accidental modifications such as the stripping of logs.

While waxed blocks are protected from interaction, 
they can still be destroyed in the usual way and will drop a honeycomb upon destruction.

## Features

- **Interaction Protection**: Protect blocks from being interacted with (e.g., trapdoors, levers) while still allowing them to be destroyed normally.
- **Fire Protection**: 
  - Prevents fire from spreading to waxed blocks.
  - Allows fire to burn indefinitely on waxed blocks without destroying them.
  - Togglable in [`config.yml`](/src/main/resources/config.yml), enabled by default.
- **Chance Blocks**:
  - Some blocks have a probabilistic interaction with honeycombs to reduce costs:
    - A configurable percentage chance of consuming a honeycomb upon waxing.
    - A configurable percentage chance of dropping a honeycomb when destroyed.
  - Chance blocks are specified in [`config.yml`](/src/main/resources/config.yml)

## Commands

- `/honeyguard help` - Displays help information, including the current chance block percentage configurations and fire protection information, if enabled.
- `/honeyguard standardblocks` - Lists blocks that require one honeycomb to wax and guarantee one honeycomb drop upon destruction.
- `/honeyguard chanceblocks` - Lists blocks that operate on a chance basis.

## Usage

- **To wax a block**: Right-click it with a honeycomb.
- **To unwax a block**: Break it.
- **To display particles on waxed blocks**: Hold a honeycomb in your main hand.
- **To check if a block is standard or chance-based**: Left-click the block with a honeycomb.

## Installation

1. Download the [`HoneyGuard.jar`](https://github.com/Elgenzay/honeyguard/releases/latest) file.
2. Place it into your server's `plugins` directory.
3. Restart or reload the server.

## Configuration

After the first run, edit the [`config.yml`](/src/main/resources/config.yml) file in the `plugins/HoneyGuard` directory to adjust settings like fire protection and the chance percentages for honeycomb consumption and drop.  
Restart or reload the server for changes to take effect.
