import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { useAuth } from '../../contexts/AuthContext';

const languages = [
  { code: 'en', name: 'English' },
  { code: 'es', name: 'Spanish' },
  { code: 'fr', name: 'French' },
  { code: 'de', name: 'German' },
  { code: 'zh', name: 'Chinese' },
  { code: 'ja', name: 'Japanese' },
  { code: 'hi', name: 'Hindi' },        // Add if not present
  { code: 'kn', name: 'Kannada' },      // ✅ NEW
  { code: 'ne', name: 'Nepali' },
  { code: 'bn', name: 'Bengali' }        // ✅ NEW
];

const LanguageSelector = ({ onChange, value, label = "Select Language", className = "" }) => {
  const [isOpen, setIsOpen] = useState(false);
  const [selectedLanguage, setSelectedLanguage] = useState(null);
  const { user } = useAuth();

  useEffect(() => {
    if (value) {
      const lang = languages.find(l => l.code === value);
      setSelectedLanguage(lang || null);
    } else if (user?.preferredLanguage) {
      const lang = languages.find(l => l.code === user.preferredLanguage);
      setSelectedLanguage(lang || null);
    }
  }, [value, user]);

  const handleSelect = (language) => {
    setSelectedLanguage(language);
    setIsOpen(false);
    if (onChange) onChange(language.code);
  };

  return (
    <div className={`relative ${className}`}>
      {label && (
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
          {label}
        </label>
      )}
      
      <button
        type="button"
        className="relative w-full bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm pl-3 pr-10 py-2 text-left cursor-pointer focus:outline-none focus:ring-1 focus:ring-primary-500 focus:border-primary-500"
        onClick={() => setIsOpen(!isOpen)}
      >
        <span className="block truncate">
          {selectedLanguage ? selectedLanguage.name : 'Select language'}
        </span>
        <span className="absolute inset-y-0 right-0 flex items-center pr-2 pointer-events-none">
          <svg className="h-5 w-5 text-gray-400" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
            <path fillRule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clipRule="evenodd" />
          </svg>
        </span>
      </button>

      {isOpen && (
        <div className="absolute z-10 mt-1 w-full bg-white dark:bg-gray-800 shadow-lg max-h-60 rounded-md py-1 text-base ring-1 ring-black ring-opacity-5 overflow-auto focus:outline-none sm:text-sm">
          {languages.map((language) => (
            <div
              key={language.code}
              className={`cursor-pointer select-none relative py-2 pl-3 pr-9 ${
                selectedLanguage?.code === language.code
                  ? 'text-white bg-primary-600 dark:bg-primary-700'
                  : 'text-gray-900 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700'
              }`}
              onClick={() => handleSelect(language)}
            >
              <span className="block truncate font-normal">
                {language.name}
              </span>
              
              {selectedLanguage?.code === language.code && (
                <span className="absolute inset-y-0 right-0 flex items-center pr-4">
                  <svg className="h-5 w-5" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                    <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                  </svg>
                </span>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default LanguageSelector;