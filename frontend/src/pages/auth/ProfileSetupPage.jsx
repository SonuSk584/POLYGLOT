import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { useAuth } from '../../contexts/AuthContext';
import Input from '../../components/ui/Input';
import Button from '../../components/ui/Button';

const languages = [
  { code: 'en', name: 'English' },
  { code: 'es', name: 'Spanish' },
  { code: 'fr', name: 'French' },
  { code: 'de', name: 'German' },
  { code: 'zh', name: 'Chinese' },
  { code: 'hi', name: 'Hindi' },
  { code: 'ar', name: 'Arabic' },
  { code: 'ru', name: 'Russian' },
  { code: 'pt', name: 'Portuguese' },
  { code: 'ja', name: 'Japanese' },
  { code: 'kn', name: 'Kannada' },      // ✅ NEW
  { code: 'ne', name: 'Nepali' }
];

const themes = [
  { code: 'light', name: 'Light' },
  { code: 'dark', name: 'Dark' },
];

const ProfileSetupPage = () => {
  const { user, updateProfile } = useAuth();
  const navigate = useNavigate();
  const [imagePreview, setImagePreview] = useState(user?.profilePictureUrl || null);

  const [formData, setFormData] = useState({
    displayName: user?.displayName || '',
    username: user?.username || '',
    preferredLanguage: user?.preferredLanguage || 'en',
    profilePictureUrl: user?.profilePictureUrl || '',
    theme: user?.theme || 'light',       // <-- ADD THIS
  });

  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);

    const success = await updateProfile(formData);
    if (success) {
      navigate('/chat');
    }

    setIsSubmitting(false);
  };

  const containerVariants = {
    hidden: { opacity: 0 },
    visible: {
      opacity: 1,
      transition: {
        when: "beforeChildren",
        staggerChildren: 0.1
      }
    }
  };

  const itemVariants = {
    hidden: { y: 20, opacity: 0 },
    visible: { y: 0, opacity: 1 }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-primary-50 to-secondary-50 dark:from-gray-900 dark:to-gray-800 p-4">
      <motion.div
        className="card max-w-md w-full"
        initial={{ scale: 0.9, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        transition={{ duration: 0.5 }}
      >
        <motion.div
          variants={containerVariants}
          initial="hidden"
          animate="visible"
        >
          <motion.div variants={itemVariants} className="text-center mb-8">
            <h1 className="text-3xl font-bold text-primary-600 dark:text-primary-400">
              Complete Your Profile
            </h1>
            <p className="text-gray-600 dark:text-gray-400 mt-2">
              Tell us a bit about yourself
            </p>
          </motion.div>

          <motion.form onSubmit={handleSubmit} variants={containerVariants}>

            {/* Display Name */}
            <motion.div variants={itemVariants}>
              <Input
                label="Display Name"
                name="displayName"
                value={formData.displayName}
                onChange={handleChange}
                placeholder="How you want to be known"
                required
              />
            </motion.div>

            {/* Username */}
            <motion.div variants={itemVariants}>
              <Input
                label="Username"
                name="username"
                value={formData.username}
                onChange={handleChange}
                placeholder="Choose a unique username"
                required
              />
            </motion.div>

            {/* Preferred Language */}
            <motion.div variants={itemVariants} className="mb-4">
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Preferred Language
              </label>
              <select
                name="preferredLanguage"
                value={formData.preferredLanguage}
                onChange={handleChange}
                className="input w-full"
                required
              >
                {languages.map((lang) => (
                  <option key={lang.code} value={lang.code}>
                    {lang.name}
                  </option>
                ))}
              </select>
            </motion.div>

            {/* Theme Selector */}
            <motion.div variants={itemVariants} className="mb-4">
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                App Theme
              </label>
              <select
                name="theme"
                value={formData.theme}
                onChange={handleChange}
                className="input w-full"
                required
              >
                {themes.map((th) => (
                  <option key={th.code} value={th.code}>
                    {th.name}
                  </option>
                ))}
              </select>
            </motion.div>

            {/* Profile Picture Upload */}
            <motion.div variants={itemVariants} className="mb-4">
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Profile Picture
              </label>

              <input
                type="file"
                accept="image/*"
                onChange={(e) => {
                  const file = e.target.files[0];
                  setFormData(prev => ({ ...prev, profileImageFile: file }));
                  setImagePreview(URL.createObjectURL(file)); // Instant preview
                }}
                className="input w-full"
              />

              {imagePreview && (
                <img
                  src={imagePreview}
                  alt="Preview"
                  className="w-24 h-24 rounded-full mt-3 object-cover border"
                />
              )}
            </motion.div>


            {/* Submit button */}
            <motion.div variants={itemVariants} className="mt-6">
              <Button
                type="submit"
                fullWidth
                isLoading={isSubmitting}
              >
                Complete Setup
              </Button>
            </motion.div>

          </motion.form>
        </motion.div>
      </motion.div>
    </div>
  );
};

export default ProfileSetupPage;
