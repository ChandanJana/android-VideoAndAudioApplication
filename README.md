# android-VideoAndAudioApplication

What Is WebRTC?

The WebRTC protocol (stands for Web Real-Time Communication), which allows for real-time communication, such as audio & video streaming and generic data sent between multiple clients and establishing direct peer-to-peer connections.
The most significant advantage of the peer-to-peer connection is that you can reduce a ton of tasks from the server by dividing the responsibilities of transmitting a lot of data that needs to be sent to each client, reducing latency.

However, it’s difficult to establish a peer-to-peer connection, unlike a traditional server-based connection. Because each peer doesn’t know the destination to send data and many obstacles, which means there are a lot of routers, proxies, and firewalls between peers. In some cases, there are more than tens of thousands of potential local network addresses.

The Signaling Server?

To achieve this complicated connection between peers, you need to build your WebRTC signaling server. The signaling server is responsible for resolving and establishing a connection between peers to allow peers to connect with each other by exposing minimized private information

Session Description Protocol?

For describing the connectivity information, WebRTC uses SDP (Session Description Protocol), which is a standard format for describing multimedia communication sessions for a peer-to-peer connection.

The SDP includes some information about the peer connection, such as Codec, source address, media types of audio and video, and other associated properties

The scenario of exchanging SDP is pretty simple. Let’s assume that Alice and Bob want to connect on a video call:

1. Alice suggests a peer connection with Bob by creating an SDP Offer that the signaling server will deliver.
2. Bob accepts the SDP Offer and responds by creating an SDP Answer that the signaling server will deliver.
3. Alice accepts the SDP Answer, and they will prepare to establish a connection between them.

Interactive Connectivity Establishment(ICE)?

Peers exchanged SDP messages through an offer/answer to set up media sessions, and now they need to connect to transfer real-time data.
ICE is used to discover the entire methods used to make that connection through the NAT with a combination of the STUN (Session Traversal Utilities for NAT) and TURN (Traversal Using Relays around NAT) protocols.

PeerConnection?

PeerConnection is one of the essential concepts to connect between a local computer and a remote peer. It provides methods to create and set an SDP offer/answer, add ICE candidates, potentially connect to a remote peer, monitor the connection, and close the connection once it’s no longer needed.
