import axios from 'axios';
import { createContext } from 'react';

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('cc_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export const errMsg = (error) =>
  error.response?.data?.error ||
  (error.request ? 'Cannot reach the backend. Is it running on port 8080?' : 'Unexpected error');

export const fmt = (value) =>
  new Date(value).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' });

export const Ctx = createContext({ user: null, uid: null, toast: () => {}, login: () => {} });
export const CATEGORIES = ['Hackathon', 'Workshop', 'Cultural', 'Sports', 'Seminar', 'Club'];
