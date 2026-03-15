import { Client } from '@stomp/stompjs';

const WS_URL = import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws';

export function createStompClient(): Client {
  return new Client({
    brokerURL: WS_URL,
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
  });
}

export interface WebSocketEvent {
  type: string;
  endorsementId?: string;
  employerId?: string;
  batchId?: string;
  timestamp: string;
}
