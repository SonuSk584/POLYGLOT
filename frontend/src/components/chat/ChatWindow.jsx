import { useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useChat } from '../../contexts/ChatContext';
import { useAuth } from '../../contexts/AuthContext';
import MessageBubble from './MessageBubble';
import MessageInput from './MessageInput';

const ChatWindow = () => {
  const { currentRoom, messages } = useChat();
  const { user } = useAuth();
  const messagesEndRef = useRef(null);
  
  // Scroll to bottom when messages change
  useEffect(() => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, currentRoom]);

  // Get room name
  const getRoomName = () => {
    if (!currentRoom) return '';
    
    if (currentRoom.type === 'GROUP') {
      return currentRoom.groupName;
    } else {
      const otherUser = currentRoom.participantDetails?.find(p => p.id !== user?.id);
      return otherUser?.displayName || 'Chat';
    }
  };

  // Get current room messages
  const currentMessages = currentRoom ? (messages[currentRoom.id] || []) : [];

  return (
    <motion.div 
      className="flex-1 flex flex-col h-full bg-gray-50 dark:bg-gray-900"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.3 }}
    >
      {currentRoom ? (
        <>
          {/* Chat header */}
          <motion.div 
            className="p-4 border-b border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 flex items-center"
            initial={{ y: -20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ duration: 0.3 }}
          >
            <h2 className="text-xl font-bold text-gray-800 dark:text-white">{getRoomName()}</h2>
          </motion.div>
          
          {/* Messages area */}
          <div className="flex-1 overflow-y-auto p-4">
            {currentMessages.length === 0 ? (
              <div className="h-full flex items-center justify-center">
                <motion.p 
                  className="text-gray-500 dark:text-gray-400 text-center"
                  initial={{ scale: 0.9, opacity: 0 }}
                  animate={{ scale: 1, opacity: 1 }}
                  transition={{ duration: 0.5 }}
                >
                  No messages yet. Start the conversation!
                </motion.p>
              </div>
            ) : (
              <AnimatePresence>
                {currentMessages.map((message, index) => (
                  <MessageBubble 
                    key={message.id || index}
                    message={message}
                    isFirst={index === 0 || currentMessages[index - 1].senderId !== message.senderId}
                    isLast={index === currentMessages.length - 1 || currentMessages[index + 1].senderId !== message.senderId}
                  />
                ))}
              </AnimatePresence>
            )}
            <div ref={messagesEndRef} />
          </div>
          
          {/* Message input */}
          <MessageInput roomId={currentRoom.id} />
        </>
      ) : (
        <div className="h-full flex items-center justify-center">
          <motion.div 
            className="text-center"
            initial={{ scale: 0.9, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ duration: 0.5 }}
          >
            <h2 className="text-2xl font-bold text-gray-800 dark:text-white mb-2">Welcome to Polyglot Chat</h2>
            <p className="text-gray-600 dark:text-gray-400">Select a chat or start a new conversation</p>
          </motion.div>
        </div>
      )}
    </motion.div>
  );
};

export default ChatWindow;