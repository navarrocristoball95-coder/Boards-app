import React, { useState } from 'react';
import { supabase } from '../lib/supabaseClient';
import { LogIn, UserPlus, Mail, Lock, AlertCircle, CheckCircle2, X, ArrowLeft } from 'lucide-react';

export type AuthMode = 'login' | 'signup' | 'forgot-password' | 'update-password';

interface AuthModalProps {
  initialMode?: AuthMode;
  onClose: () => void;
  onSuccess: () => void;
}

export const AuthModal: React.FC<AuthModalProps> = ({ initialMode = 'login', onClose, onSuccess }) => {
  const [mode, setMode] = useState<AuthMode>(initialMode);

  React.useEffect(() => {
    setMode(initialMode);
    setError(null);
    setMessage(null);
  }, [initialMode]);

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setMessage(null);

    try {
      if (mode === 'signup') {
        const { error } = await supabase.auth.signUp({
          email: email.trim(),
          password: password.trim(),
        });
        if (error) throw error;
        setMessage('¡Cuenta creada con éxito! Revisa tu correo para confirmar o inicia sesión.');
        setTimeout(() => {
          onSuccess();
          onClose();
        }, 1500);
      } else if (mode === 'login') {
        const { error } = await supabase.auth.signInWithPassword({
          email: email.trim(),
          password: password.trim(),
        });
        if (error) throw error;
        setMessage('¡Sesión iniciada correctamente!');
        setTimeout(() => {
          onSuccess();
          onClose();
        }, 600);
      } else if (mode === 'forgot-password') {
        if (!email.trim()) {
          throw new Error('Por favor ingresa tu correo electrónico');
        }
        const redirectUrl = window.location.origin.includes('localhost')
          ? 'https://boards-web.vercel.app'
          : `${window.location.origin}`;

        const { error } = await supabase.auth.resetPasswordForEmail(email.trim(), {
          redirectTo: redirectUrl,
        });
        if (error) throw error;
        setMessage('¡Enlace de recuperación enviado! Revisa tu correo (incluida la carpeta de spam).');
      } else if (mode === 'update-password') {
        if (password.length < 6) {
          throw new Error('La contraseña debe tener al menos 6 caracteres');
        }
        if (password !== confirmPassword) {
          throw new Error('Las contraseñas no coinciden');
        }
        const { error } = await supabase.auth.updateUser({
          password: password.trim(),
        });
        if (error) throw error;
        setMessage('¡Contraseña actualizada con éxito! Accediendo a tu cuenta...');
        setTimeout(() => {
          onSuccess();
          onClose();
        }, 1200);
      }
    } catch (err: any) {
      setError(err.message || 'Ocurrió un error con la autenticación');
    } finally {
      setLoading(false);
    }
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
      <div className="modal-card small" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div>
            <h3>
              {mode === 'signup' && 'Crear Cuenta'}
              {mode === 'login' && 'Iniciar Sesión'}
              {mode === 'forgot-password' && 'Recuperar Contraseña'}
              {mode === 'update-password' && 'Nueva Contraseña'}
            </h3>
            <p>
              {mode === 'forgot-password' && 'Te enviaremos un enlace para restablecer tu clave'}
              {mode === 'update-password' && 'Ingresa tu nueva contraseña para acceder'}
              {(mode === 'login' || mode === 'signup') && 'Sincroniza tus tableros entre tu PC y tu teléfono'}
            </p>
          </div>
          <button className="icon-button" onClick={onClose}><X size={18} /></button>
        </div>

        {(mode === 'login' || mode === 'signup') && (
          <div className="auth-tab-toggle">
            <button
              type="button"
              className={mode === 'login' ? 'active' : ''}
              onClick={() => { setMode('login'); setError(null); setMessage(null); }}
            >
              <LogIn size={15} /> Iniciar Sesión
            </button>
            <button
              type="button"
              className={mode === 'signup' ? 'active' : ''}
              onClick={() => { setMode('signup'); setError(null); setMessage(null); }}
            >
              <UserPlus size={15} /> Registrarse
            </button>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          {mode !== 'update-password' && (
            <div className="form-group">
              <label>Correo Electrónico</label>
              <div className="input-with-icon">
                <Mail size={16} />
                <input
                  type="email"
                  placeholder="tu@correo.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  autoFocus
                />
              </div>
            </div>
          )}

          {mode !== 'forgot-password' && (
            <div className="form-group">
              <div className="label-with-action">
                <label>{mode === 'update-password' ? 'Nueva Contraseña' : 'Contraseña'}</label>
                {mode === 'login' && (
                  <button
                    type="button"
                    className="auth-link-subtle"
                    onClick={() => { setMode('forgot-password'); setError(null); setMessage(null); }}
                  >
                    ¿Olvidaste tu contraseña?
                  </button>
                )}
              </div>
              <div className="input-with-icon">
                <Lock size={16} />
                <input
                  type="password"
                  placeholder="Mínimo 6 caracteres"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  minLength={6}
                />
              </div>
            </div>
          )}

          {mode === 'update-password' && (
            <div className="form-group">
              <label>Confirmar Nueva Contraseña</label>
              <div className="input-with-icon">
                <Lock size={16} />
                <input
                  type="password"
                  placeholder="Repite la nueva contraseña"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                  minLength={6}
                />
              </div>
            </div>
          )}

          {error && (
            <div className="status-banner error">
              <AlertCircle size={16} />
              <span>{error}</span>
            </div>
          )}

          {message && (
            <div className="status-banner success">
              <CheckCircle2 size={16} />
              <span>{message}</span>
            </div>
          )}

          <div className="modal-actions full">
            <button type="submit" className="btn-primary full" disabled={loading}>
              {loading ? 'Procesando...' : 
                mode === 'signup' ? 'Registrarme' : 
                mode === 'forgot-password' ? 'Enviar Enlace de Recuperación' : 
                mode === 'update-password' ? 'Guardar Nueva Contraseña' : 
                'Entrar'
              }
            </button>
          </div>

          {mode === 'forgot-password' && (
            <div className="auth-back-container">
              <button
                type="button"
                className="auth-link-back"
                onClick={() => { setMode('login'); setError(null); setMessage(null); }}
              >
                <ArrowLeft size={14} /> Volver a Iniciar Sesión
              </button>
            </div>
          )}
        </form>
      </div>
    </div>
  );
};
