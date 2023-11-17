# Distributed File Sharing System with UDP datagram transfer between nodes
## Overview

This project implements a distributed file sharing system using Java socket programming. It consists of a server and multiple clients that can share files with each other.

## Features

- File Fragmentation: Files are split into blocks to facilitate efficient sharing.
- UDP Communication: UDP is used for efficient block information exchange among clients.
- Mediator Thread: A mediator thread coordinates the exchange of block information between clients.
- Menu System: Clients can interact with the system through a simple console menu.

## Components
### Server

The server listens for incoming connections from clients, manages client files and block information, and facilitates communication between clients.

Usage:

<pre>
```bash
public class Example {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}
```
</pre>


### Client

Clients connect to the server, share their files, and interact with the distributed file system through a console menu. Also they have a mediator thread that manages the UDP communication between themselves, ensuring efficient exchange of block information.


## How to Use

    Start the Server:
        Run the Server class to start the server.

    Connect Clients:
        Run the Client class on multiple machines to connect clients to the server.

    Share Files:
        Clients can share files by placing them in the ClientFiles directory.

    Interact with the Menu:
        Clients can interact with the system through the console menu:
            Option 1: Ask for a file location.
            Option 2: Download a file.
            Option 3: Exit.

Dependencies

    Java SDK 8 or later.

Notes

    The project is currently configured to use the default server IP address (10.0.0.10) and port (9090). Update these values in the Client class if needed.

Contributors

    [Luís Ferreira]
