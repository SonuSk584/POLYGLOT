import { motion } from 'framer-motion';
import { ChatProvider } from '../../contexts/ChatContext';
import ChatSidebar from '../../components/chat/ChatSidebar';
import ChatWindow from '../../components/chat/ChatWindow';
import Navigation from '../../components/layout/Navigation';

const ChatPage = () => {
  return (
    <ChatProvider>
      <Navigation />
      <motion.div 
        className="h-screen flex overflow-hidden pt-14 bg-gray-50 dark:bg-gray-900"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 0.5 }}
      >
        <ChatSidebar />
        <ChatWindow />
      </motion.div>
    </ChatProvider>
  );
};

export default ChatPage;