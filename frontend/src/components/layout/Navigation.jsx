import { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { motion } from 'framer-motion';
import { useAuth } from '../../contexts/AuthContext';

const Navigation = () => {
  const { user, logout } = useAuth();
  const location = useLocation();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const navItems = [
    { name: 'Chat', path: '/chat', icon: 'chat' },
    { name: 'Profile', path: '/profile', icon: 'user' },
    { name: 'Settings', path: '/settings', icon: 'settings' }
  ];

  const isActive = (path) => location.pathname === path;

  // Icons as SVG components
  const icons = {
    chat: (
      <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
      </svg>
    ),
    user: (
      <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
      </svg>
    ),
    settings: (
      <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
      </svg>
    ),
    logout: (
      <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
      </svg>
    ),
    menu: (
      <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
      </svg>
    ),
    close: (
      <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
      </svg>
    )
  };

  return (
    <>
      {/* Desktop Navigation */}
      <motion.nav 
        className="hidden md:flex fixed top-0 left-0 right-0 bg-white dark:bg-gray-800 shadow-md z-10"
        initial={{ y: -100 }}
        animate={{ y: 0 }}
        transition={{ duration: 0.5 }}
      >
        <div className="container mx-auto px-4 py-3 flex items-center justify-between">
          <Link to="/chat" className="flex items-center">
            <motion.span 
              className="text-xl font-bold text-primary-600 dark:text-primary-400"
              whileHover={{ scale: 1.05 }}
            >
              Polyglot Chat
            </motion.span>
          </Link>
          
          <div className="flex items-center space-x-4">
            {navItems.map((item) => (
              <Link 
                key={item.name}
                to={item.path}
                className={`flex items-center px-3 py-2 rounded-md text-sm font-medium transition-colors duration-200 ${
                  isActive(item.path) 
                    ? 'bg-primary-100 text-primary-700 dark:bg-primary-900 dark:text-primary-300' 
                    : 'text-gray-700 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-gray-700'
                }`}
              >
                <span className="mr-2">{icons[item.icon]}</span>
                {item.name}
              </Link>
            ))}
            
            {user && (
              <motion.button
                className="flex items-center px-3 py-2 rounded-md text-sm font-medium text-red-600 hover:bg-red-100 dark:text-red-400 dark:hover:bg-red-900 transition-colors duration-200"
                onClick={logout}
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
              >
                <span className="mr-2">{icons.logout}</span>
                Logout
              </motion.button>
            )}
          </div>
        </div>
      </motion.nav>

      {/* Mobile Navigation */}
      <div className="md:hidden fixed top-0 left-0 right-0 bg-white dark:bg-gray-800 shadow-md z-10">
        <div className="px-4 py-3 flex items-center justify-between">
          <Link to="/chat" className="flex items-center">
            <span className="text-xl font-bold text-primary-600 dark:text-primary-400">
              Polyglot Chat
            </span>
          </Link>
          
          <button
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            className="p-2 rounded-md text-gray-700 dark:text-gray-300"
          >
            {mobileMenuOpen ? icons.close : icons.menu}
          </button>
        </div>
        
        {/* Mobile menu dropdown */}
        {mobileMenuOpen && (
          <motion.div 
            className="px-2 pt-2 pb-3 space-y-1 bg-white dark:bg-gray-800 shadow-lg"
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            transition={{ duration: 0.3 }}
          >
            {navItems.map((item) => (
              <Link 
                key={item.name}
                to={item.path}
                className={`flex items-center px-3 py-2 rounded-md text-base font-medium ${
                  isActive(item.path) 
                    ? 'bg-primary-100 text-primary-700 dark:bg-primary-900 dark:text-primary-300' 
                    : 'text-gray-700 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-gray-700'
                }`}
                onClick={() => setMobileMenuOpen(false)}
              >
                <span className="mr-2">{icons[item.icon]}</span>
                {item.name}
              </Link>
            ))}
            
            {user && (
              <button
                className="w-full flex items-center px-3 py-2 rounded-md text-base font-medium text-red-600 hover:bg-red-100 dark:text-red-400 dark:hover:bg-red-900"
                onClick={() => {
                  logout();
                  setMobileMenuOpen(false);
                }}
              >
                <span className="mr-2">{icons.logout}</span>
                Logout
              </button>
            )}
          </motion.div>
        )}
      </div>
      
      {/* Spacer to prevent content from being hidden under the navigation */}
      <div className="h-14 md:h-16"></div>
    </>
  );
};

export default Navigation;