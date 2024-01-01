# MailService: a simplified mailing system

> [!TIP]
> This project doesn't have a graphical user interface. Log into your mailbox through TCP tools like Netcat or PuTTY.

<br>

_Components:_

- Mailbox server:

  - stores and provides message access.

- Monitoring server:

  - receives usage stats from transfer servers via UDP.

- Transfer server:

  - forwards messages to appropriate mailbox servers.

- Decentralized Naming Service:
  
  - Manages mailbox server addresses and locations.
  - Operates with hierarchically structured nameservers.
  - Similar to DNS: top-level domain, subdomains (zones), nameservers.
  - Eliminates need for static configuration; transfer servers query nameservers.

- Opportunistic Encryption:

  - Secures DMAP channels for communication privacy and integrity.

- Message Integrity Verification:
  
  - Mechanisms for verifying message integrity.

<br>

<img width="1157" alt="image" src="https://user-images.githubusercontent.com/61852663/230803273-512afcc7-2cbc-4de1-921e-b549a144e027.png">

<img width="996" alt="SCR-20230613-lakx" src="https://github.com/sueszli/mailService/assets/61852663/5077b195-a931-4a41-9201-3df49562f14d">
