import { useEffect } from "react";
import { BrowserRouter as Router, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import LoginPage from './pages/auth/LoginPage';
import ProfileSetupPage from './pages/auth/ProfileSetupPage';
import ChatPage from './pages/chat/ChatPage';
import ProfilePage from './pages/profile/ProfilePage';
import SettingsPage from './pages/settings/SettingsPage';
import './App.css';

// 🔒 Protected route wrapper with profile setup logic
const ProtectedRoute = ({ children }) => {
  const { isAuthenticated, loading, user } = useAuth();
  const location = useLocation();

  if (loading) {
    return <div className="flex items-center justify-center min-h-screen">Loading...</div>;
  }

  // Not logged in → redirect to login
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  // Allow access to onboarding page even if profile is not completed
  if (location.pathname === "/profile/setup") {
    return children;
  }

  // Logged in BUT profile not completed → force onboarding
  if (user && user.profileCompleted === false) {
    return <Navigate to="/profile/setup" replace />;
  }

  return children;
};
function ApplyTheme() {
  const { user } = useAuth();

  useEffect(() => {
    if (!user) return;

    if (user.theme === "dark") {
      document.documentElement.classList.add("dark");
    } else {
      document.documentElement.classList.remove("dark");
    }
  }, [user]);

  return null;
}

function App() {
  return (
    <Router>
      <AuthProvider>
        <ApplyTheme /> 
        <Routes>

          {/* Public Route */}
          <Route path="/login" element={<LoginPage />} />

          {/* Profile Setup Route - only for authenticated users */}
          <Route
            path="/profile/setup"
            element={
              <ProtectedRoute>
                <ProfileSetupPage />
              </ProtectedRoute>
            }
          />

          {/* Protected Routes */}
          <Route
            path="/chat"
            element={
              <ProtectedRoute>
                <ChatPage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/profile"
            element={
              <ProtectedRoute>
                <ProfilePage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/settings"
            element={
              <ProtectedRoute>
                <SettingsPage />
              </ProtectedRoute>
            }
          />

          {/* Default route */}
          <Route path="/" element={<Navigate to="/chat" replace />} />

          {/* Catch-all route */}
          <Route path="*" element={<Navigate to="/login" replace />} />

        </Routes>
      </AuthProvider>
    </Router>
  );
}

export default App;
