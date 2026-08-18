import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

class WebSocketService {
  constructor() {
    this.client = null;
    this.subscriptions = new Map();
    this.connected = false;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
  }

  connect(token, onConnected, onError) {
    if (this.client && this.client.connected) {
      console.log('WebSocket already connected');
      return;
    }

    // Build WebSocket URL pointing to Spring Boot backend
    const backendHost = import.meta.env.VITE_API_BASE_URL
      ? import.meta.env.VITE_API_BASE_URL.replace(/\/api\/?$/, '')
      : 'http://localhost:8080';
    const timestamp = new Date().getTime();

    // ✅ SockJS expects HTTP(S), not WS(S)
    const wsUrl = `${backendHost}/ws?token=${token}&t=${timestamp}`;

    console.log('Attempting SockJS connection to:', wsUrl);

    this.client = new Client({
      webSocketFactory: () => {
        const socket = new SockJS(wsUrl);
        socket.onopen = () => console.log('✓ SockJS socket opened');
        socket.onerror = (error) => console.error('✗ SockJS socket error:', error);
        return socket;
      },
      connectHeaders: {
        Authorization: `Bearer ${token}`,
        token: token
      },
      debug: function (str) {
        console.log('STOMP: ' + str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: (frame) => {
        console.log('✓ WebSocket connected successfully');
        this.connected = true;
        this.reconnectAttempts = 0;
        if (onConnected) onConnected();
      },
      onStompError: (frame) => {
        console.error('✗ STOMP error:', frame);
        this.connected = false;
        if (onError) onError(frame);
      },
      onWebSocketError: (error) => {
        console.error('✗ WebSocket error:', error);
        this.connected = false;
        if (onError) onError(error);
      },
      onWebSocketClose: () => {
        console.log('⚠ WebSocket connection closed');
        this.connected = false;
      }
    });

    this.client.activate();
  }

  disconnect() {
    if (this.client && this.client.connected) {
      this.subscriptions.forEach((subscription) => {
        if (subscription) {
          subscription.unsubscribe();
        }
      });
      this.subscriptions.clear();

      this.client.deactivate();
      this.connected = false;
      console.log('WebSocket disconnected');
    }
  }

  subscribe(destination, callback) {
    if (!this.client || !this.client.connected) {
      console.warn('Cannot subscribe: WebSocket not connected');
      return null;
    }

    if (this.subscriptions.has(destination)) {
      console.log('Already subscribed to:', destination);
      return this.subscriptions.get(destination);
    }

    console.log('Subscribing to:', destination);

    const subscription = this.client.subscribe(destination, (message) => {
      try {
        const payload = JSON.parse(message.body);
        console.log('✓ Message received:', payload);
        callback(payload);
      } catch (error) {
        console.error('Error parsing message:', error);
      }
    });

    this.subscriptions.set(destination, subscription);
    return subscription;
  }

  // Subscribe to user-specific destinations
  subscribeToUser(userId, destination, callback) {
    const userDestination = `/user/${userId}${destination}`;
    return this.subscribe(userDestination, callback);
  }

  unsubscribe(destination) {
    const subscription = this.subscriptions.get(destination);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(destination);
      console.log('Unsubscribed from:', destination);
      return true;
    }
    return false;
  }

  send(destination, body) {
    if (!this.client || !this.client.connected) {
      console.warn('Cannot send message: WebSocket not connected');
      return false;
    }

    console.log('Sending message to:', destination, body);

    this.client.publish({
      destination,
      body: JSON.stringify(body)
    });

    return true;
  }

  isConnected() {
    return this.connected && this.client && this.client.connected;
  }
}

const websocketService = new WebSocketService();
export default websocketService;