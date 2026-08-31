# GG-vs-Goons-pluggin

Bukkit/Paper Plugin for GG vs Goons server

Built around a module system - each mechanic is a self-contaned class, so adding new commands doesn't mean touching existing ones

## Current Modules

``/warprisoner <player>`` sends a clickable capture offer in chat.

- Accept → flagged as a prisoner, switched to adventure mode.
- Decline → nothing happnes; it's on the captors to kill or free them. 

Commands:

- `/warprisoner <player>` - Sends a capture offer
- `/freeprisoner <player>` - Releases your prisoner, restores normal survival gamemode.

## Adding a new module



## Build

Requires JDK 21.

## Requirements

- Paper 1.21.x server
- Java 21

