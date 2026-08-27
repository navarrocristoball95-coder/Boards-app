import { column, Schema, Table } from '@powersync/web';
import { PowerSyncDatabase } from '@powersync/web';

// Definición de Tablas con Esquema Tipado Offline-First
export const foldersTable = new Table({
  name: column.text,
  color: column.text,
  parent_id: column.text,
  order_index: column.integer,
  is_deleted: column.integer,
  user_id: column.text,
  created_at: column.text,
  updated_at: column.text
});

export const quickRepliesTable = new Table({
  folder_id: column.text,
  title: column.text,
  content: column.text,
  content_type: column.text,
  media_url: column.text,
  shortcut: column.text,
  order_index: column.integer,
  is_favorite: column.integer,
  usage_count: column.integer,
  is_deleted: column.integer,
  user_id: column.text,
  created_at: column.text,
  updated_at: column.text
});

export const AppSchema = new Schema({
  folders: foldersTable,
  quick_replies: quickRepliesTable
});

export type Database = (typeof AppSchema)['types'];

let powerSyncDb: PowerSyncDatabase | null = null;

export const getPowerSyncDatabase = (): PowerSyncDatabase | null => {
  if (typeof window === 'undefined' || typeof Worker === 'undefined') {
    return null;
  }
  try {
    if (!powerSyncDb) {
      powerSyncDb = new PowerSyncDatabase({
        schema: AppSchema,
        database: {
          dbFilename: 'quickreply_boards.db'
        }
      });
    }
    return powerSyncDb;
  } catch (err) {
    console.warn('PowerSync WebWorker init failed, falling back to standard client:', err);
    return null;
  }
};
