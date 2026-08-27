import React, { useState, useMemo } from 'react';
import { QuickReply } from '../types/database';
import { parseWhatsAppMarkdownToHtml, formatLinkMessage } from '../lib/whatsappMarkdown';
import { 
  Star, 
  Copy,
  Check,
  Edit3, 
  Trash2, 
  Link, 
  FolderInput,
  ExternalLink
} from 'lucide-react';

import { DynamicFillModal } from './DynamicFillModal';

interface ReplyCardProps {
  reply: QuickReply;
  onToggleFavorite: (reply: QuickReply) => void;
  onEdit: (reply: QuickReply) => void;
  onDelete: (id: string) => void;
  onMove?: (reply: QuickReply) => void;
}

export const ReplyCard: React.FC<ReplyCardProps> = ({
  reply,
  onToggleFavorite,
  onEdit,
  onDelete,
  onMove,
}) => {
  const [copied, setCopied] = useState(false);
  const [showDynamicModal, setShowDynamicModal] = useState(false);

  const hasDynamicVars = useMemo(() => /\{[^}]+\}/.test(reply.content), [reply.content]);

  const handleCopy = () => {
    if (hasDynamicVars && reply.content_type !== 'SEQUENCE') {
      setShowDynamicModal(true);
      return;
    }

    let textToCopy = reply.content;
    if (reply.content_type === 'LINK') {
      textToCopy = formatLinkMessage(reply.content, reply.title, reply.media_url);
    }
    if (navigator.clipboard && window.isSecureContext) {
      navigator.clipboard.writeText(textToCopy).catch(() => {
        fallbackCopy(textToCopy);
      });
    } else {
      fallbackCopy(textToCopy);
    }
    setCopied(true);
    setTimeout(() => setCopied(false), 1800);
  };

  const fallbackCopy = (text: string) => {
    const el = document.createElement('textarea');
    el.value = text;
    el.setAttribute('readonly', '');
    el.style.position = 'fixed';
    el.style.left = '-9999px';
    document.body.appendChild(el);
    el.select();
    try {
      document.execCommand('copy');
    } catch (_: any) {}
    document.body.removeChild(el);
  };

  const [copiedStepIdx, setCopiedStepIdx] = useState<number | null>(null);

  const handleCopyStep = (stepText: string, idx: number, e: React.MouseEvent) => {
    e.stopPropagation();
    if (navigator.clipboard && window.isSecureContext) {
      navigator.clipboard.writeText(stepText).catch(() => fallbackCopy(stepText));
    } else {
      fallbackCopy(stepText);
    }
    setCopiedStepIdx(idx);
    setTimeout(() => setCopiedStepIdx(null), 1800);
  };

  const isSequence = reply.content_type === 'SEQUENCE';
  const sequenceSteps = isSequence ? reply.content.split('\n---PASO---\n').filter(Boolean) : [];

  return (
    <div 
      className={`reply-card ${reply.is_favorite ? 'favorite' : ''}`}
      onClick={handleCopy}
    >
      <div className="card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px', gap: '8px' }}>
        <h3 className="card-title" style={{ margin: 0, fontSize: '15px', fontWeight: 'bold', color: 'var(--text-main)', flex: 1 }}>
          {reply.title}
        </h3>

        <div className="card-actions" onClick={(e) => e.stopPropagation()}>
          {onMove && (
            <button
              type="button"
              className="icon-btn"
              onClick={() => onMove(reply)}
              title="Mover a otra carpeta / subcarpeta"
            >
              <FolderInput size={15} />
            </button>
          )}
          <button
            type="button"
            className={`icon-btn ${reply.is_favorite ? 'active' : ''}`}
            onClick={() => onToggleFavorite(reply)}
            title={reply.is_favorite ? 'Quitar de favoritos' : 'Marcar como favorito'}
          >
            <Star size={15} />
          </button>
          <button type="button" className="icon-btn" onClick={() => onEdit(reply)} title="Editar mensaje">
            <Edit3 size={15} />
          </button>
          <button
            type="button"
            className="icon-btn danger"
            onClick={() => {
              if (confirm(`¿Eliminar "${reply.title}"? Podrás deshacerlo o recuperarlo en la papelera.`)) {
                onDelete(reply.id);
              }
            }}
            title="Eliminar"
          >
            <Trash2 size={15} />
          </button>
        </div>
      </div>

      {/* Contenido según tipo */}
      {reply.content_type === 'LINK' ? (
        <div className="card-link-container" style={{ margin: '8px 0', padding: '10px', backgroundColor: 'var(--bg-subtle)', borderRadius: '8px', border: '1px solid var(--border)' }}>
          {reply.content && reply.content !== reply.title && !reply.content.startsWith('http') && (
            <div 
              className="card-content-preview" 
              style={{ marginBottom: '8px', fontSize: '12px' }}
              dangerouslySetInnerHTML={{ __html: parseWhatsAppMarkdownToHtml(reply.content) }}
            />
          )}
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '8px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <Link size={14} style={{ color: 'var(--primary)' }} />
              <strong style={{ fontSize: '13px', color: 'var(--primary)' }}>
                {reply.title || 'Enlace'}
              </strong>
            </div>
            {reply.media_url && (
              <a
                href={reply.media_url}
                target="_blank"
                rel="noopener noreferrer"
                className="card-link-preview-box clickable"
                onClick={(e) => e.stopPropagation()}
                title="Abrir enlace en el navegador"
                style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', fontSize: '11px', color: 'var(--primary)', textDecoration: 'underline' }}
              >
                <ExternalLink size={12} />
                <span>Abrir</span>
              </a>
            )}
          </div>
          {reply.media_url && (
            <span style={{ display: 'block', fontSize: '11px', color: 'var(--text-secondary)', marginTop: '4px', wordBreak: 'break-all' }}>
              {reply.media_url}
            </span>
          )}
        </div>
      ) : isSequence ? (
        <div className="sequence-preview" onClick={(e) => e.stopPropagation()}>
          <span className="sequence-tag">Secuencia de {sequenceSteps.length} pasos (toca un paso para copiarlo):</span>
          <div className="sequence-steps-mini">
            {sequenceSteps.map((step, idx) => (
              <div 
                key={idx} 
                className={`step-mini-item clickable ${copiedStepIdx === idx ? 'step-copied' : ''}`}
                onClick={(e) => handleCopyStep(step, idx, e)}
                title="Haz clic para copiar únicamente este paso"
              >
                <span className="step-num">{idx + 1}</span>
                <span className="step-snippet">{step}</span>
                <span className="step-copy-hint">
                  {copiedStepIdx === idx ? '✓ Copiado' : 'Copiar'}
                </span>
              </div>
            ))}
          </div>
        </div>
      ) : (
        <div
          className="card-content-preview"
          dangerouslySetInnerHTML={{ __html: parseWhatsAppMarkdownToHtml(reply.content) }}
        />
      )}

      {reply.content_type !== 'LINK' && reply.media_url && (
        <div className="card-media-tag">
          <span>📎 Archivo adjunto vinculado</span>
        </div>
      )}

      {/* Footer con Copiar y Contador */}
      <div className="card-footer">
        <span className="usage-stat">Usado {reply.usage_count} {reply.usage_count === 1 ? 'vez' : 'veces'}</span>
        <button className={`btn-copy ${copied ? 'copied' : ''}`} onClick={handleCopy}>
          {copied ? (
            <>
              <Check size={14} />
              <span>¡Copiado!</span>
            </>
          ) : (
            <>
              <Copy size={14} />
              <span>{isSequence ? 'Copiar Todo' : 'Copiar'}</span>
            </>
          )}
        </button>
      </div>

      {showDynamicModal && (
        <DynamicFillModal
          reply={reply}
          onClose={() => setShowDynamicModal(false)}
          onCopyComplete={() => {
            setShowDynamicModal(false);
            setCopied(true);
            setTimeout(() => setCopied(false), 1800);
          }}
        />
      )}
    </div>
  );
};
