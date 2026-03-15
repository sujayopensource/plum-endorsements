import { useEffect, useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { createStompClient, type WebSocketEvent } from '@/lib/websocket';
import { queryKeys } from '@/lib/query-keys';
import type { Client } from '@stomp/stompjs';

export type ConnectionStatus = 'connected' | 'disconnected' | 'reconnecting';

export function useWebSocket(employerId: string) {
  const queryClient = useQueryClient();
  const clientRef = useRef<Client | null>(null);
  const [status, setStatus] = useState<ConnectionStatus>('disconnected');

  useEffect(() => {
    if (!employerId) return;

    const client = createStompClient();
    clientRef.current = client;

    client.onConnect = () => {
      setStatus('connected');
      client.subscribe(`/topic/employer/${employerId}`, (message) => {
        try {
          const event: WebSocketEvent = JSON.parse(message.body);
          handleEvent(event);
        } catch {
          // ignore malformed messages
        }
      });
    };

    client.onDisconnect = () => {
      setStatus('disconnected');
    };

    client.onStompError = () => {
      setStatus('reconnecting');
    };

    client.onWebSocketClose = () => {
      setStatus('reconnecting');
    };

    client.activate();

    function handleEvent(event: WebSocketEvent) {
      const { type, endorsementId } = event;

      // Invalidate endorsement lists for any endorsement event
      queryClient.invalidateQueries({ queryKey: queryKeys.endorsements.lists() });

      // Invalidate specific endorsement detail
      if (endorsementId) {
        queryClient.invalidateQueries({
          queryKey: queryKeys.endorsements.detail(endorsementId),
        });
      }

      // Invalidate EA accounts on financial events
      if (type === 'EADebited' || type === 'EACredited') {
        queryClient.invalidateQueries({ queryKey: queryKeys.eaAccounts.all });
      }

      // Invalidate batches on batch events
      if (type === 'BatchSubmitted' || type === 'BatchCompleted') {
        queryClient.invalidateQueries({
          queryKey: [...queryKeys.endorsements.all, 'batches'],
        });
      }
    }

    return () => {
      client.deactivate();
      clientRef.current = null;
    };
  }, [employerId, queryClient]);

  return { status };
}
