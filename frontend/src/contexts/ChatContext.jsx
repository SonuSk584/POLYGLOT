import { createContext, useContext, useState, useEffect } from 'react';
import { useAuth } from './AuthContext';
import api from '../services/api';

const ChatContext = createContext();

export const useChat = () => useContext(ChatContext);

export const ChatProvider = ({ children }) => {
  const { user, isAuthenticated } = useAuth();
  const [chatRooms, setChatRooms] = useState([]);
  const [currentRoom, setCurrentRoom] = useState(null);
  const [messages, setMessages] = useState({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [wsConnected, setWsConnected] = useState(false);
  
  const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
  
  const connectWebSocket = () => {
    if (!token || !user) return;
    
    import('../services/websocket').then(module => {
      const websocketService = module.default;
      
      websocketService.connect(
        token,
        () => {
          console.log('✅ WebSocket connected successfully');
          setWsConnected(true);
          
          if (currentRoom) {
            subscribeToRoom(currentRoom.id);
          }
          
          websocketService.subscribeToUser(
            user.id,
            '/queue/roomUpdates',
            (roomUpdate) => {
              console.log('📩 Room update received:', roomUpdate);
              
              if (roomUpdate.action === 'CREATED' || roomUpdate.action === 'USER_ADDED') {
                fetchRooms();
              }
            }
          );
        },
        (error) => {
          console.error('❌ WebSocket connection error:', error);
          setWsConnected(false);
          setError('Failed to connect to real-time messaging');
        }
      );
    });
  };
  
  const subscribeToRoom = (roomId) => {
    if (!roomId) return;
    
    import('../services/websocket').then(module => {
      const websocketService = module.default;
      
      if (websocketService.isConnected()) {
        console.log('📡 Subscribing to room:', roomId);
        
        // ✅ FIXED: Use /topic/room/ instead of /topic/chat/
        websocketService.subscribe(`/topic/room/${roomId}`, (message) => {
          console.log('📩 Received message:', message);
          
          setMessages(prev => {
            const roomMessages = [...(prev[roomId] || [])];
            
            const exists = roomMessages.some(m => m.id === message.id);
            if (!exists) {
              roomMessages.push(message);
              
              // Sort by timestamp
              roomMessages.sort((a, b) => 
                new Date(a.timestamp) - new Date(b.timestamp)
              );
            }
            
            return {
              ...prev,
              [roomId]: roomMessages
            };
          });
        });
      } else {
        console.warn('⚠️ WebSocket not connected, cannot subscribe to room:', roomId);
      }
    });
  };

  const fetchRooms = async () => {
    if (!token) return;
    
    setLoading(true);
    try {
      console.log('📥 Fetching chat rooms...');
      const response = await api.get('/chat/rooms');
      console.log('✅ Chat rooms fetched:', response.data.length, 'rooms');
      setChatRooms(response.data);
      setError(null);
    } catch (err) {
      setError('Failed to fetch chat rooms');
      console.error('❌ Error fetching chat rooms:', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchMessages = async (roomId) => {
    if (!token || !roomId) return;
    
    try {
      console.log('📥 Fetching messages for room:', roomId);
      const response = await api.get(`/chat/rooms/${roomId}/messages`);
      console.log('✅ Messages fetched:', response.data.length, 'messages');
      setMessages(prev => ({
        ...prev,
        [roomId]: response.data
      }));
      setError(null);
    } catch (err) {
      setError('Failed to fetch messages');
      console.error('❌ Error fetching messages:', err);
    }
  };

  useEffect(() => {
    if (!currentRoom || !token) return;

    fetchMessages(currentRoom.id);
    subscribeToRoom(currentRoom.id);
    
  }, [currentRoom, token]);

  const sendMessage = async (roomId, content, targetLanguage = null) => {
  if (!token || !roomId || !content || !user) return;
  
  try {
    // ✅ FIXED: Get language from user object, not localStorage
    const userLanguage = user.preferredLanguage || 'en';
    
    console.log('📤 Sending message with source language:', userLanguage);
    
    const messageData = {
      roomId: roomId,
      senderId: user.id,
      content: content,
      sourceLanguage: userLanguage  // ✅ Now correctly uses user's preferred language
    };
    
    const websocketModule = await import('../services/websocket');
    const websocketService = websocketModule.default;
    
    if (websocketService.isConnected()) {
      console.log('📤 Sending message via WebSocket:', messageData);
      
      // Send to /app/chat.sendMessage
      websocketService.send('/app/chat.sendMessage', messageData);
      
      // Optimistic update
      const optimisticMessage = {
        id: 'temp-' + Date.now(),
        roomId: roomId,
        senderId: user.id,
        senderName: user.displayName || user.username,
        originalText: content,
        originalLanguage: userLanguage,
        timestamp: new Date(),
        translations: {}
      };
      
      setMessages(prev => {
        const roomMessages = [...(prev[roomId] || []), optimisticMessage];
        return {
          ...prev,
          [roomId]: roomMessages
        };
      });
      
      return optimisticMessage;
    } else {
      console.warn('⚠️ WebSocket not connected, using REST API');
      const response = await api.post(`/chat/rooms/${roomId}/messages`, {
        content,
        sourceLanguage: userLanguage  // ✅ Also fixed for REST API fallback
      });
      
      // Message will be broadcasted via WebSocket from backend
      return response.data;
    }
  } catch (err) {
    setError('Failed to send message');
    console.error('❌ Error sending message:', err);
    return null;
  }
};

  const createDirectChat = async (recipientId) => {
    if (!token || !recipientId) {
      console.error('❌ Missing token or recipientId');
      return null;
    }
    
    setLoading(true);
    try {
      console.log('📞 Creating direct chat with:', recipientId);
      
      const existingRoom = chatRooms.find(room => 
        room.type === 'DIRECT' && 
        room.participants.includes(recipientId) &&
        room.participants.length === 2
      );
      
      if (existingRoom) {
        console.log('✅ Direct chat already exists:', existingRoom.id);
        setCurrentRoom(existingRoom);
        return existingRoom;
      }
      
      const response = await api.post(`/chat/rooms/direct/${recipientId}`);
      
      console.log('✅ Direct chat created:', response.data);
      
      setChatRooms(prev => {
        const roomExists = prev.some(room => room.id === response.data.id);
        if (roomExists) return prev;
        return [...prev, response.data];
      });
      
      setCurrentRoom(response.data);
      
      const websocketModule = await import('../services/websocket');
      const websocketService = websocketModule.default;
      
      if (websocketService.isConnected()) {
        websocketService.send('/app/chat.roomUpdate', {
          roomId: response.data.id,
          action: 'CREATED',
          initiatedBy: user.id,
          targetUser: recipientId
        });
      }
      
      return response.data;
    } catch (err) {
      const errorMsg = err.response?.data?.error || err.response?.data?.message || 'Failed to create chat';
      setError(errorMsg);
      console.error('❌ Error creating direct chat:', err);
      return null;
    } finally {
      setLoading(false);
    }
  };

  const createGroupChat = async (name, participantIds) => {
    if (!token || !name || !participantIds?.length) return;
    
    setLoading(true);
    try {
      console.log('👥 Creating group chat:', { name, participantIds });
      const response = await api.post('/chat/rooms/group', {
        name,
        participants: participantIds
      });
      
      console.log('✅ Group chat created:', response.data);
      
      setChatRooms(prev => [...prev, response.data]);
      setCurrentRoom(response.data);
      
      return response.data;
    } catch (err) {
      const errorMsg = err.response?.data?.error || err.response?.data?.message || 'Failed to create group';
      setError(errorMsg);
      console.error('❌ Error creating group chat:', err);
      return null;
    } finally {
      setLoading(false);
    }
  };

  const selectRoom = async (roomId) => {
    const room = chatRooms.find(r => r.id === roomId);
    if (room) {
      setCurrentRoom(room);
      await fetchMessages(roomId);
    }
  };

  useEffect(() => {
    if (user && token) {
      connectWebSocket();
    }
  }, [user, token]);

  useEffect(() => {
    if (user && isAuthenticated) {
      fetchRooms();
    }
  }, [user, isAuthenticated]);

  const value = {
    chatRooms,
    currentRoom,
    setCurrentRoom,
    messages,
    loading,
    error,
    wsConnected,
    fetchRooms,
    fetchMessages,
    sendMessage,
    createDirectChat,
    createGroupChat,
    selectRoom
  };

  return (
    <ChatContext.Provider value={value}>
      {children}
    </ChatContext.Provider>
  );
};