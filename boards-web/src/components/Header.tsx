import React from 'react';
import { Cloud, RefreshCw, Key, LogOut, Sun, Moon, Smartphone } from 'lucide-react';
import { User } from '@supabase/supabase-js';

interface HeaderProps {
  user: User | null;
  syncStatus: 'synced' | 'syncing' | 'offline' | 'error';
  theme: 'light' | 'dark';
  onToggleTheme: () => void;
  onOpenConfig: () => void;
  onOpenAuth: () => void;
  onSignOut: () => void;
  onManualSync: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  user,
  syncStatus,
  theme,
  onToggleTheme,
  onOpenConfig,
  onOpenAuth,
  onSignOut,
  onManualSync,
}) => {
  return (
    <header className="app-header">
      <div className="header-left">
        <div className="app-logo">
          <div className="logo-icon">⚡</div>
          <div className="logo-text">
            <h1>Boards <span className="badge-pro">Web Sync</span></h1>
            <p>Escribe en PC, envía en tu teléfono al instante</p>
          </div>
        </div>
      </div>

      <div className="header-right">
        {/* Sync Status Badge */}
        <button className={`sync-pill ${syncStatus}`} onClick={onManualSync} title="Clic para forzar sincronización">
          {syncStatus === 'syncing' && <RefreshCw size={14} className="spin" />}
          {syncStatus === 'synced' && <Cloud size={14} />}
          {syncStatus === 'offline' && <Smartphone size={14} />}
          {syncStatus === 'error' && <RefreshCw size={14} />}
          <span>
            {syncStatus === 'syncing' && 'Sincronizando...'}
            {syncStatus === 'synced' && 'En la nube (Supabase)'}
            {syncStatus === 'offline' && 'Modo Local'}
            {syncStatus === 'error' && 'Reintentar'}
          </span>
        </button>

        {/* Theme Toggle */}
        <button className="icon-button" onClick={onToggleTheme} title="Cambiar tema">
          {theme === 'light' ? <Moon size={18} /> : <Sun size={18} />}
        </button>

        {/* API Credentials / Settings */}
        <button className="icon-button" onClick={onOpenConfig} title="Configurar Supabase">
          <Key size={18} />
        </button>

        {/* User Auth */}
        {user ? (
          <div className="user-profile-menu">
            <span className="user-email" title={user.email}>{user.email?.split('@')[0]}</span>
            <button className="icon-button danger" onClick={onSignOut} title="Cerrar sesión">
              <LogOut size={16} />
            </button>
          </div>
        ) : (
          <button className="btn-secondary" onClick={onOpenAuth}>
            Iniciar Sesión
          </button>
        )}
      </div>
    </header>
  );
};
