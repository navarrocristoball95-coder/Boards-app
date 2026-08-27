import React, { useState } from 'react';
import { updateSupabaseCredentials, getStoredCredentials } from '../lib/supabaseClient';
import { Key, Database, Check, AlertCircle, X } from 'lucide-react';

interface ConfigModalProps {
  onClose: () => void;
  onSaved: () => void;
}

export const ConfigModal: React.FC<ConfigModalProps> = ({ onClose, onSaved }) => {
  const current = getStoredCredentials();
  const [url, setUrl] = useState(current.url);
  const [anonKey, setAnonKey] = useState(current.anonKey);
  const [statusMsg, setStatusMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    if (!url.trim() || !anonKey.trim()) {
      setStatusMsg({ type: 'error', text: 'Ingresa la URL y la Anon Key de tu proyecto Supabase.' });
      return;
    }

    updateSupabaseCredentials(url.trim(), anonKey.trim());
    setStatusMsg({ type: 'success', text: '¡Credenciales guardadas con éxito!' });
    setTimeout(() => {
      onSaved();
      onClose();
    }, 800);
  };

  const isMouseDownOnBackdrop = React.useRef(false);

  return (
    <div 
      className="modal-backdrop" 
      onMouseDown={(e) => {
        isMouseDownOnBackdrop.current = (e.target === e.currentTarget);
      }}
      onMouseUp={(e) => {
        if (isMouseDownOnBackdrop.current && e.target === e.currentTarget) {
          onClose();
        }
        isMouseDownOnBackdrop.current = false;
      }}
    >
      <div className="modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-icon-badge">
            <Database size={20} />
          </div>
          <div>
            <h3>Conexión con Supabase</h3>
            <p>Configura las credenciales de tu base de datos en la nube</p>
          </div>
          <button className="icon-button" onClick={onClose}><X size={18} /></button>
        </div>

        <form onSubmit={handleSave}>
          <div className="form-group">
            <label>Supabase Project URL</label>
            <input
              type="url"
              placeholder="https://nbtzhmsyvjjgtkfupsby.supabase.co"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label>Supabase Anon / Public Key</label>
            <textarea
              rows={3}
              placeholder="Pega aquí tu clave anon public de Supabase..."
              value={anonKey}
              onChange={(e) => setAnonKey(e.target.value)}
              required
              className="mono-input"
            />
            <span className="help-text">
              La encuentras en tu panel de Supabase: <strong>Project Settings → API → Project API keys → anon public</strong>.
            </span>
          </div>

          {statusMsg && (
            <div className={`status-banner ${statusMsg.type}`}>
              {statusMsg.type === 'success' ? <Check size={16} /> : <AlertCircle size={16} />}
              <span>{statusMsg.text}</span>
            </div>
          )}

          <div className="modal-actions">
            <button type="button" className="btn-secondary" onClick={onClose}>
              Cerrar
            </button>
            <button type="submit" className="btn-primary">
              <Key size={16} />
              <span>Guardar Credenciales</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
