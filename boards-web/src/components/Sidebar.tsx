import React, { useState, useMemo } from 'react';
import { Folder as FolderType } from '../types/database';
import { 
  FolderPlus, 
  Trash2, 
  Edit2, 
  Star, 
  Search, 
  Hash, 
  ChevronDown, 
  ChevronRight, 
  Folder as FolderIcon,
  Plus
} from 'lucide-react';

interface SidebarProps {
  folders: FolderType[];
  activeFolderId: string | 'all' | 'favorites' | 'trash';
  searchQuery: string;
  onSelectFolder: (id: string | 'all' | 'favorites' | 'trash') => void;
  onSearchChange: (query: string) => void;
  onCreateFolder: (name: string, color: string, parentId?: string | null) => void;
  onEditFolder: (id: string, name: string, color: string) => void;
  onDeleteFolder: (id: string) => void;
  onMoveReply?: (replyId: string, targetFolderId: string) => void;
  onOpenTrash: () => void;
  totalReplies: number;
  totalFavorites: number;
  totalDeleted: number;
}

const PRESET_COLORS = ['#4361EE', '#3A0CA3', '#7209B7', '#F72585', '#4CC9F0', '#10B981', '#F59E0B', '#EF4444'];
const POPULAR_EMOJIS = ['📋', '💼', '🚀', '💬', '⚡', '🛒', '🏢', '⭐', '💰', '🎯', '🏷️', '📦', '📁', '🔑', '💡', '🔥', '📞', '✨', '📝', '📍', '🛍️', '🎉', '🤝', '💎'];

function parseFolderEmojiAndName(rawName: string): { emoji: string; cleanName: string } {
  if (!rawName) return { emoji: '📋', cleanName: '' };
  const trimmed = rawName.trim();
  const match = trimmed.match(/^(\p{Extended_Pictographic}|\p{Emoji_Presentation}|\p{Emoji}\uFE0F)\s*(.*)$/u);
  if (match) {
    return { emoji: match[1], cleanName: match[2].trim() };
  }
  return { emoji: '📋', cleanName: trimmed };
}

export const Sidebar: React.FC<SidebarProps> = ({
  folders,
  activeFolderId,
  searchQuery,
  onSelectFolder,
  onSearchChange,
  onCreateFolder,
  onEditFolder,
  onDeleteFolder,
  onMoveReply,
  onOpenTrash,
  totalReplies,
  totalFavorites,
  totalDeleted,
}) => {
  const [showNewFolderModal, setShowNewFolderModal] = useState(false);
  const [editingFolder, setEditingFolder] = useState<FolderType | null>(null);
  const [newFolderName, setNewFolderName] = useState('');
  const [selectedEmoji, setSelectedEmoji] = useState('📋');
  const [parentBoardId, setParentBoardId] = useState<string | null>(null);
  const [selectedColor, setSelectedColor] = useState(PRESET_COLORS[0]);
  const [collapsedBoards, setCollapsedBoards] = useState<Record<string, boolean>>({});
  const [dragOverFolderId, setDragOverFolderId] = useState<string | null>(null);

  const rootFolders = useMemo(() => {
    const uniqueMap = new Map<string, FolderType>();
    const seenNames = new Set<string>();

    for (const f of folders) {
      if (f.parent_id || f.is_deleted) continue;
      const normName = f.name.trim().toLowerCase();
      if (!uniqueMap.has(f.id) && !seenNames.has(normName)) {
        uniqueMap.set(f.id, f);
        seenNames.add(normName);
      }
    }
    return Array.from(uniqueMap.values());
  }, [folders]);

  const getSubfolders = (parentId: string) => {
    const uniqueMap = new Map<string, FolderType>();
    for (const f of folders) {
      if (f.parent_id === parentId && !f.is_deleted && !uniqueMap.has(f.id)) {
        uniqueMap.set(f.id, f);
      }
    }
    return Array.from(uniqueMap.values());
  };

  const toggleCollapse = (boardId: string) => {
    setCollapsedBoards((prev) => ({ ...prev, [boardId]: !prev[boardId] }));
  };

  const handleOpenCreateModal = (parentId: string | null = null) => {
    setEditingFolder(null);
    setParentBoardId(parentId);
    const parentFolder = folders.find((f) => f.id === parentId);
    setSelectedColor(parentFolder ? parentFolder.color : PRESET_COLORS[0]);
    setSelectedEmoji(parentId ? '📁' : '📋');
    setNewFolderName('');
    setShowNewFolderModal(true);
  };

  const handleOpenEditModal = (folder: FolderType) => {
    setEditingFolder(folder);
    setParentBoardId(folder.parent_id || null);
    setSelectedColor(folder.color || PRESET_COLORS[0]);
    const { emoji, cleanName } = parseFolderEmojiAndName(folder.name);
    setSelectedEmoji(emoji);
    setNewFolderName(cleanName || folder.name);
    setShowNewFolderModal(true);
  };

  const handleSubmitFolder = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newFolderName.trim()) return;

    const trimmedName = newFolderName.trim();
    const finalName = selectedEmoji ? `${selectedEmoji} ${trimmedName}` : trimmedName;

    if (editingFolder) {
      onEditFolder(editingFolder.id, finalName, selectedColor);
    } else {
      onCreateFolder(finalName, selectedColor, parentBoardId);
    }

    setNewFolderName('');
    setEditingFolder(null);
    setShowNewFolderModal(false);
  };

  return (
    <aside className="app-sidebar">
      {/* Search Box */}
      <div className="search-container">
        <Search size={16} className="search-icon" />
        <input
          type="text"
          placeholder="Buscar mensajes o atajos..."
          value={searchQuery}
          onChange={(e) => onSearchChange(e.target.value)}
          className="search-input"
        />
        {searchQuery && (
          <button className="clear-search" onClick={() => onSearchChange('')}>×</button>
        )}
      </div>

      {/* Global Navigation Filters */}
      <div className="sidebar-section">
        <span className="section-title">Vistas Rápidas</span>
        <button
          className={`nav-item ${activeFolderId === 'all' ? 'active' : ''}`}
          onClick={() => onSelectFolder('all')}
        >
          <div className="nav-icon" style={{ backgroundColor: '#EEF2FF', color: '#4361EE' }}>
            <Hash size={16} />
          </div>
          <span className="nav-label">Todos los Mensajes</span>
          <span className="nav-count">{totalReplies}</span>
        </button>

        <button
          className={`nav-item ${activeFolderId === 'favorites' ? 'active' : ''}`}
          onClick={() => onSelectFolder('favorites')}
        >
          <div className="nav-icon" style={{ backgroundColor: '#FEF3C7', color: '#D97706' }}>
            <Star size={16} />
          </div>
          <span className="nav-label">Fijados / Favoritos</span>
          <span className="nav-count">{totalFavorites}</span>
        </button>

        <button
          className={`nav-item ${activeFolderId === 'trash' ? 'active' : ''}`}
          onClick={onOpenTrash}
        >
          <div className="nav-icon" style={{ backgroundColor: '#FEE2E2', color: '#EF4444' }}>
            <Trash2 size={16} />
          </div>
          <span className="nav-label">Papelera de Reciclaje</span>
          <span className="nav-count" style={{ backgroundColor: totalDeleted > 0 ? '#FECACA' : 'var(--bg-subtle)', color: totalDeleted > 0 ? '#DC2626' : 'var(--text-secondary)' }}>
            {totalDeleted}
          </span>
        </button>
      </div>

      {/* Custom Folders / Boards */}
      <div className="sidebar-section flex-grow">
        <div className="section-header">
          <span className="section-title">Mis Tableros & Carpetas</span>
          <button
            className="icon-mini-btn"
            onClick={() => handleOpenCreateModal(null)}
            title="Crear nuevo tablero"
            style={{ width: '28px', height: '28px', color: 'var(--primary)' }}
          >
            <FolderPlus size={18} />
          </button>
        </div>

        <div className="folders-list">
          {rootFolders.length === 0 ? (
            <div className="empty-folders">
              <p>No tienes tableros creados aún.</p>
              <button className="btn-link" onClick={() => handleOpenCreateModal(null)}>+ Crear el primero</button>
            </div>
          ) : (
            rootFolders.map((board) => {
              const subfolders = getSubfolders(board.id);
              const isCollapsed = !!collapsedBoards[board.id];
              const isBoardActive = activeFolderId === board.id;

              return (
                <div key={board.id} className="board-tree-node">
                  {/* Board Row */}
                  <div
                    className={`folder-item ${isBoardActive ? 'active' : ''} ${dragOverFolderId === board.id ? 'drag-over-active' : ''}`}
                    onClick={() => onSelectFolder(board.id)}
                    onDragOver={(e) => {
                      e.preventDefault();
                      e.dataTransfer.dropEffect = 'move';
                      if (dragOverFolderId !== board.id) setDragOverFolderId(board.id);
                    }}
                    onDragLeave={() => {
                      if (dragOverFolderId === board.id) setDragOverFolderId(null);
                    }}
                    onDrop={(e) => {
                      e.preventDefault();
                      setDragOverFolderId(null);
                      const replyId = e.dataTransfer.getData('text/plain');
                      if (replyId && onMoveReply) {
                        onMoveReply(replyId, board.id);
                      }
                    }}
                  >
                    {subfolders.length > 0 ? (
                      <button
                        type="button"
                        className="btn-collapse"
                        onClick={(e) => {
                          e.stopPropagation();
                          toggleCollapse(board.id);
                        }}
                      >
                        {isCollapsed ? <ChevronRight size={14} /> : <ChevronDown size={14} />}
                      </button>
                    ) : (
                      <div className="folder-color-dot" style={{ backgroundColor: board.color || '#4361EE' }} />
                    )}

                    <span className="folder-name">{board.name}</span>

                    <div className="folder-actions" onClick={(e) => e.stopPropagation()}>
                      <button
                        className="icon-mini-btn"
                        onClick={() => handleOpenCreateModal(board.id)}
                        title="Crear subcarpeta dentro de este tablero"
                      >
                        <Plus size={13} />
                      </button>
                      <button
                        className="icon-mini-btn"
                        onClick={() => handleOpenEditModal(board)}
                        title="Editar nombre y color del tablero"
                      >
                        <Edit2 size={12} />
                      </button>
                      <button
                        className="icon-mini-btn danger"
                        onClick={() => {
                          if (confirm(`¿Eliminar el tablero "${board.name}" y todas sus subcarpetas y respuestas?`)) {
                            onDeleteFolder(board.id);
                          }
                        }}
                        title="Eliminar tablero"
                      >
                        <Trash2 size={12} />
                      </button>
                    </div>
                  </div>

                  {/* Subfolders list */}
                  {!isCollapsed && subfolders.length > 0 && (
                    <div className="subfolders-container">
                      {subfolders.map((sub) => {
                        const isSubActive = activeFolderId === sub.id;
                        return (
                          <div
                            key={sub.id}
                            className={`subfolder-item ${isSubActive ? 'active' : ''} ${dragOverFolderId === sub.id ? 'drag-over-active' : ''}`}
                            onClick={() => onSelectFolder(sub.id)}
                            onDragOver={(e) => {
                              e.preventDefault();
                              e.dataTransfer.dropEffect = 'move';
                              if (dragOverFolderId !== sub.id) setDragOverFolderId(sub.id);
                            }}
                            onDragLeave={() => {
                              if (dragOverFolderId === sub.id) setDragOverFolderId(null);
                            }}
                            onDrop={(e) => {
                              e.preventDefault();
                              setDragOverFolderId(null);
                              const replyId = e.dataTransfer.getData('text/plain');
                              if (replyId && onMoveReply) {
                                onMoveReply(replyId, sub.id);
                              }
                            }}
                          >
                            <FolderIcon size={14} className="subfolder-icon" style={{ color: sub.color || board.color }} />
                            <span className="subfolder-name">{sub.name}</span>

                            <div className="folder-actions" onClick={(e) => e.stopPropagation()}>
                              <button
                                className="icon-mini-btn"
                                onClick={() => handleOpenEditModal(sub)}
                                title="Editar nombre y color de la subcarpeta"
                              >
                                <Edit2 size={11} />
                              </button>
                              <button
                                className="icon-mini-btn danger"
                                onClick={() => {
                                  if (confirm(`¿Eliminar la subcarpeta "${sub.name}" y sus respuestas?`)) {
                                    onDeleteFolder(sub.id);
                                  }
                                }}
                                title="Eliminar subcarpeta"
                              >
                                <Trash2 size={11} />
                              </button>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              );
            })
          )}
        </div>
      </div>

      {/* Modal Nuevo o Editar Tablero / Subcarpeta */}
      {showNewFolderModal && (
        <div className="modal-backdrop" onClick={() => setShowNewFolderModal(false)}>
          <div className="modal-card small" onClick={(e) => e.stopPropagation()}>
            <h3>
              {editingFolder
                ? (editingFolder.parent_id ? 'Editar Subcarpeta' : 'Editar Tablero')
                : (parentBoardId ? 'Crear Subcarpeta en Tablero' : 'Crear Nuevo Tablero')}
            </h3>
            <p className="modal-subtitle">
              {editingFolder
                ? 'Modifica el nombre o el color identificador del ícono'
                : (parentBoardId
                  ? `Se guardará dentro de: "${folders.find((f) => f.id === parentBoardId)?.name}"`
                  : 'Crea un tablero principal para organizar tus mensajes por temática')}
            </p>

            <form onSubmit={handleSubmitFolder}>
              {/* Preview de Ícono y Color */}
              <div style={{ display: 'flex', alignItems: 'center', gap: '14px', marginBottom: '16px', padding: '12px', background: 'var(--bg-subtle)', borderRadius: '12px', border: '1px solid var(--border)' }}>
                <div style={{ width: '48px', height: '48px', borderRadius: '50%', backgroundColor: `${selectedColor}22`, border: `2px solid ${selectedColor}`, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '24px' }}>
                  {selectedEmoji || '📁'}
                </div>
                <div>
                  <strong style={{ fontSize: '13px', display: 'block', color: 'var(--text-main)' }}>Ícono y Color del Tablero</strong>
                  <span style={{ fontSize: '11px', color: 'var(--text-secondary)' }}>Selecciona un emoji representativo y el color del ícono</span>
                </div>
              </div>

              {/* Selector de Emoji */}
              <div className="form-group" style={{ marginBottom: '14px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                  <label style={{ margin: 0 }}>Emoji / Ícono Representativo</label>
                  {selectedEmoji && (
                    <button 
                      type="button" 
                      onClick={() => setSelectedEmoji('')}
                      style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', fontSize: '11px', cursor: 'pointer', textDecoration: 'underline' }}
                    >
                      Quitar emoji
                    </button>
                  )}
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(8, 1fr)', gap: '6px', maxHeight: '110px', overflowY: 'auto', padding: '6px', background: 'var(--bg-card)', border: '1px solid var(--border)', borderRadius: '8px' }}>
                  {POPULAR_EMOJIS.map((emo) => (
                    <button
                      type="button"
                      key={emo}
                      onClick={() => setSelectedEmoji(emo)}
                      style={{
                        fontSize: '18px',
                        padding: '6px',
                        border: selectedEmoji === emo ? `2px solid ${selectedColor}` : '1px solid transparent',
                        borderRadius: '6px',
                        backgroundColor: selectedEmoji === emo ? `${selectedColor}18` : 'transparent',
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center'
                      }}
                    >
                      {emo}
                    </button>
                  ))}
                </div>
              </div>

              {/* Nombre del Tablero */}
              <div className="form-group">
                <label>
                  {editingFolder
                    ? (editingFolder.parent_id ? 'Nombre de la Subcarpeta' : 'Nombre del Tablero')
                    : (parentBoardId ? 'Nombre de la Subcarpeta' : 'Nombre del Tablero')}
                </label>
                <input
                  type="text"
                  placeholder={parentBoardId || editingFolder?.parent_id ? 'Ej: Clientes WhatsApp, Precios...' : 'Ej: Ventas, Soporte, Saludos...'}
                  value={newFolderName}
                  onChange={(e) => setNewFolderName(e.target.value)}
                  autoFocus
                  required
                />
              </div>

              {/* Selector de Color */}
              <div className="form-group">
                <label>Color del Ícono / Identificador</label>
                <div className="color-palette">
                  {PRESET_COLORS.map((c) => (
                    <button
                      type="button"
                      key={c}
                      className={`color-bubble ${selectedColor === c ? 'selected' : ''}`}
                      style={{ backgroundColor: c }}
                      onClick={() => setSelectedColor(c)}
                    />
                  ))}
                </div>
              </div>

              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => setShowNewFolderModal(false)}>
                  Cancelar
                </button>
                <button type="submit" className="btn-primary" disabled={!newFolderName.trim()}>
                  {editingFolder ? 'Guardar Cambios' : (parentBoardId ? 'Crear Subcarpeta' : 'Crear Tablero')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </aside>
  );
};
