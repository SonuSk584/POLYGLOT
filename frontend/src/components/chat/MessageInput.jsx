import { useState } from 'react';
import { motion } from 'framer-motion';
import { useChat } from '../../contexts/ChatContext';
import Button from '../ui/Button';

const MessageInput = ({ roomId }) => {
  const [message, setMessage] = useState('');
  const { sendMessage } = useChat();

  const handleSubmit = (e) => {
    e.preventDefault();
    if (message.trim()) {
      sendMessage(roomId, message.trim());
      setMessage('');
    }
  };

  return (
    <motion.div 
      className="p-4 border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800"
      initial={{ y: 20, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      transition={{ duration: 0.3 }}
    >
      <form onSubmit={handleSubmit} className="flex items-center space-x-2">
        <motion.input
          type="text"
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          placeholder="Type your message..."
          className="input flex-1"
          whileFocus={{ scale: 1.01 }}
          transition={{ type: 'spring', stiffness: 400, damping: 25 }}
        />
        <motion.div whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}>
          <Button type="submit" disabled={!message.trim()}>
            <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
              <path fillRule="evenodd" d="M10.293 3.293a1 1 0 011.414 0l6 6a1 1 0 010 1.414l-6 6a1 1 0 01-1.414-1.414L14.586 11H3a1 1 0 110-2h11.586l-4.293-4.293a1 1 0 010-1.414z" clipRule="evenodd" />
            </svg>
          </Button>
        </motion.div>
      </form>
    </motion.div>
  );
};

export default MessageInput;