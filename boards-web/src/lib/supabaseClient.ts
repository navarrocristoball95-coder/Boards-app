import { createClient } from '@supabase/supabase-js';

const DEFAULT_SUPABASE_URL = 'https://nbtzhmsyvjjgtkfupsby.supabase.co';
const DEFAULT_ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im5idHpobXN5dmpqZ3RrZnVwc2J5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY3MDU0NzAsImV4cCI6MjEwMjI4MTQ3MH0.yD3i_dAmNDINxpJzu22AvY_S7WqWT7VGD3m8KEGAhOk';

const STORED_URL = localStorage.getItem('boards_supabase_url') || import.meta.env.VITE_SUPABASE_URL || DEFAULT_SUPABASE_URL;
const STORED_KEY = localStorage.getItem('boards_supabase_anon_key') || import.meta.env.VITE_SUPABASE_ANON_KEY || DEFAULT_ANON_KEY;

export let supabase = createClient(STORED_URL, STORED_KEY);

export const updateSupabaseCredentials = (url: string, anonKey: string) => {
  localStorage.setItem('boards_supabase_url', url);
  localStorage.setItem('boards_supabase_anon_key', anonKey);
  supabase = createClient(url, anonKey);
};

export const getStoredCredentials = () => {
  return {
    url: localStorage.getItem('boards_supabase_url') || import.meta.env.VITE_SUPABASE_URL || DEFAULT_SUPABASE_URL,
    anonKey: localStorage.getItem('boards_supabase_anon_key') || import.meta.env.VITE_SUPABASE_ANON_KEY || DEFAULT_ANON_KEY,
  };
};

export const hasValidCredentials = () => {
  const { anonKey } = getStoredCredentials();
  return Boolean(anonKey && anonKey.trim().length > 20);
};
