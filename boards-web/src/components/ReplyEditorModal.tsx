import React, { useState, useRef, useEffect } from 'react';
import { QuickReply, Folder, ContentType } from '../types/database';
import { formatSelection, applyListFormat, parseWhatsAppMarkdownToHtml, formatLinkMessage } from '../lib/whatsappMarkdown';
import { 
  Bold, 
  Italic, 
  Strikethrough, 
  Code, 
  List, 
  ListOrdered, 
  Quote, 
  Eye, 
  X, 
  Save, 
  Layers, 
  Plus, 
  Trash2,
  FileText,
  RotateCcw,
  Image,
  Mic,
  FileSpreadsheet,
  User,
  MapPin,
  Link,
  ExternalLink,
  Type
} from 'lucide-react';

const DRAFT_STORAGE_KEY = 'boards_reply_draft';

interface ReplyEditorModalProps {
  initialReply: Partial<QuickReply> | null;
  folders: Folder[];
  currentFolderId: string;
  onSave: (replyData: {
    id?: string;
    folder_id: string;
    title: string;
    content: string;
    content_type: ContentType;
    media_url?: string | null;
    is_favorite: boolean;
  }) => void;
  onClose: () => void;
}

export const ReplyEditorModal: React.FC<ReplyEditorModalProps> = ({
  initialReply,
  folders,
  currentFolderId,
  onSave,
  onClose,
}) => {
  // Load saved draft if creating a new reply
  const savedDraft = !initialReply?.id ? (() => {
    try {
      const raw = localStorage.getItem(DRAFT_STORAGE_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  })() : null;

  const [hasDraftRestored, setHasDraftRestored] = useState<boolean>(!!savedDraft);

  const [title, setTitle] = useState(
    initialReply?.title || savedDraft?.title || ''
  );
  const [content, setContent] = useState(
    initialReply?.content || savedDraft?.content || ''
  );
  const [folderId, setFolderId] = useState(
    initialReply?.folder_id ||
      savedDraft?.folder_id ||
      (currentFolderId !== 'all' && currentFolderId !== 'favorites' ? currentFolderId : folders[0]?.id || '')
  );
  const [contentType, setContentType] = useState<ContentType>(
    initialReply?.content_type || savedDraft?.content_type || 'TEXT'
  );
  const isFavorite = initialReply?.is_favorite ?? savedDraft?.is_favorite ?? false;
  const [mediaUrl, setMediaUrl] = useState(
    initialReply?.media_url || savedDraft?.media_url || ''
  );

  // Contact specific state
  const [contactName, setContactName] = useState('');
  const [contactPhone, setContactPhone] = useState('');
  const [contactOrg, setContactOrg] = useState('');
  const [contactEmail, setContactEmail] = useState('');

  // Location specific state
  const [locName, setLocName] = useState('');
  const [locAddress, setLocAddress] = useState('');
  const [locMapUrl, setLocMapUrl] = useState('');

  // Document/Audio specific state
  const [docFileName, setDocFileName] = useState('');
  const [previewPlatform, setPreviewPlatform] = useState<'whatsapp' | 'facebook'>('whatsapp');

  // Secuencia
  const [sequenceSteps, setSequenceSteps] = useState<string[]>(() => {
    if (initialReply?.content_type === 'SEQUENCE' && initialReply.content) {
      return initialReply.content.split('\n---PASO---\n').filter(Boolean);
    }
    if (savedDraft?.sequenceSteps && Array.isArray(savedDraft.sequenceSteps)) {
      return savedDraft.sequenceSteps;
    }
    return ['¡Hola {nombre}! Te comparto la propuesta 😊', 'Incluye soporte y actualizaciones.', '¿Agendamos una breve llamada?'];
  });

  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const isMouseDownOnBackdrop = useRef(false);

  // Parse contact or location if editing existing
  useEffect(() => {
    if (initialReply?.content_type === 'CONTACT' && initialReply.content) {
      try {
        const lines = initialReply.content.split('\n');
        lines.forEach(l => {
          if (l.startsWith('👤 Contacto:')) setContactName(l.replace('👤 Contacto:', '').trim());
          if (l.startsWith('📱 Teléfono:')) setContactPhone(l.replace('📱 Teléfono:', '').trim());
          if (l.startsWith('🏢 Empresa:')) setContactOrg(l.replace('🏢 Empresa:', '').trim());
          if (l.startsWith('✉️ Email:')) setContactEmail(l.replace('✉️ Email:', '').trim());
        });
      } catch (_) {}
    } else if (initialReply?.content_type === 'LOCATION' && initialReply.content) {
      try {
        const lines = initialReply.content.split('\n');
        lines.forEach(l => {
          if (l.startsWith('📍 Lugar:')) setLocName(l.replace('📍 Lugar:', '').trim());
          if (l.startsWith('🏠 Dirección:')) setLocAddress(l.replace('🏠 Dirección:', '').trim());
          if (l.startsWith('🗺️ Maps:')) setLocMapUrl(l.replace('🗺️ Maps:', '').trim());
        });
      } catch (_) {}
    } else if (initialReply?.content_type === 'LINK') {
      if (initialReply.media_url && !mediaUrl) {
        setMediaUrl(initialReply.media_url);
      } else if (initialReply.content && (initialReply.content.includes('http://') || initialReply.content.includes('https://'))) {
        const lines = initialReply.content.split('\n');
        const urlLine = lines.find(l => l.trim().startsWith('http://') || l.trim().startsWith('https://')) || '';
        const otherText = lines.filter(l => l !== urlLine).join('\n').trim();
        if (urlLine && !mediaUrl) setMediaUrl(urlLine.trim());
        if (otherText) setContent(otherText);
      }
    }
  }, [initialReply]);

  // Autosave draft continuously if creating a new message
  useEffect(() => {
    if (!initialReply?.id) {
      const hasContent = title.trim() || content.trim() || mediaUrl.trim() || sequenceSteps.some(s => s.trim());
      if (hasContent) {
        localStorage.setItem(
          DRAFT_STORAGE_KEY,
          JSON.stringify({
            title,
            content,
            folder_id: folderId,
            content_type: contentType,
            is_favorite: isFavorite,
            media_url: mediaUrl,
            sequenceSteps,
            contactName,
            contactPhone,
            contactOrg,
            contactEmail,
            locName,
            locAddress,
            locMapUrl,
            updatedAt: Date.now(),
          })
        );
      }
    }
  }, [title, content, folderId, contentType, isFavorite, mediaUrl, sequenceSteps, contactName, contactPhone, contactOrg, contactEmail, locName, locAddress, locMapUrl, initialReply]);

  const clearDraft = () => {
    localStorage.removeItem(DRAFT_STORAGE_KEY);
    setTitle('');
    setContent('');
    setMediaUrl('');
    setSequenceSteps(['', '']);
    setContactName('');
    setContactPhone('');
    setContactOrg('');
    setContactEmail('');
    setLocName('');
    setLocAddress('');
    setLocMapUrl('');
    setHasDraftRestored(false);
  };

  const applyFormat = (syntax: string) => {
    if (!textareaRef.current) return;
    const { selectionStart, selectionEnd } = textareaRef.current;
    const { newText, newStart, newEnd } = formatSelection(content, selectionStart, selectionEnd, syntax);
    setContent(newText);
    setTimeout(() => {
      if (textareaRef.current) {
        textareaRef.current.focus();
        textareaRef.current.setSelectionRange(newStart, newEnd);
      }
    }, 10);
  };

  const applyList = (type: 'bullet' | 'numbered' | 'quote') => {
    if (!textareaRef.current) return;
    const { selectionStart, selectionEnd } = textareaRef.current;
    const { newText, newStart, newEnd } = applyListFormat(content, selectionStart, selectionEnd, type);
    setContent(newText);
    setTimeout(() => {
      if (textareaRef.current) {
        textareaRef.current.focus();
        textareaRef.current.setSelectionRange(newStart, newEnd);
      }
    }, 10);
  };

  const isSavingRef = useRef(false);
  const [isSaving, setIsSaving] = useState(false);
  const [titleError, setTitleError] = useState(false);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isSavingRef.current) return;
    if (!title.trim()) {
      setTitleError(true);
      return;
    }

    isSavingRef.current = true;
    setIsSaving(true);

    let finalContent = content;
    let finalMediaUrl = mediaUrl.trim() || null;

    if (contentType === 'SEQUENCE') {
      finalContent = sequenceSteps.filter((s) => s.trim().length > 0).join('\n---PASO---\n');
    } else if (contentType === 'CONTACT') {
      finalContent = [
        contactName.trim() ? `👤 Contacto: ${contactName.trim()}` : '',
        contactPhone.trim() ? `📱 Teléfono: ${contactPhone.trim()}` : '',
        contactOrg.trim() ? `🏢 Empresa: ${contactOrg.trim()}` : '',
        contactEmail.trim() ? `✉️ Email: ${contactEmail.trim()}` : '',
        content.trim() ? `\n${content.trim()}` : ''
      ].filter(Boolean).join('\n');
    } else if (contentType === 'LOCATION') {
      finalContent = [
        locName.trim() ? `📍 Lugar: ${locName.trim()}` : '',
        locAddress.trim() ? `🏠 Dirección: ${locAddress.trim()}` : '',
        locMapUrl.trim() ? `🗺️ Maps: ${locMapUrl.trim()}` : '',
        content.trim() ? `\n${content.trim()}` : ''
      ].filter(Boolean).join('\n');
    } else if (contentType === 'LINK') {
      let cleanUrl = mediaUrl.trim();
      if (cleanUrl && !cleanUrl.startsWith('http://') && !cleanUrl.startsWith('https://')) {
        cleanUrl = `https://${cleanUrl}`;
      }
      let cleanText = content.trim();
      if (cleanUrl && cleanText.includes(cleanUrl)) {
        cleanText = cleanText.replace(cleanUrl, '').replace(/\n+$/, '').trim();
      }
      finalMediaUrl = cleanUrl || null;
      finalContent = cleanText;
    } else if (contentType === 'PDF' || contentType === 'AUDIO') {
      if (docFileName.trim()) {
        finalContent = content.trim() ? `[${docFileName.trim()}]\n\n${content.trim()}` : `[${docFileName.trim()}]`;
      }
    }

    try {
      await onSave({
        id: initialReply?.id,
        folder_id: folderId || folders[0]?.id || '',
        title: title.trim(),
        content: finalContent,
        content_type: contentType,
        media_url: finalMediaUrl,
        is_favorite: isFavorite,
      });

      // Clear draft upon successful save
      localStorage.removeItem(DRAFT_STORAGE_KEY);
    } catch (err: any) {
      console.error("Save error in modal:", err);
    } finally {
      isSavingRef.current = false;
      setIsSaving(false);
    }
  };

  const handleSafeClose = () => {
    onClose();
  };

  return (
    <div 
      className="modal-backdrop" 
      onMouseDown={(e) => {
        isMouseDownOnBackdrop.current = (e.target === e.currentTarget);
      }}
      onMouseUp={(e) => {
        if (isMouseDownOnBackdrop.current && e.target === e.currentTarget) {
          handleSafeClose();
        }
        isMouseDownOnBackdrop.current = false;
      }}
    >
      <div className="modal-card large" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div>
            <h2>{initialReply?.id ? 'Editar Mensaje Rápido' : 'Nuevo Mensaje Rápido'}</h2>
            <p>Escribe tu plantilla con formato de WhatsApp, archivos, ubicación o contactos</p>
          </div>
          <button className="icon-button" onClick={handleSafeClose} title="Cerrar ventana"><X size={20} /></button>
        </div>

        {hasDraftRestored && !initialReply?.id && (
          <div className="draft-alert-banner">
            <div className="draft-alert-left">
              <FileText size={16} />
              <span>Borrador recuperado automáticamente. Tu texto no se perdió.</span>
            </div>
            <button type="button" className="btn-draft-clear" onClick={clearDraft}>
              <RotateCcw size={13} /> <span>Empezar de cero</span>
            </button>
          </div>
        )}

        <form onSubmit={handleSave} className="editor-form-layout">
          {/* Columna Izquierda: Formulario y Editor */}
          <div className="editor-fields-col">
            <div className="form-row">
              <div className="form-group flex-2">
                <label>
                  Título o Atajo
                  {titleError && <span style={{ color: '#EF4444', fontSize: '0.75rem' }}> * Requerido</span>}
                </label>
                <input
                  type="text"
                  placeholder="Ej: 2° agendamiento, Saludo inicial, Catálogo 2026..."
                  value={title}
                  maxLength={120}
                  onChange={(e) => {
                    setTitle(e.target.value);
                    if (e.target.value.trim()) setTitleError(false);
                  }}
                  style={titleError ? { borderColor: '#EF4444', backgroundColor: '#FEF2F2' } : {}}
                  autoFocus
                  required
                />
              </div>

              <div className="form-group flex-1">
                <label>Tablero / Carpeta</label>
                <select value={folderId} onChange={(e) => setFolderId(e.target.value)}>
                  {folders.filter((f) => !f.parent_id).map((parent) => {
                    const subs = folders.filter((f) => f.parent_id === parent.id);
                    return (
                      <React.Fragment key={parent.id}>
                        <option value={parent.id}>📁 {parent.name}</option>
                        {subs.map((sub) => (
                          <option key={sub.id} value={sub.id}>
                            &nbsp;&nbsp;&nbsp;↳ 📂 {sub.name}
                          </option>
                        ))}
                      </React.Fragment>
                    );
                  })}
                </select>
              </div>
            </div>

            {/* Selector de Tipo de Contenido Completo */}
            <div className="content-type-selector-grid">
              <button
                type="button"
                className={`type-chip ${contentType === 'TEXT' ? 'active' : ''}`}
                onClick={() => setContentType('TEXT')}
              >
                <Type size={14} /> <span>Texto</span>
              </button>

              <button
                type="button"
                className={`type-chip ${contentType === 'IMAGE' ? 'active' : ''}`}
                onClick={() => setContentType('IMAGE')}
              >
                <Image size={14} /> <span>Imagen</span>
              </button>

              <button
                type="button"
                className={`type-chip ${contentType === 'AUDIO' ? 'active' : ''}`}
                onClick={() => setContentType('AUDIO')}
              >
                <Mic size={14} /> <span>Audio</span>
              </button>

              <button
                type="button"
                className={`type-chip ${contentType === 'PDF' ? 'active' : ''}`}
                onClick={() => setContentType('PDF')}
              >
                <FileSpreadsheet size={14} /> <span>Documento</span>
              </button>

              <button
                type="button"
                className={`type-chip ${contentType === 'CONTACT' ? 'active' : ''}`}
                onClick={() => setContentType('CONTACT')}
              >
                <User size={14} /> <span>Contacto</span>
              </button>

              <button
                type="button"
                className={`type-chip ${contentType === 'LOCATION' ? 'active' : ''}`}
                onClick={() => setContentType('LOCATION')}
              >
                <MapPin size={14} /> <span>Ubicación</span>
              </button>

              <button
                type="button"
                className={`type-chip ${contentType === 'LINK' ? 'active' : ''}`}
                onClick={() => setContentType('LINK')}
              >
                <Link size={14} /> <span>Enlace</span>
              </button>

              <button
                type="button"
                className={`type-chip ${contentType === 'SEQUENCE' ? 'active' : ''}`}
                onClick={() => setContentType('SEQUENCE')}
              >
                <Layers size={14} /> <span>Secuencia</span>
              </button>
            </div>

            {/* CAMPOS ESPECÍFICOS SEGÚN EL TIPO SELECCIONADO */}
            {contentType === 'IMAGE' && (
              <div className="form-group media-field-box">
                <label>URL o Enlace de la Imagen</label>
                <input
                  type="url"
                  placeholder="https://ejemplo.com/catalogo.jpg"
                  value={mediaUrl}
                  onChange={(e) => setMediaUrl(e.target.value)}
                />
              </div>
            )}

            {contentType === 'AUDIO' && (
              <div className="form-row">
                <div className="form-group flex-2">
                  <label>URL de la Nota de Voz / Audio</label>
                  <input
                    type="url"
                    placeholder="https://ejemplo.com/audio-bienvenida.mp3"
                    value={mediaUrl}
                    onChange={(e) => setMediaUrl(e.target.value)}
                  />
                </div>
                <div className="form-group flex-1">
                  <label>Nombre / Duración</label>
                  <input
                    type="text"
                    placeholder="Ej: Audio de Bienvenida (0:45)"
                    value={docFileName}
                    onChange={(e) => setDocFileName(e.target.value)}
                  />
                </div>
              </div>
            )}

            {contentType === 'PDF' && (
              <div className="form-row">
                <div className="form-group flex-2">
                  <label>URL del Documento / PDF</label>
                  <input
                    type="url"
                    placeholder="https://ejemplo.com/catalogo_precios.pdf"
                    value={mediaUrl}
                    onChange={(e) => setMediaUrl(e.target.value)}
                  />
                </div>
                <div className="form-group flex-1">
                  <label>Nombre del Archivo</label>
                  <input
                    type="text"
                    placeholder="Ej: Catalogo_2026.pdf"
                    value={docFileName}
                    onChange={(e) => setDocFileName(e.target.value)}
                  />
                </div>
              </div>
            )}

            {contentType === 'LINK' && (
              <div className="link-fields-box">
                <div className="form-group media-field-box">
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                    <label style={{ margin: 0 }}>URL de Destino Real (Web / Enlace)</label>
                    {mediaUrl && (
                      <a
                        href={mediaUrl.startsWith('http') ? mediaUrl : `https://${mediaUrl}`}
                        target="_blank"
                        rel="noreferrer"
                        style={{ fontSize: '11px', color: 'var(--primary)', display: 'inline-flex', alignItems: 'center', gap: '3px', textDecoration: 'underline' }}
                        onClick={(e) => e.stopPropagation()}
                      >
                        <ExternalLink size={11} /> Probar enlace
                      </a>
                    )}
                  </div>
                  <input
                    type="url"
                    placeholder="https://calendar.app.google/... o tusitio.com"
                    value={mediaUrl}
                    onChange={(e) => setMediaUrl(e.target.value)}
                    required
                  />
                  <small className="form-help" style={{ fontSize: '11px', color: 'var(--text-secondary)', marginTop: '4px', display: 'block' }}>
                    Dirección web de destino. Se validará automáticamente con https://.
                  </small>
                </div>
              </div>
            )}

            {contentType === 'CONTACT' && (
              <div className="contact-fields-box">
                <div className="form-row">
                  <div className="form-group flex-1">
                    <label>Nombre del Contacto</label>
                    <input
                      type="text"
                      placeholder="Ej: Juan Pérez (Ventas)"
                      value={contactName}
                      onChange={(e) => setContactName(e.target.value)}
                    />
                  </div>
                  <div className="form-group flex-1">
                    <label>Teléfono / WhatsApp</label>
                    <input
                      type="tel"
                      placeholder="Ej: +56 9 1234 5678"
                      value={contactPhone}
                      onChange={(e) => setContactPhone(e.target.value)}
                    />
                  </div>
                </div>
                <div className="form-row">
                  <div className="form-group flex-1">
                    <label>Empresa / Organización (Opcional)</label>
                    <input
                      type="text"
                      placeholder="Ej: Mi Empresa SpA"
                      value={contactOrg}
                      onChange={(e) => setContactOrg(e.target.value)}
                    />
                  </div>
                  <div className="form-group flex-1">
                    <label>Correo Electrónico (Opcional)</label>
                    <input
                      type="email"
                      placeholder="contacto@empresa.com"
                      value={contactEmail}
                      onChange={(e) => setContactEmail(e.target.value)}
                    />
                  </div>
                </div>
              </div>
            )}

            {contentType === 'LOCATION' && (
              <div className="contact-fields-box">
                <div className="form-row">
                  <div className="form-group flex-1">
                    <label>Nombre del Lugar / Sucursal</label>
                    <input
                      type="text"
                      placeholder="Ej: Oficina Central Santiago"
                      value={locName}
                      onChange={(e) => setLocName(e.target.value)}
                    />
                  </div>
                  <div className="form-group flex-1">
                    <label>Dirección</label>
                    <input
                      type="text"
                      placeholder="Ej: Av. Providencia 1234, Of 502"
                      value={locAddress}
                      onChange={(e) => setLocAddress(e.target.value)}
                    />
                  </div>
                </div>
                <div className="form-group">
                  <label>Enlace de Google Maps / Coordenadas</label>
                  <input
                    type="url"
                    placeholder="https://maps.google.com/?q=-33.425,-70.612"
                    value={locMapUrl}
                    onChange={(e) => setLocMapUrl(e.target.value)}
                  />
                </div>
              </div>
            )}

            {contentType === 'SEQUENCE' ? (
              /* Creador de Secuencias */
              <div className="sequence-builder">
                <div className="sequence-builder-header">
                  <label>Pasos de la Secuencia (Se enviarán uno por uno)</label>
                  <button
                    type="button"
                    className="btn-text-action"
                    onClick={() => setSequenceSteps([...sequenceSteps, ''])}
                  >
                    <Plus size={14} /> <span>Agregar Paso</span>
                  </button>
                </div>

                <div className="sequence-step-inputs">
                  {sequenceSteps.map((step, idx) => (
                    <div key={idx} className="step-input-row">
                      <div className="step-badge-circle">{idx + 1}</div>
                      <textarea
                        rows={2}
                        value={step}
                        placeholder={`Mensaje del paso ${idx + 1}...`}
                        onChange={(e) => {
                          const updated = [...sequenceSteps];
                          updated[idx] = e.target.value;
                          setSequenceSteps(updated);
                        }}
                      />
                      {sequenceSteps.length > 1 && (
                        <button
                          type="button"
                          className="icon-mini-btn danger"
                          onClick={() => {
                            setSequenceSteps(sequenceSteps.filter((_, i) => i !== idx));
                          }}
                        >
                          <Trash2 size={16} />
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            ) : (
              /* Editor de Texto con Barra WhatsApp */
              <div className="form-group">
                <div className="toolbar-header">
                  <label>
                    {contentType === 'IMAGE' ? 'Pie de Foto / Mensaje Acompañante' :
                     contentType === 'PDF' ? 'Mensaje o Instrucciones del Documento' :
                     contentType === 'AUDIO' ? 'Nota o Transcripción del Audio' :
                     contentType === 'CONTACT' ? 'Nota adicional del contacto' :
                     contentType === 'LOCATION' ? 'Indicaciones de llegada' :
                     contentType === 'LINK' ? 'Texto de acompañamiento (Opcional)' :
                     'Contenido del Mensaje'}
                  </label>
                  <div className="whatsapp-toolbar">
                    <button type="button" onClick={() => applyFormat('*')} title="Negrita (*texto*)">
                      <Bold size={15} />
                    </button>
                    <button type="button" onClick={() => applyFormat('_')} title="Cursiva (_texto_)">
                      <Italic size={15} />
                    </button>
                    <button type="button" onClick={() => applyFormat('~')} title="Tachado (~texto~)">
                      <Strikethrough size={15} />
                    </button>
                    <button type="button" onClick={() => applyFormat('```')} title="Monoespaciado">
                      <Code size={15} />
                    </button>
                    <div className="toolbar-divider" />
                    <button type="button" onClick={() => applyList('bullet')} title="Lista con viñetas (• )">
                      <List size={15} />
                    </button>
                    <button type="button" onClick={() => applyList('numbered')} title="Lista numerada (1. )">
                      <ListOrdered size={15} />
                    </button>
                    <button type="button" onClick={() => applyList('quote')} title="Cita (> )">
                      <Quote size={15} />
                    </button>
                  </div>
                </div>

                <textarea
                  ref={textareaRef}
                  rows={contentType === 'TEXT' ? 8 : 4}
                  maxLength={5000}
                  placeholder={
                    contentType === 'IMAGE' ? 'Escribe el mensaje que acompañará a la foto...' :
                    contentType === 'CONTACT' ? 'Ej: "Hola, te paso el contacto directo de nuestro asesor de ventas..."' :
                    contentType === 'LINK' ? 'Ej: Hola {nombre}, te envío el enlace para agendar:' :
                    'Escribe tu mensaje con negrita *hola*, variables {nombre} y listas...'
                  }
                  value={content}
                  onChange={(e) => setContent(e.target.value)}
                />
              </div>
            )}
          </div>

          {/* Columna Derecha: Vista Previa Real WhatsApp / Facebook */}
          <div className="editor-preview-col">
            <div className="preview-header">
              <div className="preview-header-title">
                <Eye size={15} />
                <span>Vista Previa Real</span>
              </div>
              <div className="platform-preview-tabs">
                <button
                  type="button"
                  className={`platform-tab-btn ${previewPlatform === 'whatsapp' ? 'active whatsapp' : ''}`}
                  onClick={() => setPreviewPlatform('whatsapp')}
                >
                  🟢 WhatsApp
                </button>
                <button
                  type="button"
                  className={`platform-tab-btn ${previewPlatform === 'facebook' ? 'active facebook' : ''}`}
                  onClick={() => setPreviewPlatform('facebook')}
                >
                  🔵 Facebook
                </button>
              </div>
            </div>

            <div className={`wa-phone-mockup ${previewPlatform}`}>
              <div className="wa-chat-header">
                <div className="wa-avatar">
                  {previewPlatform === 'whatsapp' ? '🟢' : '👤'}
                </div>
                <div className="wa-chat-info">
                  <strong>{previewPlatform === 'whatsapp' ? 'Cliente WhatsApp' : 'Cliente Facebook'}</strong>
                  <span>{previewPlatform === 'whatsapp' ? 'en línea' : 'Activo(a) ahora'}</span>
                </div>
              </div>

              <div className="wa-chat-body">
                <div className="wa-bubble incoming">
                  ¡Hola! ¿Me podrían enviar la información solicitada por favor?
                  <span className="wa-time">10:44 AM</span>
                </div>

                <div className="wa-bubble outgoing">
                  {contentType === 'IMAGE' && (
                    <div className="wa-media-preview-box">
                      {mediaUrl ? (
                        <img src={mediaUrl} alt="Preview" className="wa-bubble-img" />
                      ) : (
                        <div className="wa-media-placeholder">
                          <Image size={32} />
                          <span>Vista previa de imagen</span>
                        </div>
                      )}
                    </div>
                  )}

                  {contentType === 'AUDIO' && (
                    <div className="wa-audio-preview-bubble">
                      <div className="wa-audio-icon-mic">
                        <Mic size={18} />
                      </div>
                      <div className="wa-audio-track">
                        <div className="wa-audio-bar" />
                        <span className="wa-audio-duration">{docFileName || '0:45'}</span>
                      </div>
                    </div>
                  )}

                  {contentType === 'PDF' && (
                    <div className="wa-doc-preview-bubble">
                      <div className="wa-doc-icon-badge">
                        <FileSpreadsheet size={20} />
                      </div>
                      <div className="wa-doc-details">
                        <strong>{docFileName || 'Documento_Adjunto.pdf'}</strong>
                        <span>1.4 MB • Documento</span>
                      </div>
                    </div>
                  )}

                  {contentType === 'CONTACT' && (
                    <div className="wa-contact-preview-bubble">
                      <div className="wa-contact-avatar">
                        <User size={24} />
                      </div>
                      <div className="wa-contact-info">
                        <strong>{contactName || 'Nombre del Contacto'}</strong>
                        <span>{contactPhone || '+56 9 1234 5678'}</span>
                        {contactOrg && <span className="wa-contact-org">{contactOrg}</span>}
                      </div>
                    </div>
                  )}

                  {contentType === 'LOCATION' && (
                    <div className="wa-location-preview-bubble">
                      <div className="wa-location-map-mock">
                        <MapPin size={28} className="map-pin-pulse" />
                      </div>
                      <div className="wa-location-text">
                        <strong>{locName || 'Ubicación'}</strong>
                        <span>{locAddress || 'Dirección no especificada'}</span>
                      </div>
                    </div>
                  )}

                  {contentType === 'LINK' ? (
                    <div className="wa-link-preview-bubble">
                      <div
                        className="wa-message-text"
                        dangerouslySetInnerHTML={{
                          __html: parseWhatsAppMarkdownToHtml(
                            formatLinkMessage(content, title, mediaUrl) || 'Escribe la URL de destino para previsualizar...'
                          ),
                        }}
                      />
                      {mediaUrl && (
                        <div className="wa-link-card" style={{ marginTop: '8px' }}>
                          <div className="wa-link-header">
                            <Link size={13} />
                            <span className="wa-link-domain">
                              {(() => {
                                try {
                                  return new URL(mediaUrl.startsWith('http') ? mediaUrl : `https://${mediaUrl}`).hostname;
                                } catch (_) {
                                  return 'enlace.web';
                                }
                              })()}
                            </span>
                          </div>
                        </div>
                      )}
                      <span className="wa-time">10:45 AM ✓✓</span>
                    </div>
                  ) : contentType === 'SEQUENCE' ? (
                    <div className="sequence-preview-list">
                      {sequenceSteps.filter(s => s.trim().length > 0).map((step, idx) => (
                        <div key={idx} className="sequence-bubble-item">
                          <div className="step-tag-mini">Paso {idx + 1}</div>
                          <div
                            className="wa-message-text"
                            dangerouslySetInnerHTML={{
                              __html: parseWhatsAppMarkdownToHtml(step),
                            }}
                          />
                        </div>
                      ))}
                    </div>
                  ) : (
                    <>
                      {content && (
                        <div
                          className="wa-message-text"
                          dangerouslySetInnerHTML={{
                            __html: parseWhatsAppMarkdownToHtml(content || 'Escribe tu mensaje para previsualizarlo aquí con formato real...'),
                          }}
                        />
                      )}
                      <span className="wa-time">10:45 AM ✓✓</span>
                    </>
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* Footer Acciones */}
          <div className="modal-footer-full">
            <button type="button" className="btn-secondary" onClick={handleSafeClose}>
              Cancelar
            </button>
            <button type="submit" className="btn-primary" disabled={isSaving}>
              <Save size={16} />
              <span>{isSaving ? 'Guardando y Sincronizando...' : 'Guardar y Sincronizar'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
