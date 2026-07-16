import axios from 'axios';
import { getTokenCookie, eraseTokenCookie } from '../utils/cookies';


const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
});

// Interceptor to add JWT token to requests
api.interceptors.request.use((config) => {
  const token = getTokenCookie();
  if (token && !config.url.includes('/api/auth/')) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Interceptor to handle 401 responses (unauthorized/expired token)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      eraseTokenCookie();
      localStorage.removeItem('token');
      window.location.href = '/admin/login';
    }
    return Promise.reject(error);
  }
);

export const getApiData = (response) => response?.data?.data ?? response?.data;

export default api;
