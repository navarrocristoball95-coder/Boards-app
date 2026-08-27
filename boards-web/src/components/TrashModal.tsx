import React from 'react';
import { QuickReply, Folder } from '../types/database';
import { RotateCcw, Trash2, X, AlertTriangle, Inbox } from 'lucide-react';

interface TrashModalProps {
  deletedReplies: QuickReply[];
  folders: Folder[];
  onRestore: (id: string) => void;
  onRestoreAll: () => void;
  onPermanentDelete: (id: string) => void;
  onEmptyTrash: () => void;
  onClose: () => void;
}

export const TrashModal: React.FC<TrashModalProps> = ({
  deletedReplies,
  folders,
  onRestore,
  onRestoreAll,
  onPermanentDelete,
  onEmptyTrash,
  onClose,
}) => {
  const getFolderName = (folderId: string) => {
    const f = folders.find((item) => item.id === folderId);
    return f ? f.name : 'Tablero General';
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-card large" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div className="flex items-center gap-3">
            <div className="trash-header-icon">
              <Trash2 size={22} />
            </div>
            <div>
              <h2>Papelera de Reciclaje</h2>
              <p>Recupera mensajes eliminados o bórralos permanentemente ({deletedReplies.length} en papelera)</p>
            </div>
          </div>
          <button className="icon-button" onClick={onClose} title="Cerrar"><X size={20} /></button>
        </div>

        {deletedReplies.length > 0 && (
          <div className="trash-toolbar">
            <button className="btn-secondary" onClick={onRestoreAll}>
              <RotateCcw size={15} />
              <span>Restaurar Todo</span>
            </button>
            <button
              className="btn-danger"
              onClick={() => {
                if (confirm('¿Estás seguro de vaciar la papelera? Esta acción no se puede deshacer.')) {
                  onEmptyTrash();
                }
              }}
            >
              <Trash2 size={15} />
              <span>Vaciar Papelera</span>
            </button>
          </div>
        )}

        <div className="trash-list-container">
          {deletedReplies.length === 0 ? (
            <div className="empty-state">
              <div className="empty-icon-circle">
                <Inbox size={32} />
              </div>
              <h3>La papelera está vacía</h3>
              <p>Los mensajes que elimines aparecerán aquí para que puedas restaurarlos cuando los necesites.</p>
            </div>
          ) : (
            <div className="trash-items-grid">
              {deletedReplies.map((reply) => (
                <div key={reply.id} className="trash-item-card">
                  <div className="trash-card-header">
                    <div className="trash-card-info">
                      <strong>{reply.title}</strong>
                      <span className="trash-folder-tag">📁 {getFolderName(reply.folder_id)}</span>
                    </div>
                    <div className="trash-card-actions">
                      <button
                        className="btn-restore-mini"
                        onClick={() => onRestore(reply.id)}
                        title="Restaurar este mensaje"
                      >
                        <RotateCcw size={14} />
                        <span>Restaurar</span>
                      </button>
                      <button
                        className="icon-mini-btn danger"
                        onClick={() => {
                          if (confirm(`¿Eliminar definitivamente "${reply.title}"?`)) {
                            onPermanentDelete(reply.id);
                          }
                        }}
                        title="Eliminar permanentemente"
                      >
                        <Trash2 size={14} />
                      </button>
                    </div>
                  </div>
                  <p className="trash-card-snippet">
                    {reply.content.length > 120 ? `${reply.content.substring(0, 120)}...` : reply.content}
                  </p>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="modal-footer-full">
          <div className="trash-footer-hint">
            <AlertTriangle size={14} />
            <span>Los mensajes en la papelera no se envían en el teclado móvil hasta que sean restaurados.</span>
          </div>
          <button type="button" className="btn-secondary" onClick={onClose}>
            Cerrar
          </button>
        </div>
      </div>
    </div>
  );
};
