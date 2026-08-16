import { useState } from 'react';
import { motion } from 'framer-motion';
import { useAuth } from '../../contexts/AuthContext';
import Button from '../../components/ui/Button';

const SettingsPage = () => {
  const { user } = useAuth();
  const [darkMode, setDarkMode] = useState(() => {
    // Check if user has a preference stored or use system preference
    const savedPreference = localStorage.getItem('darkMode');
    return savedPreference ? JSON.parse(savedPreference) : window.matchMedia('(prefers-color-scheme: dark)').matches;
  });
  const [notificationsEnabled, setNotificationsEnabled] = useState(true);
  const [autoTranslate, setAutoTranslate] = useState(true);
  const [saveSuccess, setSaveSuccess] = useState(false);

  // Toggle dark mode
  const toggleDarkMode = () => {
    const newValue = !darkMode;
    setDarkMode(newValue);
    localStorage.setItem('darkMode', JSON.stringify(newValue));
    
    // Apply dark mode to document
    if (newValue) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  };

  // Save settings
  const saveSettings = () => {
    // Save notification and translation preferences
    localStorage.setItem('notificationsEnabled', JSON.stringify(notificationsEnabled));
    localStorage.setItem('autoTranslate', JSON.stringify(autoTranslate));
    
    // Show success message
    setSaveSuccess(true);
    setTimeout(() => setSaveSuccess(false), 3000);
  };

  return (
    <motion.div 
      className="min-h-screen bg-gray-50 dark:bg-gray-900 py-12 px-4 sm:px-6 lg:px-8"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.5 }}
    >
      <div className="max-w-md mx-auto bg-white dark:bg-gray-800 rounded-xl shadow-md overflow-hidden">
        <motion.div 
          className="p-8"
          initial={{ y: 20, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          transition={{ delay: 0.2, duration: 0.5 }}
        >
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-6">Settings</h1>

          {saveSuccess && (
            <motion.div 
              className="mb-4 p-3 bg-green-100 text-green-700 rounded-md"
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0 }}
            >
              Settings saved successfully!
            </motion.div>
          )}

          <div className="space-y-6">
            {/* Appearance Section */}
            <div>
              <h2 className="text-lg font-medium text-gray-900 dark:text-white mb-3">Appearance</h2>
              <div className="flex items-center justify-between">
                <span className="text-gray-700 dark:text-gray-300">Dark Mode</span>
                <label className="relative inline-flex items-center cursor-pointer">
                  <input 
                    type="checkbox" 
                    className="sr-only peer" 
                    checked={darkMode}
                    onChange={toggleDarkMode}
                  />
                  <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-primary-300 dark:peer-focus:ring-primary-800 rounded-full peer dark:bg-gray-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-600 peer-checked:bg-primary-600"></div>
                </label>
              </div>
            </div>

            {/* Notifications Section */}
            <div>
              <h2 className="text-lg font-medium text-gray-900 dark:text-white mb-3">Notifications</h2>
              <div className="flex items-center justify-between">
                <span className="text-gray-700 dark:text-gray-300">Enable Notifications</span>
                <label className="relative inline-flex items-center cursor-pointer">
                  <input 
                    type="checkbox" 
                    className="sr-only peer" 
                    checked={notificationsEnabled}
                    onChange={() => setNotificationsEnabled(!notificationsEnabled)}
                  />
                  <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-primary-300 dark:peer-focus:ring-primary-800 rounded-full peer dark:bg-gray-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-600 peer-checked:bg-primary-600"></div>
                </label>
              </div>
            </div>

            {/* Translation Section */}
            <div>
              <h2 className="text-lg font-medium text-gray-900 dark:text-white mb-3">Translation</h2>
              <div className="flex items-center justify-between">
                <span className="text-gray-700 dark:text-gray-300">Auto-translate Messages</span>
                <label className="relative inline-flex items-center cursor-pointer">
                  <input 
                    type="checkbox" 
                    className="sr-only peer" 
                    checked={autoTranslate}
                    onChange={() => setAutoTranslate(!autoTranslate)}
                  />
                  <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-primary-300 dark:peer-focus:ring-primary-800 rounded-full peer dark:bg-gray-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-600 peer-checked:bg-primary-600"></div>
                </label>
              </div>
              
              {user && (
                <div className="mt-3">
                  <p className="text-sm text-gray-600 dark:text-gray-400">
                    Your preferred language: <span className="font-medium">{user.preferredLanguage || 'Not set'}</span>
                  </p>
                  <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
                    You can change your preferred language in your profile settings.
                  </p>
                </div>
              )}
            </div>

            {/* Save Button */}
            <motion.div 
              className="pt-4"
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
            >
              <Button
                onClick={saveSettings}
                variant="primary"
                fullWidth
              >
                Save Settings
              </Button>
            </motion.div>
          </div>
        </motion.div>
      </div>
    </motion.div>
  );
};

export default SettingsPage;