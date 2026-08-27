export type ContentType = 
  | 'TEXT'
  | 'IMAGE'
  | 'AUDIO'
  | 'PDF'
  | 'CONTACT'
  | 'LOCATION'
  | 'LINK'
  | 'SEQUENCE';

export interface Folder {
  id: string;
  user_id: string;
  parent_id?: string | null;
  name: string;
  color: string;
  order_index: number;
  is_deleted: boolean;
  created_at: string;
  updated_at: string;
}

export interface QuickReply {
  id: string;
  user_id: string;
  folder_id: string;
  title: string;
  content: string;
  content_type: ContentType;
  media_url: string | null;
  is_favorite: boolean;
  order_index: number;
  usage_count: number;
  is_deleted: boolean;
  created_at: string;
  updated_at: string;
}

export interface ContactData {
  fullName: string;
  phone: string;
  email?: string;
  organization?: string;
  jobTitle?: string;
  notes?: string;
  photoUrl?: string;
}

export interface DynamicVariable {
  rawTag: string;
  name: string;
  defaultValue: string;
  isCalculated: boolean;
  formula: string;
}
