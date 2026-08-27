import React, { useState } from 'react';
import { Folder, QuickReply } from '../types/database';
import { Folder as FolderIcon, Check, X, Search, MoveRight } from 'lucide-react';

interface MoveReplyModalProps {
  reply: QuickReply;
  folders: Folder[];
  currentFolderId: string | null;
  onClose: () => void;
  onMove: (replyId: string, targetFolderId: string) => void;
}

export const MoveReplyModal: React.FC<MoveReplyModalProps> = ({
  reply,
  folders,
  currentFolderId,
  onClose,
  onMove,
}) => {
  const [searchTerm, setSearchTerm] = useState('');

  const rootFolders = folders.filter((f) => !f.parent_id);
  const getSubfolders = (parentId: string) => folders.filter((f) => f.parent_id === parentId);

  const filteredFolders = searchTerm.trim()
    ? folders.filter((f) => f.name.toLowerCase().includes(searchTerm.toLowerCase()))
    : null;

  const handleSelectFolder = (targetFolderId: string) => {
    if (targetFolderId === currentFolderId) return;
    onMove(reply.id, targetFolderId);
    onClose();
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-card small" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div>
            <h3>Mover Respuesta Rápida</h3>
            <p className="modal-subtitle">
              Mover <strong>"{reply.title}"</strong> a otro tablero o subcarpeta
            </p>
          </div>
          <button className="icon-mini-btn" onClick={onClose}>
            <X size={18} />
          </button>
        </div>

        <div className="search-container" style={{ margin: '12px 0' }}>
          <Search size={15} className="search-icon" />
          <input
            type="text"
            placeholder="Filtrar tableros o subcarpetas..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="search-input"
            autoFocus
          />
        </div>

        <div className="folders-move-list" style={{ maxHeight: '280px', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '6px' }}>
          {filteredFolders ? (
            filteredFolders.length === 0 ? (
              <p style={{ textAlign: 'center', color: 'var(--text-secondary)', padding: '20px' }}>No se encontraron coincidencias.</p>
            ) : (
              filteredFolders.map((folder) => {
                const isCurrent = folder.id === currentFolderId;
                const isSub = !!folder.parent_id;
                const parentName = isSub ? folders.find((f) => f.id === folder.parent_id)?.name : null;

                return (
                  <button
                    key={folder.id}
                    className={`move-folder-option ${isCurrent ? 'current' : ''}`}
                    onClick={() => handleSelectFolder(folder.id)}
                    disabled={isCurrent}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '10px',
                      padding: '10px 12px',
                      borderRadius: '8px',
                      border: isCurrent ? '1px solid var(--border-color)' : '1px solid var(--border-color)',
                      backgroundColor: isCurrent ? 'var(--bg-subtle)' : 'var(--card-bg)',
                      cursor: isCurrent ? 'default' : 'pointer',
                      textAlign: 'left',
                      width: '100%',
                    }}
                  >
                    <div
                      style={{
                        width: '26px',
                        height: '26px',
                        borderRadius: '6px',
                        backgroundColor: `${folder.color || '#4361EE'}20`,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: folder.color || '#4361EE',
                        flexShrink: 0,
                      }}
                    >
                      <FolderIcon size={14} />
                    </div>
                    <div style={{ flexGrow: 1, minWidth: 0 }}>
                      <div style={{ fontWeight: 600, fontSize: '13px', color: isCurrent ? 'var(--text-secondary)' : 'var(--text-primary)' }}>
                        {folder.name}
                      </div>
                      {parentName && (
                        <div style={{ fontSize: '11px', color: 'var(--text-secondary)' }}>
                          En tablero: {parentName}
                        </div>
                      )}
                    </div>
                    {isCurrent ? (
                      <span style={{ fontSize: '11px', color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: '4px' }}>
                        <Check size={13} /> Actual
                      </span>
                    ) : (
                      <MoveRight size={14} style={{ color: 'var(--primary)', opacity: 0.7 }} />
                    )}
                  </button>
                );
              })
            )
          ) : (
            rootFolders.map((root) => {
              const subfolders = getSubfolders(root.id);
              const isRootCurrent = root.id === currentFolderId;

              return (
                <div key={root.id} style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                  {/* Root folder option */}
                  <button
                    className={`move-folder-option ${isRootCurrent ? 'current' : ''}`}
                    onClick={() => handleSelectFolder(root.id)}
                    disabled={isRootCurrent}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '10px',
                      padding: '10px 12px',
                      borderRadius: '8px',
                      border: '1px solid var(--border-color)',
                      backgroundColor: isRootCurrent ? 'var(--bg-subtle)' : 'var(--card-bg)',
                      cursor: isRootCurrent ? 'default' : 'pointer',
                      textAlign: 'left',
                      width: '100%',
                    }}
                  >
                    <div
                      style={{
                        width: '26px',
                        height: '26px',
                        borderRadius: '6px',
                        backgroundColor: `${root.color || '#4361EE'}20`,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: root.color || '#4361EE',
                        flexShrink: 0,
                      }}
                    >
                      <FolderIcon size={14} />
                    </div>
                    <div style={{ flexGrow: 1, minWidth: 0 }}>
                      <div style={{ fontWeight: 600, fontSize: '13px', color: isRootCurrent ? 'var(--text-secondary)' : 'var(--text-primary)' }}>
                        {root.name}
                      </div>
                    </div>
                    {isRootCurrent ? (
                      <span style={{ fontSize: '11px', color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: '4px' }}>
                        <Check size={13} /> Actual
                      </span>
                    ) : (
                      <MoveRight size={14} style={{ color: 'var(--primary)', opacity: 0.7 }} />
                    )}
                  </button>

                  {/* Subfolders list */}
                  {subfolders.map((sub) => {
                    const isSubCurrent = sub.id === currentFolderId;
                    return (
                      <button
                        key={sub.id}
                        className={`move-folder-option ${isSubCurrent ? 'current' : ''}`}
                        onClick={() => handleSelectFolder(sub.id)}
                        disabled={isSubCurrent}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          gap: '10px',
                          padding: '8px 12px',
                          marginLeft: '20px',
                          borderRadius: '8px',
                          border: '1px solid var(--border-color)',
                          backgroundColor: isSubCurrent ? 'var(--bg-subtle)' : 'var(--bg-subtle)',
                          cursor: isSubCurrent ? 'default' : 'pointer',
                          textAlign: 'left',
                        }}
                      >
                        <div
                          style={{
                            width: '22px',
                            height: '22px',
                            borderRadius: '4px',
                            backgroundColor: `${sub.color || root.color || '#4361EE'}20`,
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            color: sub.color || root.color || '#4361EE',
                            flexShrink: 0,
                          }}
                        >
                          <FolderIcon size={12} />
                        </div>
                        <div style={{ flexGrow: 1, minWidth: 0 }}>
                          <div style={{ fontWeight: 500, fontSize: '12px', color: isSubCurrent ? 'var(--text-secondary)' : 'var(--text-primary)' }}>
                            {sub.name}
                          </div>
                        </div>
                        {isSubCurrent ? (
                          <span style={{ fontSize: '11px', color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: '4px' }}>
                            <Check size={13} /> Actual
                          </span>
                        ) : (
                          <MoveRight size={13} style={{ color: 'var(--primary)', opacity: 0.7 }} />
                        )}
                      </button>
                    );
                  })}
                </div>
              );
            })
          )}
        </div>

        <div className="modal-actions" style={{ marginTop: '16px' }}>
          <button type="button" className="btn-secondary" onClick={onClose}>
            Cancelar
          </button>
        </div>
      </div>
    </div>
  );
};
