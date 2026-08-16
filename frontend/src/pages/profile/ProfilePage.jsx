import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { useAuth } from '../../contexts/AuthContext';
import Button from '../../components/ui/Button';
import Input from '../../components/ui/Input';

const ProfilePage = () => {
  const { user, updateProfile, loading, error } = useAuth();
  const [formData, setFormData] = useState({
    displayName: '',
    username: '',
    preferredLanguage: '',
    profilePictureUrl: ''
  });
  const [isEditing, setIsEditing] = useState(false);
  const [updateSuccess, setUpdateSuccess] = useState(false);

  // Load user data when component mounts
  useEffect(() => {
    if (user) {
      setFormData({
        displayName: user.displayName || '',
        username: user.username || '',
        preferredLanguage: user.preferredLanguage || '',
        profilePictureUrl: user.profilePictureUrl || ''
      });
    }
  }, [user]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const success = await updateProfile(formData);
    if (success) {
      setIsEditing(false);
      setUpdateSuccess(true);
      setTimeout(() => setUpdateSuccess(false), 3000);
    }
  };

  if (!user) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <p>Loading user profile...</p>
      </div>
    );
  }

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
          <div className="flex justify-between items-center mb-6">
            <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Your Profile</h1>
            {!isEditing && (
              <Button 
                onClick={() => setIsEditing(true)}
                variant="secondary"
                size="sm"
              >
                Edit Profile
              </Button>
            )}
          </div>

          {updateSuccess && (
            <motion.div 
              className="mb-4 p-3 bg-green-100 text-green-700 rounded-md"
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0 }}
            >
              Profile updated successfully!
            </motion.div>
          )}

          {error && (
            <motion.div 
              className="mb-4 p-3 bg-red-100 text-red-700 rounded-md"
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
            >
              {error}
            </motion.div>
          )}

          <form onSubmit={handleSubmit}>
            <div className="mb-6 flex justify-center">
              <motion.div 
                className="w-24 h-24 rounded-full overflow-hidden bg-gray-200 dark:bg-gray-700 flex items-center justify-center"
                whileHover={{ scale: 1.05 }}
              >
                {formData.profilePictureUrl ? (
                  <img 
                    src={formData.profilePictureUrl} 
                    alt={formData.displayName} 
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <span className="text-3xl text-gray-400">{formData.displayName?.charAt(0)?.toUpperCase() || '?'}</span>
                )}
              </motion.div>
            </div>

            <div className="space-y-4">
              <Input
                label="Display Name"
                name="displayName"
                value={formData.displayName}
                onChange={handleChange}
                disabled={!isEditing}
                required
              />
              
              <Input
                label="Username"
                name="username"
                value={formData.username}
                onChange={handleChange}
                disabled={!isEditing}
                required
              />
              
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Preferred Language
                </label>
                <select
                  name="preferredLanguage"
                  value={formData.preferredLanguage}
                  onChange={handleChange}
                  disabled={!isEditing}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 dark:bg-gray-700 dark:text-white"
                  required
                >
                  <option value="">Select a language</option>
                  <option value="en">English</option>
                  <option value="es">Spanish</option>
                  <option value="fr">French</option>
                  <option value="de">German</option>
                  <option value="it">Italian</option>
                  <option value="pt">Portuguese</option>
                  <option value="ru">Russian</option>
                  <option value="zh">Chinese</option>
                  <option value="ja">Japanese</option>
                  <option value="ko">Korean</option>
                  <option value="hi">Hindi</option>
                  <option value="kn">Kannada</option>
                  <option value="ne">Nepali</option>
                  <option value="bn">Bengali</option>
                </select>
              </div>
              
              <Input
                label="Profile Picture URL"
                name="profilePictureUrl"
                value={formData.profilePictureUrl}
                onChange={handleChange}
                disabled={!isEditing}
                placeholder="https://example.com/your-image.jpg"
              />
            </div>

            {isEditing && (
              <motion.div 
                className="mt-6 flex space-x-3"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ delay: 0.3 }}
              >
                <Button
                  type="submit"
                  variant="primary"
                  loading={loading}
                  fullWidth
                >
                  Save Changes
                </Button>
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => {
                    setIsEditing(false);
                    // Reset form to original user data
                    if (user) {
                      setFormData({
                        displayName: user.displayName || '',
                        username: user.username || '',
                        preferredLanguage: user.preferredLanguage || '',
                        profilePictureUrl: user.profilePictureUrl || ''
                      });
                    }
                  }}
                  fullWidth
                >
                  Cancel
                </Button>
              </motion.div>
            )}
          </form>
        </motion.div>
      </div>
    </motion.div>
  );
};

export default ProfilePage;