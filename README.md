MailService is a comprehensive project that aims to implement an enhanced electronic mail service with a range of advanced functionalities. The system comprises core components, including a message transfer protocol (DMTP), a message access protocol (DMAP), transfer servers, mailbox servers, and a monitoring server. 

Users interact with the servers using TCP tools like Netcat or PuTTY, as depicted in the architecture overview. The transfer server plays a crucial role in forwarding messages to the appropriate mailbox servers, while the mailbox server stores and provides access to the messages for the users. Additionally, the monitoring server receives usage statistics from transfer servers via UDP.

To elevate the traditional mail service, our system incorporates several extended features. Firstly, we introduce a decentralized naming service that effectively manages addresses and locations of mailbox servers. This service operates in a network of hierarchically structured nameservers, similar to DNS. A single top-level domain is hosted by the root nameserver, and each domain can be further divided into smaller subdomains (zones) managed by respective nameservers. Transfer servers utilize this network to query nameservers for mailbox server lookups, eliminating the need for a static configuration file.

Furthermore, our system employs opportunistic encryption to secure DMAP channels, ensuring the privacy and integrity of communication. Users can enjoy a seamless and protected messaging experience. Moreover, we implement integrity verification mechanisms for messages, adding an extra layer of trust and reliability to the system.

Lastly, MailService includes a user-friendly message client that simplifies user access and interaction with the mail service. With an intuitive interface, users can conveniently compose, send, receive, and manage their messages.

Overall, MailService presents a robust and feature-rich electronic mail service, improving upon the basic functionalities through decentralized naming, opportunistic encryption, integrity verification, and a user-friendly message client.

<img width="1157" alt="image" src="https://user-images.githubusercontent.com/61852663/230803273-512afcc7-2cbc-4de1-921e-b549a144e027.png">

<img width="996" alt="SCR-20230613-lakx" src="https://github.com/sueszli/mailService/assets/61852663/5077b195-a931-4a41-9201-3df49562f14d">
