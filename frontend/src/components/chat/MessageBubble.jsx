import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useAuth } from '../../contexts/AuthContext';

const MessageBubble = ({ message, isFirst, isLast }) => {
  const { user } = useAuth();
  const [showOriginal, setShowOriginal] = useState(false);
  const isCurrentUser = message.senderId === user?.id;
  
  // Format timestamp
  const formattedTime = new Date(message.timestamp).toLocaleTimeString([], { 
    hour: '2-digit', 
    minute: '2-digit' 
  });

  // Animation variants
  const bubbleVariants = {
    hidden: { 
      opacity: 0, 
      x: isCurrentUser ? 20 : -20,
      y: 10
    },
    visible: { 
      opacity: 1, 
      x: 0,
      y: 0,
      transition: { 
        type: 'spring',
        stiffness: 500,
        damping: 40
      }
    }
  };

  // Determine bubble style based on sender
  const bubbleStyle = isCurrentUser
    ? 'bg-primary-500 text-white rounded-tl-2xl rounded-tr-2xl rounded-bl-2xl'
    : 'bg-gray-200 dark:bg-gray-700 text-gray-800 dark:text-white rounded-tl-2xl rounded-tr-2xl rounded-br-2xl';

  // Determine container alignment
  const containerStyle = isCurrentUser
    ? 'justify-end'
    : 'justify-start';
  
  // ✅ Get the translated text for the current user's preferred language
  const getDisplayText = () => {
    // If showing original, return original text
    if (showOriginal) {
      return message.originalText;
    }
    
    // Get user's preferred language from their profile
    const userPreferredLanguage = user?.preferredLanguage || 'en';
    
    // If message has translations and user's language is available, show it
    if (message.translations && message.translations[userPreferredLanguage]) {
      return message.translations[userPreferredLanguage];
    }
    
    // Fallback to original text if no translation available
    return message.originalText;
  };
  
  // ✅ Check if translation is available
  const hasTranslation = () => {
    const userPreferredLanguage = user?.preferredLanguage || 'en';
    return message.translations && 
           message.translations[userPreferredLanguage] && 
           message.translations[userPreferredLanguage] !== message.originalText;
  };
  
  // ✅ Check if currently showing translated text
  const isShowingTranslation = () => {
    if (showOriginal) return false;
    const userPreferredLanguage = user?.preferredLanguage || 'en';
    return message.translations && 
           message.translations[userPreferredLanguage] &&
           message.translations[userPreferredLanguage] !== message.originalText;
  };

  return (
    <motion.div 
      className={`flex ${containerStyle} mb-2`}
      variants={bubbleVariants}
      initial="hidden"
      animate="visible"
    >
      <div className="max-w-[75%]">
        {!isCurrentUser && isFirst && (
          <div className="ml-2 mb-1 text-xs text-gray-500 dark:text-gray-400">
            {message.senderName}
          </div>
        )}
        
        <motion.div 
          className={`px-4 py-2 ${bubbleStyle}`}
          whileHover={{ scale: 1.01 }}
        >
          {/* ✅ Show translated text by default, or original if no translation */}
          <div>{getDisplayText()}</div>
          
          {/* ✅ Show original message when user clicks "Show original" */}
          <AnimatePresence>
            {showOriginal && message.originalLanguage && (
              <motion.div 
                className="mt-2 pt-2 border-t border-white/20 dark:border-gray-600/30"
                initial={{ opacity: 0, height: 0 }}
                animate={{ opacity: 1, height: 'auto' }}
                exit={{ opacity: 0, height: 0 }}
                transition={{ duration: 0.2 }}
              >
                <div className="text-sm opacity-90">
                  <span className="font-medium">
                    {message.originalLanguage.toUpperCase()}:
                  </span> {message.originalText}
                </div>
              </motion.div>
            )}
          </AnimatePresence>
          
          {/* ✅ Only show toggle button if translation exists and is different from original */}
          {hasTranslation() && (
            <div className="flex justify-between items-center mt-1">
              <button 
                onClick={() => setShowOriginal(!showOriginal)}
                className="text-xs underline opacity-70 hover:opacity-100 transition-opacity"
              >
                {showOriginal ? 'Hide original' : 'Show original'}
              </button>
              
              {/* ✅ Show indicator when viewing translation */}
              {isShowingTranslation() && !showOriginal && (
                <span className="text-xs opacity-60 italic ml-2">
                  Translated
                </span>
              )}
            </div>
          )}
        </motion.div>
        
        <div className={`text-xs text-gray-500 dark:text-gray-400 mt-1 ${isCurrentUser ? 'text-right mr-2' : 'ml-2'}`}>
          {formattedTime}
        </div>
      </div>
    </motion.div>
  );
};

export default MessageBubble;