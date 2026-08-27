-- ==============================================================================
-- BOARDS APP - SUPABASE PRODUCTION DATABASE SCHEMA (100% IDEMPOTENTE)
-- Proyecto: nbtzhmsyvjjgtkfupsby (https://nbtzhmsyvjjgtkfupsby.supabase.co)
-- ==============================================================================

-- 1. Habilitar extensión UUID
create extension if not exists "uuid-ossp";

-- 2. TABLA DE TABLEROS / CARPETAS (folders con soporte de subcarpetas)
create table if not exists public.folders (
    id uuid primary key default uuid_generate_v4(),
    user_id uuid not null references auth.users(id) on delete cascade,
    parent_id uuid references public.folders(id) on delete cascade,
    name text not null,
    color text not null default '#4361EE',
    order_index integer not null default 0,
    is_deleted boolean not null default false,
    created_at timestamp with time zone not null default timezone('utc'::text, now()),
    updated_at timestamp with time zone not null default timezone('utc'::text, now())
);

-- Migración segura si la columna parent_id no existía
do $$
begin
  if not exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'folders' and column_name = 'parent_id') then
    alter table public.folders add column parent_id uuid references public.folders(id) on delete cascade;
  end if;
end $$;

-- Índices para folders
create index if not exists idx_folders_user_order on public.folders (user_id, is_deleted, order_index);
create index if not exists idx_folders_parent on public.folders (parent_id);

-- 3. TABLA DE RESPUESTAS RÁPIDAS (quick_replies)
create table if not exists public.quick_replies (
    id uuid primary key default uuid_generate_v4(),
    user_id uuid not null references auth.users(id) on delete cascade,
    folder_id uuid not null references public.folders(id) on delete cascade,
    title text not null,
    content text not null,
    content_type text not null default 'TEXT', -- TEXT, IMAGE, AUDIO, PDF, CONTACT, LOCATION, LINK, SEQUENCE
    media_url text,
    is_favorite boolean not null default false,
    order_index integer not null default 0,
    usage_count integer not null default 0,
    is_deleted boolean not null default false,
    created_at timestamp with time zone not null default timezone('utc'::text, now()),
    updated_at timestamp with time zone not null default timezone('utc'::text, now())
);

-- Índices para quick_replies
create index if not exists idx_replies_user_folder on public.quick_replies (user_id, folder_id, is_deleted, order_index);
create index if not exists idx_replies_favorites on public.quick_replies (user_id, is_favorite, is_deleted);

-- 4. FUNCIÓN Y TRIGGERS PARA 'updated_at'
create or replace function public.handle_updated_at()
returns trigger as $$
begin
    new.updated_at = timezone('utc'::text, now());
    return new;
end;
$$ language plpgsql;

do $$
begin
  if not exists (select 1 from pg_trigger where tgname = 'set_folders_updated_at') then
    create trigger set_folders_updated_at
      before update on public.folders
      for each row execute function public.handle_updated_at();
  end if;

  if not exists (select 1 from pg_trigger where tgname = 'set_quick_replies_updated_at') then
    create trigger set_quick_replies_updated_at
      before update on public.quick_replies
      for each row execute function public.handle_updated_at();
  end if;
end $$;

-- 5. POLÍTICAS DE SEGURIDAD RLS (ROW LEVEL SECURITY)
alter table public.folders enable row level security;
alter table public.quick_replies enable row level security;

-- Políticas de folders
do $$
begin
  drop policy if exists "Los usuarios solo pueden ver sus propios tableros" on public.folders;
  create policy "Los usuarios solo pueden ver sus propios tableros"
      on public.folders for select using (auth.uid() = user_id);

  drop policy if exists "Los usuarios pueden crear sus propios tableros" on public.folders;
  create policy "Los usuarios pueden crear sus propios tableros"
      on public.folders for insert with check (auth.uid() = user_id);

  drop policy if exists "Los usuarios pueden actualizar sus propios tableros" on public.folders;
  create policy "Los usuarios pueden actualizar sus propios tableros"
      on public.folders for update using (auth.uid() = user_id) with check (auth.uid() = user_id);

  drop policy if exists "Los usuarios pueden eliminar sus propios tableros" on public.folders;
  create policy "Los usuarios pueden eliminar sus propios tableros"
      on public.folders for delete using (auth.uid() = user_id);
end $$;

-- Políticas de quick_replies
do $$
begin
  drop policy if exists "Los usuarios solo pueden ver sus propias respuestas" on public.quick_replies;
  create policy "Los usuarios solo pueden ver sus propias respuestas"
      on public.quick_replies for select using (auth.uid() = user_id);

  drop policy if exists "Los usuarios pueden crear sus propias respuestas" on public.quick_replies;
  create policy "Los usuarios pueden crear sus propias respuestas"
      on public.quick_replies for insert with check (auth.uid() = user_id);

  drop policy if exists "Los usuarios pueden actualizar sus propias respuestas" on public.quick_replies;
  create policy "Los usuarios pueden actualizar sus propias respuestas"
      on public.quick_replies for update using (auth.uid() = user_id) with check (auth.uid() = user_id);

  drop policy if exists "Los usuarios pueden eliminar sus propias respuestas" on public.quick_replies;
  create policy "Los usuarios pueden eliminar sus propias respuestas"
      on public.quick_replies for delete using (auth.uid() = user_id);
end $$;

-- 6. HABILITAR PUBLICACIÓN EN TIEMPO REAL (SUPABASE REALTIME)
do $$
begin
  if not exists (
    select 1 from pg_publication_tables 
    where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'folders'
  ) then
    alter publication supabase_realtime add table public.folders;
  end if;

  if not exists (
    select 1 from pg_publication_tables 
    where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'quick_replies'
  ) then
    alter publication supabase_realtime add table public.quick_replies;
  end if;
end $$;

-- 7. STORAGE BUCKET PARA MULTIMEDIA
insert into storage.buckets (id, name, public)
values ('boards_media', 'boards_media', true)
on conflict (id) do nothing;

do $$
begin
  drop policy if exists "Acceso publico de lectura a boards_media" on storage.objects;
  create policy "Acceso publico de lectura a boards_media"
      on storage.objects for select using (bucket_id = 'boards_media');

  drop policy if exists "Usuarios pueden subir a boards_media" on storage.objects;
  create policy "Usuarios pueden subir a boards_media"
      on storage.objects for insert with check (bucket_id = 'boards_media' and auth.role() = 'authenticated');
end $$;
