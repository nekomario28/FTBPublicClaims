#!/usr/bin/env python3
import socket
import struct
import sys


def receive_packet(sock: socket.socket) -> tuple[int, int, str]:
    length_data = sock.recv(4)
    if len(length_data) != 4:
        raise RuntimeError("RCON connection closed before packet length")
    length = struct.unpack("<i", length_data)[0]
    payload = b""
    while len(payload) < length:
        chunk = sock.recv(length - len(payload))
        if not chunk:
            raise RuntimeError("RCON connection closed during packet")
        payload += chunk
    request_id, packet_type = struct.unpack("<ii", payload[:8])
    text = payload[8:-2].decode("utf-8", errors="replace")
    return request_id, packet_type, text


def send_packet(sock: socket.socket, request_id: int, packet_type: int, text: str) -> None:
    body = struct.pack("<ii", request_id, packet_type) + text.encode("utf-8") + b"\0\0"
    sock.sendall(struct.pack("<i", len(body)) + body)


def main() -> int:
    if len(sys.argv) < 5:
        print("usage: minecraft_rcon.py HOST PORT PASSWORD COMMAND", file=sys.stderr)
        return 2

    host, port_text, password = sys.argv[1:4]
    command = " ".join(sys.argv[4:])

    with socket.create_connection((host, int(port_text)), timeout=10) as sock:
        sock.settimeout(10)
        send_packet(sock, 1, 3, password)
        request_id, _, message = receive_packet(sock)
        if request_id == -1:
            raise RuntimeError("RCON authentication failed")
        if message:
            print(message)

        send_packet(sock, 2, 2, command)
        try:
            _, _, response = receive_packet(sock)
        except (ConnectionError, RuntimeError, socket.timeout):
            if command == "stop":
                return 0
            raise
        if response:
            print(response)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
