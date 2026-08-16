import { createContext, useState, useEffect, useContext } from 'react';
import { authAPI } from '../services/api';

const AuthContext = createContext();

export const useAuth = () => useContext(AuthContext);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      fetchCurrentUser();
    } else {
      setLoading(false);
    }
  }, []);
  const firebaseLogin = async (firebaseUser) => {
  try {
    setLoading(true);
    setError(null);

    const firebaseToken = await firebaseUser.getIdToken();

    const { data } = await authAPI.firebaseLogin(firebaseToken);


    // Save JWT
    localStorage.setItem("token", data.token);

    // Save user
    setUser(data.user);
    setIsAuthenticated(true);

    // Apply theme instantly
    if (data.user?.theme === "dark") {
      document.documentElement.classList.add("dark");
    } else {
      document.documentElement.classList.remove("dark");
    }

    return {
      success: true,
      isNewUser: data.isNewUser,
      profileCompleted: data.profileCompleted,
    };

  } catch (err) {
    console.error(err);
    setError("Firebase login failed");
    return { success: false };
  } finally {
    setLoading(false);
  }
};



  const fetchCurrentUser = async () => {
    try {
      setLoading(true);
      const { data } = await authAPI.getCurrentUser();
      setUser(data);
      setIsAuthenticated(true);
    } catch (err) {
      console.error('Failed to fetch user:', err);
      localStorage.removeItem('token');
      setError('Session expired. Please login again.');
    } finally {
      setLoading(false);
    }
  };

  const sendOtp = async (mobileNumber) => {
    try {
      setLoading(true);
      setError(null);
      await authAPI.sendOtp(mobileNumber);
      return true;
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to send OTP');
      return false;
    } finally {
      setLoading(false);
    }
  };

  const verifyOtp = async (mobileNumber, otp) => {
    try {
      setLoading(true);
      setError(null);

      const { data } = await authAPI.verifyOtp(mobileNumber, otp);

      // Save token
      localStorage.setItem('token', data.token);

      // Save user object
      setUser(data.user);

      // Mark authenticated
      setIsAuthenticated(true);

      // ⭐ APPLY THE THEME IMMEDIATELY
      if (data.theme === "dark") {
        document.documentElement.classList.add("dark");
      } else {
        document.documentElement.classList.remove("dark");
      }

      return {
        success: true,
        isNewUser: data.isNewUser,
        profileCompleted: data.profileCompleted,
        theme: data.theme,
      };
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid OTP');
      return { success: false };
    } finally {
      setLoading(false);
    }
  };



  const updateProfile = async (userData) => {
    try {
      setLoading(true);
      setError(null);

      const form = new FormData();
      form.append("displayName", userData.displayName);
      form.append("username", userData.username);
      form.append("preferredLanguage", userData.preferredLanguage);
      form.append("theme", userData.theme);

      if (userData.profileImageFile) {
        form.append("image", userData.profileImageFile);  // <-- file here
      }

      const { data } = await authAPI.updateProfile(form);
      setUser(data);

      // Apply theme instantly
      if (data.theme === "dark") {
        document.documentElement.classList.add("dark");
      } else {
        document.documentElement.classList.remove("dark");
      }

      return true;
    } catch (err) {
      setError(err.response?.data?.message || "Failed to update profile");
      return false;
    } finally {
      setLoading(false);
    }
  };



  const logout = () => {
    localStorage.removeItem('token');
    setUser(null);
    setIsAuthenticated(false);
  };

  const value = {
    user,
    loading,
    error,
    isAuthenticated,
    token: localStorage.getItem('token'),
    sendOtp,
    verifyOtp,
    updateProfile,
    firebaseLogin,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export default AuthContext;