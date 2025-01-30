![image](https://github.com/user-attachments/assets/959c2093-3169-4e1e-a00e-eba04aca7fd7)

# RAFT-Based Leader Election and Secure Communication System

This project implements a secure and distributed system integrating RAFT-based leader election, messaging, and real-time communication using various technologies. It ensures modularity, security, and fault tolerance through microservices and message-driven architecture.

## Architecture Overview

The system consists of multiple components:

1. **Leader Election with RAFT Algorithm**
2. **Message Broker for Distributed Communication**
3. **Intermediary Manager Server**
4. **Web Server Cluster with Leader Election**
5. **Automated File Transfer via FTP**
6. **SMTP Client for Email Notifications**
7. **Dockerized Deployment**

### Leader Election Process
- Implements the RAFT consensus algorithm for leader election among nodes using UDP sockets.
- Runs asynchronously using multithreading or containerized execution.
- Handles heartbeat messages to maintain leadership status.
- Once elected, the leader communicates updates to the intermediary server.

### Message Broker Integration
- Uses RabbitMQ as a message broker for inter-service communication.
- The scraper publishes data to RabbitMQ queues.
- The intermediary server consumes messages and forwards relevant data to web servers.

### Intermediary Manager Server
- Acts as a middleware between the scraper and web servers.
- Listens for messages from RabbitMQ and processes them.
- Makes POST requests to the web servers for data updates.

### Web Server Cluster with Leader Election
- Multiple web servers operate on a shared database.
- Uses UDP-based RAFT consensus to elect a leader among them.
- The leader updates the intermediary server to manage traffic redirection.

### Automated File Transfer via FTP
- A separate thread in the intermediary server fetches files from an FTP server every 30 seconds.
- The retrieved file is sent as a multipart request to the web servers.
- The FTP server is dynamically populated with processed data.

### SMTP Client
- Implements an SMTP client for sending email notifications.
- Ensures successful communication with mail servers.

### Deployment
- All components are containerized.
- Uses Docker Compose to manage services including RabbitMQ and FTP.
- Ensures isolated and scalable execution.

