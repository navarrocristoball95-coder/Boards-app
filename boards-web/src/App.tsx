import { useState, useEffect, useMemo, useCallback, useRef } from 'react';
import { supabase, hasValidCredentials } from './lib/supabaseClient';
import { Folder, QuickReply, ContentType } from './types/database';
import { Header } from './components/Header';
import { Sidebar } from './components/Sidebar';
import { ReplyCard } from './components/ReplyCard';
import { ReplyEditorModal } from './components/ReplyEditorModal';
import { MoveReplyModal } from './components/MoveReplyModal';
import { ConfigModal } from './components/ConfigModal';
import { AuthModal } from './components/AuthModal';
import { TrashModal } from './components/TrashModal';
import { User } from '@supabase/supabase-js';
import { Plus, Sparkles, Inbox, RotateCcw, X, Check } from 'lucide-react';
import confetti from 'canvas-confetti';
import './App.css';

const isUUID = (str: string) => /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(str);

export function App() {
  const [theme, setTheme] = useState<'light' | 'dark'>(() => {
    return (localStorage.getItem('boards_theme') as 'light' | 'dark') || 'light';
  });

  const [user, setUser] = useState<User | null>(null);
  const [syncStatus, setSyncStatus] = useState<'synced' | 'syncing' | 'offline' | 'error'>('offline');
  const [folders, setFolders] = useState<Folder[]>([]);
  const [replies, setReplies] = useState<QuickReply[]>([]);
  const [activeFolderId, setActiveFolderId] = useState<string | 'all' | 'favorites' | 'trash'>('all');
  const [searchQuery, setSearchQuery] = useState('');

  // Modals
  const [isEditorOpen, setIsEditorOpen] = useState(false);
  const [editingReply, setEditingReply] = useState<Partial<QuickReply> | null>(null);
  const [isConfigOpen, setIsConfigOpen] = useState(false);
  const [isAuthOpen, setIsAuthOpen] = useState(false);
  const [authInitialMode, setAuthInitialMode] = useState<'login' | 'signup' | 'forgot-password' | 'update-password'>('login');
  const [isTrashOpen, setIsTrashOpen] = useState(false);
  const [replyToMove, setReplyToMove] = useState<QuickReply | null>(null);

  // Undo Toast state
  const [lastDeletedReply, setLastDeletedReply] = useState<QuickReply | null>(null);
  const [showUndoToast, setShowUndoToast] = useState(false);
  const undoTimeoutRef = useRef<any>(null);

  // Apply theme
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('boards_theme', theme);
  }, [theme]);

  // Initialize Session
  useEffect(() => {
    supabase.auth.getSession().then(({ data: { session } }) => {
      const currentUser = session?.user ?? null;
      setUser(currentUser);
      if (!currentUser) {
        setFolders([]);
        setReplies([]);
        localStorage.removeItem('boards_local_folders');
        localStorage.removeItem('boards_local_replies');
        setSyncStatus('offline');
      }
    });

    const {
      data: { subscription },
    } = supabase.auth.onAuthStateChange((event, session) => {
      const currentUser = session?.user ?? null;
      setUser(currentUser);
      if (event === 'PASSWORD_RECOVERY') {
        setAuthInitialMode('update-password');
        setIsAuthOpen(true);
      } else if (event === 'SIGNED_OUT' || !currentUser) {
        setFolders([]);
        setReplies([]);
        localStorage.removeItem('boards_local_folders');
        localStorage.removeItem('boards_local_replies');
        setActiveFolderId('all');
        setSyncStatus('offline');
      }
    });

    return () => subscription.unsubscribe();
  }, []);

  // Fetch Data from Supabase (Only for authenticated users)
  const loadData = useCallback(async () => {
    if (!user) {
      setFolders([]);
      setReplies([]);
      setSyncStatus('offline');
      return;
    }

    if (!hasValidCredentials()) {
      setSyncStatus('offline');
      return;
    }

    try {
      setSyncStatus('syncing');
      
      // Fetch remote data from Supabase (Only active, non-deleted)
      const { data: foldersData, error: foldersErr } = await supabase
        .from('folders')
        .select('*')
        .eq('is_deleted', false)
        .order('order_index', { ascending: true });

      if (foldersErr) throw foldersErr;

      const { data: repliesData, error: repliesErr } = await supabase
        .from('quick_replies')
        .select('*')
        .eq('is_deleted', false)
        .order('is_favorite', { ascending: false })
        .order('order_index', { ascending: true });

      if (repliesErr) throw repliesErr;

      // 1. Deduplicate remote folders by id
      const uniqueFoldersMap = new Map<string, Folder>();
      for (const f of foldersData || []) {
        if (!uniqueFoldersMap.has(f.id)) {
          uniqueFoldersMap.set(f.id, f);
        }
      }
      const rawFolders = Array.from(uniqueFoldersMap.values());

      // 2. Clean any duplicate boards with the exact same name and parent_id
      const seenNameAndParent = new Map<string, string>();
      const dupesToDelete: string[] = [];
      const currentFolders: Folder[] = [];

      for (const f of rawFolders) {
        const key = `${(f.parent_id || 'root')}_${f.name.trim().toLowerCase()}`;
        if (!seenNameAndParent.has(key)) {
          seenNameAndParent.set(key, f.id);
          currentFolders.push(f);
        } else {
          dupesToDelete.push(f.id);
        }
      }

      if (dupesToDelete.length > 0) {
        supabase.from('folders').update({ is_deleted: true }).in('id', dupesToDelete).then(() => {});
      }

      // 3. Deduplicate replies by id and eliminate dummy 'prueba'
      const uniqueRepliesMap = new Map<string, QuickReply>();
      const dummyRepliesToDelete: string[] = [];

      for (const r of repliesData || []) {
        const isDummy = r.title.trim().toLowerCase() === 'prueba' || r.title.trim().toLowerCase() === 'mensaje de prueba';
        if (isDummy) {
          dummyRepliesToDelete.push(r.id);
          continue;
        }
        if (!uniqueRepliesMap.has(r.id)) {
          uniqueRepliesMap.set(r.id, r);
        }
      }

      if (dummyRepliesToDelete.length > 0) {
        supabase.from('quick_replies').update({ is_deleted: true }).in('id', dummyRepliesToDelete).then(() => {});
      }

      const currentReplies = Array.from(uniqueRepliesMap.values());

      setFolders(currentFolders);
      setReplies(currentReplies);
      localStorage.setItem('boards_local_folders', JSON.stringify(currentFolders));
      localStorage.setItem('boards_local_replies', JSON.stringify(currentReplies));
      setSyncStatus('synced');
    } catch (err) {
      console.warn('Supabase fetch error', err);
      setSyncStatus('error');
    }
  }, [user]);

  useEffect(() => {
    if (user) {
      loadData();
    } else {
      setFolders([]);
      setReplies([]);
      setSyncStatus('offline');
    }
  }, [user, loadData]);

  // Realtime subscription & Background Polling (ONLY active when user is authenticated)
  useEffect(() => {
    if (!user || !hasValidCredentials()) {
      setSyncStatus('offline');
      return;
    }

    const channel = supabase
      .channel('boards_realtime_sync')
      .on('postgres_changes', { event: '*', schema: 'public', table: 'folders' }, () => {
        loadData();
      })
      .on('postgres_changes', { event: '*', schema: 'public', table: 'quick_replies' }, () => {
        loadData();
      })
      .subscribe();

    const interval = setInterval(() => {
      loadData();
    }, 4000);

    return () => {
      supabase.removeChannel(channel);
      clearInterval(interval);
    };
  }, [user, loadData]);

  // Handlers for Folders
  const handleCreateFolder = async (name: string, color: string, parentId?: string | null) => {
    const tempId = Date.now().toString();
    const newFolderLocal: Folder = {
      id: tempId,
      user_id: user?.id || '00000000-0000-0000-0000-000000000000',
      parent_id: parentId || null,
      name: name.trim(),
      color,
      order_index: folders.length,
      is_deleted: false,
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    };

    setFolders(prev => [...prev, newFolderLocal]);
    setActiveFolderId(tempId);

    if (hasValidCredentials()) {
      try {
        let parentUuid: string | null = null;
        if (parentId) {
          if (isUUID(parentId)) {
            parentUuid = parentId;
          } else {
            const parentFolder = folders.find(f => f.id === parentId);
            if (parentFolder) {
              const { data: existingParent } = await supabase
                .from('folders')
                .select('id')
                .eq('name', parentFolder.name)
                .eq('is_deleted', false)
                .maybeSingle();

              if (existingParent) {
                parentUuid = existingParent.id;
              }
            }
          }
        }

        const payload: any = {
          user_id: user?.id || null,
          name: name.trim(),
          color,
          order_index: folders.length,
        };
        if (parentUuid) {
          payload.parent_id = parentUuid;
        }

        const { data, error } = await supabase.from('folders').insert([payload]).select().single();
        if (!error && data) {
          setFolders(prev => prev.map(f => f.id === tempId ? data : f));
          setActiveFolderId(data.id);
        }
      } catch (err) {
        console.warn('Cloud folder sync error:', err);
      }
    }
  };

  const handleEditFolder = async (id: string, name: string, color: string) => {
    const previousFolders = [...folders];
    setFolders(prev => prev.map((f) => (f.id === id ? { ...f, name: name.trim(), color: color, updated_at: new Date().toISOString() } : f)));

    if (hasValidCredentials() && isUUID(id)) {
      try {
        const { error } = await supabase.from('folders').update({ name: name.trim(), color: color }).eq('id', id);
        if (error) {
          console.error('Error updating folder in Supabase:', error);
          setFolders(previousFolders);
          setSyncStatus('error');
        } else {
          setSyncStatus('synced');
        }
      } catch (err) {
        console.error('Network failure updating folder:', err);
        setFolders(previousFolders);
        setSyncStatus('error');
      }
    }
  };

  const handleDeleteFolder = async (id: string) => {
    const previousFolders = [...folders];
    const previousReplies = [...replies];

    // Identify folder and any subfolders belonging to it
    const subfolderIds = folders.filter((f) => f.parent_id === id).map((f) => f.id);
    const targetIds = [id, ...subfolderIds];

    const nextFolders = folders.filter((f) => !targetIds.includes(f.id));
    const nextReplies = replies.filter((r) => !targetIds.includes(r.folder_id));

    setFolders(nextFolders);
    setReplies(nextReplies);
    localStorage.setItem('boards_local_folders', JSON.stringify(nextFolders));
    localStorage.setItem('boards_local_replies', JSON.stringify(nextReplies));

    if (activeFolderId === id || subfolderIds.includes(activeFolderId)) {
      setActiveFolderId('all');
    }

    if (hasValidCredentials() && user) {
      const validUuids = targetIds.filter(isUUID);
      if (validUuids.length > 0) {
        try {
          const { error: fErr } = await supabase.from('folders').update({ is_deleted: true }).in('id', validUuids);
          const { error: rErr } = await supabase.from('quick_replies').update({ is_deleted: true }).in('folder_id', validUuids);
          if (fErr || rErr) {
            console.error('Error deleting folder/replies in Supabase:', fErr || rErr);
            setFolders(previousFolders);
            setReplies(previousReplies);
            localStorage.setItem('boards_local_folders', JSON.stringify(previousFolders));
            localStorage.setItem('boards_local_replies', JSON.stringify(previousReplies));
            setSyncStatus('error');
            alert('No se pudo eliminar el tablero en la nube.');
          } else {
            setSyncStatus('synced');
          }
        } catch (err) {
          console.error('Network failure deleting folder:', err);
          setFolders(previousFolders);
          setReplies(previousReplies);
          localStorage.setItem('boards_local_folders', JSON.stringify(previousFolders));
          localStorage.setItem('boards_local_replies', JSON.stringify(previousReplies));
          setSyncStatus('error');
        }
      }
    }
  };

  const handleMoveReply = async (replyId: string, targetFolderId: string) => {
    const targetFolder = folders.find((f) => f.id === targetFolderId);
    if (!targetFolder) return;

    const previousReplies = [...replies];

    // 1. Optimistic Local Update (0ms)
    setReplies((prev) =>
      prev.map((r) =>
        r.id === replyId
          ? { ...r, folder_id: targetFolderId, updated_at: new Date().toISOString() }
          : r
      )
    );

    // 2. Direct Cloud Sync to Supabase
    if (hasValidCredentials() && user && isUUID(replyId)) {
      try {
        let cloudFolderUuid = targetFolderId;
        if (!isUUID(cloudFolderUuid)) {
          const { data: existingF } = await supabase
            .from('folders')
            .select('id')
            .eq('user_id', user.id)
            .eq('name', targetFolder.name)
            .eq('is_deleted', false)
            .maybeSingle();
          if (existingF) cloudFolderUuid = existingF.id;
        }

        if (isUUID(cloudFolderUuid)) {
          const { error } = await supabase
            .from('quick_replies')
            .update({ folder_id: cloudFolderUuid, updated_at: new Date().toISOString() })
            .eq('id', replyId);
          if (error) {
            console.error('Supabase move reply error:', error);
            setReplies(previousReplies);
            setSyncStatus('error');
            alert('No se pudo mover el mensaje en la nube.');
          } else {
            setSyncStatus('synced');
          }
        }
      } catch (err) {
        console.error('Error syncing moved reply to Supabase:', err);
        setReplies(previousReplies);
        setSyncStatus('error');
      }
    }
  };

  // Handlers for Replies
  const handleSaveReply = async (data: {
    id?: string;
    folder_id: string;
    title: string;
    content: string;
    content_type: ContentType;
    media_url?: string | null;
    is_favorite: boolean;
  }) => {
    const tempId = data.id || Date.now().toString();
    const resolvedFolderId = data.folder_id && data.folder_id !== 'all' && data.folder_id !== 'favorites' && data.folder_id !== 'trash'
      ? data.folder_id
      : (folders[0]?.id || '1');

    // 1. Optimistic Local Update (0ms)
    if (data.id) {
      setReplies(prev => prev.map(r => r.id === data.id ? { ...r, ...data, folder_id: resolvedFolderId, updated_at: new Date().toISOString() } : r));
    } else {
      const newReplyOptimistic: QuickReply = {
        id: tempId,
        user_id: user?.id || 'local',
        folder_id: resolvedFolderId,
        title: data.title,
        content: data.content,
        content_type: data.content_type,
        media_url: data.media_url || null,
        is_favorite: data.is_favorite,
        order_index: replies.length,
        usage_count: 0,
        is_deleted: false,
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString(),
      };
      setReplies(prev => [newReplyOptimistic, ...prev]);
      confetti({ particleCount: 50, spread: 60, origin: { y: 0.85 } });
    }

    setIsEditorOpen(false);
    setEditingReply(null);

    // 2. Direct Cloud Sync to Supabase
    if (hasValidCredentials()) {
      try {
        let cloudFolderUuid = resolvedFolderId;

        if (!isUUID(cloudFolderUuid) || !folders.some(f => f.id === cloudFolderUuid && isUUID(f.id))) {
          const matchingFolder = folders.find(f => f.id === resolvedFolderId);
          const folderName = matchingFolder?.name || 'General';
          const folderColor = matchingFolder?.color || '#4361EE';

          const { data: existingF } = await supabase
            .from('folders')
            .select('*')
            .eq('name', folderName)
            .eq('is_deleted', false)
            .maybeSingle();

          if (existingF) {
            cloudFolderUuid = existingF.id;
          } else {
            const { data: newF } = await supabase.from('folders').insert([{
              user_id: user?.id || null,
              name: folderName,
              color: folderColor,
              order_index: folders.length,
            }]).select().single();

            if (newF) {
              cloudFolderUuid = newF.id;
              setFolders(prev => [newF, ...prev]);
            }
          }
        }

        if (data.id && isUUID(data.id)) {
          const { error } = await supabase.from('quick_replies').update({
            folder_id: cloudFolderUuid,
            title: data.title,
            content: data.content,
            content_type: data.content_type,
            media_url: data.media_url,
            is_favorite: data.is_favorite,
          }).eq('id', data.id);
          if (error) console.error('Supabase update error:', error);
        } else {
          const { data: createdReply, error } = await supabase.from('quick_replies').insert([{
            user_id: user?.id || null,
            folder_id: cloudFolderUuid,
            title: data.title,
            content: data.content,
            content_type: data.content_type,
            media_url: data.media_url,
            is_favorite: data.is_favorite,
            order_index: replies.length,
          }]).select().single();

          if (!error && createdReply) {
            setReplies(prev => prev.map(r => r.id === tempId ? createdReply : r));
          } else if (error) {
            console.error('Supabase insert error:', error);
          }
        }
      } catch (err) {
        console.error('Cloud synchronization error:', err);
      }
    }
  };

  const handleToggleFavorite = async (reply: QuickReply) => {
    const nextFav = !reply.is_favorite;
    if (hasValidCredentials() && isUUID(reply.id)) {
      await supabase.from('quick_replies').update({ is_favorite: nextFav }).eq('id', reply.id);
    }
    setReplies(prev => prev.map((r) => (r.id === reply.id ? { ...r, is_favorite: nextFav } : r)));
  };

  // Soft Delete with Undo Toast
  const handleDeleteReply = async (id: string) => {
    const replyToDelete = replies.find(r => r.id === id);
    if (!replyToDelete) return;

    setLastDeletedReply(replyToDelete);
    setShowUndoToast(true);

    if (undoTimeoutRef.current) clearTimeout(undoTimeoutRef.current);
    undoTimeoutRef.current = setTimeout(() => {
      setShowUndoToast(false);
    }, 6000);

    // Soft delete in state
    setReplies(prev => prev.map(r => r.id === id ? { ...r, is_deleted: true } : r));

    // Soft delete in Supabase
    if (hasValidCredentials() && isUUID(id)) {
      await supabase.from('quick_replies').update({ is_deleted: true }).eq('id', id);
    }
  };

  // Undo Delete Action
  const handleUndoDelete = async () => {
    if (!lastDeletedReply) return;
    const toRestore = lastDeletedReply;
    setLastDeletedReply(null);
    setShowUndoToast(false);
    if (undoTimeoutRef.current) clearTimeout(undoTimeoutRef.current);

    setReplies(prev => prev.map(r => r.id === toRestore.id ? { ...r, is_deleted: false } : r));

    if (hasValidCredentials() && isUUID(toRestore.id)) {
      await supabase.from('quick_replies').update({ is_deleted: false }).eq('id', toRestore.id);
    }
  };

  // Restore from Trash
  const handleRestoreReply = async (id: string) => {
    setReplies(prev => prev.map(r => r.id === id ? { ...r, is_deleted: false } : r));
    if (hasValidCredentials() && isUUID(id)) {
      await supabase.from('quick_replies').update({ is_deleted: false }).eq('id', id);
    }
  };

  // Restore All from Trash
  const handleRestoreAllDeleted = async () => {
    const deletedIds = replies.filter(r => r.is_deleted).map(r => r.id);
    setReplies(prev => prev.map(r => ({ ...r, is_deleted: false })));
    if (hasValidCredentials() && deletedIds.length > 0) {
      await supabase.from('quick_replies').update({ is_deleted: false }).in('id', deletedIds.filter(isUUID));
    }
  };

  // Permanent Delete
  const handlePermanentDelete = async (id: string) => {
    setReplies(prev => prev.filter(r => r.id !== id));
    if (hasValidCredentials() && user && isUUID(id)) {
      await supabase.from('quick_replies').delete().eq('id', id);
    }
  };

  // Empty Trash
  const handleEmptyTrash = async () => {
    const deletedIds = replies.filter(r => r.is_deleted).map(r => r.id);
    setReplies(prev => prev.filter(r => !r.is_deleted));
    if (hasValidCredentials() && user && deletedIds.length > 0) {
      await supabase.from('quick_replies').delete().in('id', deletedIds.filter(isUUID));
    }
  };

  // Filtered Active and Deleted Replies
  const normalize = (str: string) => str.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();

  const activeReplies = useMemo(() => replies.filter(r => !r.is_deleted), [replies]);
  const deletedReplies = useMemo(() => replies.filter(r => r.is_deleted), [replies]);

  const filteredReplies = useMemo(() => {
    return activeReplies.filter((reply) => {
      if (activeFolderId === 'favorites') {
        if (!reply.is_favorite) return false;
      } else if (activeFolderId !== 'all' && activeFolderId !== 'trash') {
        const childFolderIds = folders.filter((f) => f.parent_id === activeFolderId).map((f) => f.id);
        const matchesFolder = reply.folder_id === activeFolderId || childFolderIds.includes(reply.folder_id);
        if (!matchesFolder) return false;
      }

      if (searchQuery.trim()) {
        const q = normalize(searchQuery);
        const matchTitle = normalize(reply.title).includes(q);
        const matchContent = normalize(reply.content).includes(q);
        if (!matchTitle && !matchContent) return false;
      }

      return true;
    });
  }, [activeReplies, activeFolderId, folders, searchQuery]);

  const activeFolderName = useMemo(() => {
    if (activeFolderId === 'all') return 'Todos los Mensajes';
    if (activeFolderId === 'favorites') return 'Mensajes Fijados';
    if (activeFolderId === 'trash') return 'Papelera de Reciclaje';
    const current = folders.find((item) => item.id === activeFolderId);
    if (!current) return 'Tablero';
    if (current.parent_id) {
      const parent = folders.find((f) => f.id === current.parent_id);
      return parent ? `${parent.name} ❯ ${current.name}` : current.name;
    }
    return current.name;
  }, [activeFolderId, folders]);

  const openAuthModal = (mode: 'login' | 'signup' | 'forgot-password' | 'update-password' = 'login') => {
    setAuthInitialMode(mode);
    setIsAuthOpen(true);
  };

  return (
    <div className="app-container">
      <Header
        user={user}
        syncStatus={syncStatus}
        theme={theme}
        onToggleTheme={() => setTheme(theme === 'light' ? 'dark' : 'light')}
        onOpenConfig={() => setIsConfigOpen(true)}
        onOpenAuth={() => openAuthModal('login')}
        onSignOut={async () => {
          await supabase.auth.signOut();
          localStorage.removeItem('boards_local_folders');
          localStorage.removeItem('boards_local_replies');
          setUser(null);
          setFolders([]);
          setReplies([]);
          setActiveFolderId('all');
          setAuthInitialMode('login');
        }}
        onManualSync={loadData}
      />

      <div className="main-content-layout">
        <Sidebar
          folders={folders}
          activeFolderId={activeFolderId}
          searchQuery={searchQuery}
          onSelectFolder={(id) => {
            if (id === 'trash') {
              setIsTrashOpen(true);
            } else {
              setActiveFolderId(id);
            }
          }}
          onSearchChange={setSearchQuery}
          onCreateFolder={handleCreateFolder}
          onEditFolder={handleEditFolder}
          onDeleteFolder={handleDeleteFolder}
          onMoveReply={handleMoveReply}
          onOpenTrash={() => setIsTrashOpen(true)}
          totalReplies={activeReplies.length}
          totalFavorites={activeReplies.filter((r) => r.is_favorite).length}
          totalDeleted={deletedReplies.length}
        />

        {!user ? (
          <main className="content-area empty-auth-state">
            <div className="empty-auth-container">
              <div className="empty-icon-circle-large">
                <Sparkles size={36} />
              </div>
              <h2>Bienvenido a Boards</h2>
              <p>Inicia sesión con tu cuenta para acceder a tus tableros, respuestas rápidas y sincronizar en tiempo real con tu teléfono móvil.</p>
              <button className="btn-primary-large" onClick={() => openAuthModal('login')}>
                <span>Iniciar Sesión / Registrarse</span>
              </button>
            </div>
          </main>
        ) : (
          <main className="content-area">
            <div className="content-top-bar">
              <div>
                <h2 className="current-view-title">{activeFolderName}</h2>
                <span className="current-view-subtitle">
                  {filteredReplies.length} {filteredReplies.length === 1 ? 'respuesta disponible' : 'respuestas disponibles'}
                </span>
              </div>

              <div className="content-actions">
                <button
                  className="btn-secondary"
                  onClick={() => {
                    const parentId = activeFolderId !== 'all' && activeFolderId !== 'favorites' && activeFolderId !== 'trash' ? activeFolderId : null;
                    const name = prompt(parentId ? 'Nombre de la subcarpeta:' : 'Nombre del nuevo tablero:');
                    if (name && name.trim()) {
                      handleCreateFolder(name.trim(), '#4361EE', parentId);
                    }
                  }}
                  title="Crear tablero o subcarpeta rápida"
                >
                  <Plus size={16} />
                  <span>{activeFolderId !== 'all' && activeFolderId !== 'favorites' && activeFolderId !== 'trash' ? 'Nueva Subcarpeta' : 'Nuevo Tablero'}</span>
                </button>

                <button
                  className="btn-primary"
                  onClick={() => {
                    setEditingReply(null);
                    setIsEditorOpen(true);
                  }}
                >
                  <Plus size={16} />
                  <span>Crear Respuesta</span>
                </button>
              </div>
            </div>

            {filteredReplies.length === 0 ? (
              <div className="empty-state">
                <div className="empty-icon-circle">
                  <Inbox size={32} />
                </div>
                <h3>No hay respuestas en esta vista</h3>
                <p>Comienza agregando tu primera respuesta rápida con plantillas de WhatsApp o secuencias.</p>
                <button
                  className="btn-primary"
                  onClick={() => {
                    setEditingReply(null);
                    setIsEditorOpen(true);
                  }}
                >
                  <Sparkles size={16} />
                  <span>Crear Primera Respuesta</span>
                </button>
              </div>
            ) : (
              <div className="replies-grid">
                {filteredReplies.map((reply) => (
                  <ReplyCard
                    key={reply.id}
                    reply={reply}
                    onToggleFavorite={handleToggleFavorite}
                    onEdit={(r) => {
                      setEditingReply(r);
                      setIsEditorOpen(true);
                    }}
                    onDelete={handleDeleteReply}
                    onMove={(r) => setReplyToMove(r)}
                  />
                ))}
              </div>
            )}
          </main>
        )}
      </div>

      {/* Floating Undo Toast */}
      {showUndoToast && lastDeletedReply && (
        <div className="undo-floating-toast">
          <div className="undo-toast-left">
            <div className="undo-icon-badge">
              <Check size={14} />
            </div>
            <span>Respuesta eliminada</span>
          </div>
          <button className="btn-undo-action" onClick={handleUndoDelete}>
            <RotateCcw size={14} />
            <span>Deshacer</span>
          </button>
          <button className="btn-toast-close" onClick={() => setShowUndoToast(false)}>
            <X size={14} />
          </button>
        </div>
      )}

      {/* Modals */}
      {replyToMove && (
        <MoveReplyModal
          reply={replyToMove}
          folders={folders}
          currentFolderId={replyToMove.folder_id}
          onClose={() => setReplyToMove(null)}
          onMove={handleMoveReply}
        />
      )}

      {isEditorOpen && (
        <ReplyEditorModal
          initialReply={editingReply}
          folders={folders}
          currentFolderId={activeFolderId}
          onSave={handleSaveReply}
          onClose={() => {
            setIsEditorOpen(false);
            setEditingReply(null);
          }}
        />
      )}

      {isTrashOpen && (
        <TrashModal
          deletedReplies={deletedReplies}
          folders={folders}
          onRestore={handleRestoreReply}
          onRestoreAll={handleRestoreAllDeleted}
          onPermanentDelete={handlePermanentDelete}
          onEmptyTrash={handleEmptyTrash}
          onClose={() => setIsTrashOpen(false)}
        />
      )}

      {isConfigOpen && (
        <ConfigModal
          onClose={() => setIsConfigOpen(false)}
          onSaved={loadData}
        />
      )}

      {isAuthOpen && (
        <AuthModal
          initialMode={authInitialMode}
          onClose={() => setIsAuthOpen(false)}
          onSuccess={loadData}
        />
      )}
    </div>
  );
}

export default App;
