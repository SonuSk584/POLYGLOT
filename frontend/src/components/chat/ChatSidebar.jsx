import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useChat } from '../../contexts/ChatContext';
import { useAuth } from '../../contexts/AuthContext';
import Button from '../ui/Button';

const ChatSidebar = () => {
  const { chatRooms, currentRoom, setCurrentRoom, loading, createDirectChat } = useChat();
  const { user } = useAuth();
  const [showNewChatModal, setShowNewChatModal] = useState(false);
  const [newContactId, setNewContactId] = useState('');
  const [isMobile, setIsMobile] = useState(false);
  const [showMobileSidebar, setShowMobileSidebar] = useState(true);

  useEffect(() => {
    const checkIfMobile = () => {
      setIsMobile(window.innerWidth < 768);
    };
    
    checkIfMobile();
    window.addEventListener('resize', checkIfMobile);
    
    return () => {
      window.removeEventListener('resize', checkIfMobile);
    };
  }, []);

  const handleCreateDirectChat = async () => {
    if (newContactId) {
      const newRoom = await createDirectChat(newContactId);
      if (newRoom) {
        setCurrentRoom(newRoom);
        setShowNewChatModal(false);
        setNewContactId('');
        if (isMobile) setShowMobileSidebar(false);
      }
    }
  };

  const getOtherParticipant = (room) => {
    if (room.type === 'DIRECT') {
      const otherUser = room.participantDetails.find(p => p.id !== user.id);
      return otherUser || { displayName: 'Unknown User' };
    }
    return null;
  };

  const getRoomName = (room) => {
    if (room.type === 'GROUP') {
      return room.groupName;
    } else {
      const otherUser = getOtherParticipant(room);
      return otherUser?.displayName || 'Unknown User';
    }
  };

  const getRoomAvatar = (room) => {
    if (room.type === 'GROUP') {
      return room.groupIconUrl || '/default-group.png';
    } else {
      const otherUser = getOtherParticipant(room);
      return otherUser?.profilePictureUrl || '/default-avatar.png';
    }
  };

  return (
    <>
      {/* Mobile toggle button */}
      {isMobile && (
        <button 
          className="fixed bottom-4 left-4 z-30 bg-primary-600 text-white p-3 rounded-full shadow-lg"
          onClick={() => setShowMobileSidebar(!showMobileSidebar)}
        >
          {showMobileSidebar ? (
            <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          ) : (
            <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
            </svg>
          )}
        </button>
      )}

      <AnimatePresence>
        {(!isMobile || showMobileSidebar) && (
          <motion.div 
            className={`${isMobile ? 'fixed z-20 left-0 top-0' : ''} w-80 bg-white dark:bg-gray-800 border-r border-gray-200 dark:border-gray-700 h-full flex flex-col`}
            initial={{ x: isMobile ? -320 : -80, opacity: 0 }}
            animate={{ x: 0, opacity: 1 }}
            exit={{ x: -320, opacity: 0 }}
            transition={{ duration: 0.3 }}
          >
            <div className="p-4 border-b border-gray-200 dark:border-gray-700">
              <div className="flex items-center justify-between">
                <h2 className="text-xl font-bold text-gray-800 dark:text-white">Chats</h2>
                <Button 
                  variant="primary" 
                  size="sm"
                  onClick={() => setShowNewChatModal(true)}
                >
                  New Chat
                </Button>
              </div>
            </div>

            <div className="flex-1 overflow-y-auto">
              {loading ? (
                <div className="flex justify-center p-4">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
                </div>
              ) : !chatRooms || chatRooms.length === 0 ? (
                <div className="p-4 text-center text-gray-500 dark:text-gray-400">
                  No chats yet. Start a new conversation!
                </div>
              ) : (
                <AnimatePresence>
                  {chatRooms.map((room) => (
                    <motion.div
                      key={room.id}
                      initial={{ opacity: 0, y: 10 }}
                      animate={{ opacity: 1, y: 0 }}
                      exit={{ opacity: 0, x: -20 }}
                      whileHover={{ backgroundColor: 'rgba(0,0,0,0.05)' }}
                      className={`p-3 cursor-pointer border-b border-gray-100 dark:border-gray-700 ${
                        currentRoom?.id === room.id ? 'bg-primary-50 dark:bg-primary-900/20' : ''
                      }`}
                      onClick={() => setCurrentRoom(room)}
                    >
                      <div className="flex items-center">
                        <div className="w-12 h-12 rounded-full overflow-hidden mr-3 flex-shrink-0">
                          <img 
                            src={getRoomAvatar(room)} 
                            alt={getRoomName(room)}
                            className="w-full h-full object-cover"
                            onError={(e) => {
                              e.target.onerror = null;
                              e.target.src = 'https://via.placeholder.com/40';
                            }}
                          />
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="flex justify-between items-baseline">
                            <h3 className="font-medium text-gray-900 dark:text-white truncate">
                              {getRoomName(room)}
                            </h3>
                            {room.lastMessageTimestamp && (
                              <span className="text-xs text-gray-500 dark:text-gray-400">
                                {new Date(room.lastMessageTimestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                              </span>
                            )}
                          </div>
                          <p className="text-sm text-gray-500 dark:text-gray-400 truncate">
                            {room.lastMessagePreview || 'No messages yet'}
                          </p>
                        </div>
                      </div>
                    </motion.div>
                  ))}
                </AnimatePresence>
              )}
            </div>

            {/* New Chat Modal */}
            <AnimatePresence>
              {showNewChatModal && (
                <motion.div
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
                  onClick={() => setShowNewChatModal(false)}
                >
                  <motion.div
                    initial={{ scale: 0.9, opacity: 0 }}
                    animate={{ scale: 1, opacity: 1 }}
                    exit={{ scale: 0.9, opacity: 0 }}
                    className="bg-white dark:bg-gray-800 rounded-lg p-6 w-full max-w-md"
                    onClick={(e) => e.stopPropagation()}
                  >
                    <h3 className="text-xl font-bold mb-4 text-gray-900 dark:text-white">Start New Chat</h3>
                    <div className="mb-4">
                      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                        User ID
                      </label>
                      <input
                        type="text"
                        value={newContactId}
                        onChange={(e) => setNewContactId(e.target.value)}
                        className="input w-full"
                        placeholder="Enter user ID"
                      />
                    </div>
                    <div className="flex justify-end space-x-3">
                      <Button 
                        variant="outline" 
                        onClick={() => setShowNewChatModal(false)}
                      >
                        Cancel
                      </Button>
                      <Button 
                        variant="primary" 
                        onClick={handleCreateDirectChat}
                      >
                        Start Chat
                      </Button>
                    </div>
                  </motion.div>
                </motion.div>
              )}
            </AnimatePresence>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
};

export default ChatSidebar;