import axios from 'axios';

export const API_BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add a request interceptor to include auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
      console.log('Token added to request:', token.substring(0, 20) + '...');
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Add a response interceptor to handle 401 errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const url = error.config?.url || '';
      console.error('401 Unauthorized on endpoint:', url);

      if (url.includes('/auth/me') || url.includes('/auth/profile')) {
        console.error('Auth token expired - redirecting to login');
        localStorage.removeItem('token');
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

// --------------------------
// ⭐ AUTH API
// --------------------------
export const authAPI = {

  // OLD Twilio (now unused — can delete later)
  sendOtp: (mobileNumber) => api.post('/auth/send-otp', { mobileNumber }),
  verifyOtp: (mobileNumber, otp) => api.post('/auth/verify-otp', { mobileNumber, otp }),

  getCurrentUser: () => api.get('/auth/me'),

  updateProfile: (formData) =>
    api.put('/auth/profile', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }),

  // ⭐ NEW — Firebase Login
  firebaseLogin: (firebaseToken) =>
    api.post("/auth/firebase-login", { firebaseToken }),
};


// --------------------------
// ⭐ CHAT API
// --------------------------
export const chatAPI = {
  getChatRooms: () => api.get('/chat/rooms'),
  getChatRoomById: (roomId) => api.get(`/chat/rooms/${roomId}`),
  createDirectChat: (userId) => api.post(`/chat/rooms/direct/${userId}`),
  createGroupChat: (groupData) => api.post('/chat/rooms/group', groupData),
  getMessages: (roomId, limit = 50) =>
    api.get(`/chat/rooms/${roomId}/messages?limit=${limit}`),
  getMessagesBefore: (roomId, timestamp, limit = 50) =>
    api.get(`/chat/rooms/${roomId}/messages?before=${timestamp}&limit=${limit}`),
};

export default api;
