import type { ApiErrorBody, Board, Comment, Notification, Page, Post, Profile, ReactionResponse, ReactionType, User } from './types';

// @ts-ignore
const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');
let accessToken: string | null = null;
let reissuePromise: Promise<boolean> | null = null;

export class ApiError extends Error {
  constructor(public status: number, public code: string, message: string) {
    super(message);
    this.name = 'ApiError';
  }
}

export function setAccessToken(token: string | null) {
  accessToken = token ? token.replace(/^Bearer\s+/i, '') : null;
}

export function imageUrl(path?: string | null) {
  if (!path) return '';
  if (/^https?:\/\//.test(path)) return path;
  return `${API_BASE}${path.startsWith('/') ? path : `/${path}`}`;
}

async function parseError(response: Response) {
  try {
    const body = await response.json() as ApiErrorBody;
    return new ApiError(response.status, body.code ?? 'UNKNOWN_ERROR', body.message ?? '요청을 처리하지 못했습니다.');
  } catch {
    return new ApiError(response.status, 'HTTP_ERROR', `요청을 처리하지 못했습니다. (${response.status})`);
  }
}

async function refreshAccessToken() {
  if (!reissuePromise) {
    reissuePromise = fetch(`${API_BASE}/api/auth/reissue`, {
      method: 'POST', credentials: 'include',
    }).then(async (response) => {
      if (!response.ok) return false;
      const data = await response.json() as { accessToken: string };
      setAccessToken(data.accessToken);
      return true;
    }).catch(() => false).finally(() => { reissuePromise = null; });
  }
  return reissuePromise;
}

async function request<T>(path: string, init: RequestInit = {}, retry = true): Promise<T> {
  const headers = new Headers(init.headers);
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`);
  if (init.body && !(init.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  const response = await fetch(`${API_BASE}${path}`, { ...init, headers, credentials: 'include' });
  if (response.status === 401 && retry && !path.startsWith('/api/auth/')) {
    if (await refreshAccessToken()) return request<T>(path, init, false);
  }
  if (!response.ok) throw await parseError(response);
  if (response.status === 204 || response.headers.get('content-length') === '0') return undefined as T;
  const text = await response.text();
  if (!text) return undefined as T;
  return response.headers.get('content-type')?.includes('application/json')
    ? JSON.parse(text) as T
    : text as T;
}

export const api = {
  restore: refreshAccessToken,
  signup: (email: string, password: string, nickName: string) => request<{ status: string }>('/api/auth/signup', {
    method: 'POST', body: JSON.stringify({ email, password, nick_name: nickName, role: 'USER' }),
  }),
  login: (email: string, password: string) => request<User & { accessToken: string }>('/api/auth/login', {
    method: 'POST', body: JSON.stringify({ email, password }),
  }),
  logout: () => request<void>('/api/auth/logout', { method: 'POST' }),
  boards: () => request<Board[]>('/api/board/all'),
  createBoard: (data: { name: string; description: string }) => request<Board>('/api/board/new', { method: 'POST', body: JSON.stringify(data) }),
  updateBoard: (id: number, data: { name: string; description: string }) => request<Board>(`/api/board/${id}/update`, { method: 'PUT', body: JSON.stringify(data) }),
  deleteBoard: (id: number) => request<string>(`/api/board/${id}`, { method: 'DELETE' }),
  posts: (boardId: number, page = 0) => request<Page<Post>>(`/api/post/${boardId}/all?page=${page}&size=10&sort=createdAt,desc`),
  allPosts: () => request<Post[]>('/api/post/all'),
  post: (id: number) => request<Post>(`/api/post/${id}`),
  createPost: (boardId: number, title: string, body: string, images: File[]) => {
    const form = new FormData();
    form.append('post', new Blob([JSON.stringify({ title, body })], { type: 'application/json' }));
    images.forEach((file) => form.append('images', file));
    return request<Post>(`/api/post/${boardId}/new`, { method: 'POST', body: form });
  },
  updatePost: (id: number, title: string, body: string) => request<Post>(`/api/post/${id}/update`, { method: 'PUT', body: JSON.stringify({ title, body }) }),
  deletePost: (id: number) => request<string>(`/api/post/${id}`, { method: 'DELETE' }),
  comments: (postId: number, page = 0) => request<Page<Comment>>(`/api/comment/post/${postId}/list?page=${page}&size=10&sort=createdAt,desc`),
  createComment: (postId: number, content: string, parentId: number | null) => request<Comment>(`/api/comment/posts/${postId}/new`, { method: 'POST', body: JSON.stringify({ content, parentId }) }),
  updateComment: (id: number, content: string) => request<Comment>(`/api/comment/${id}`, { method: 'PUT', body: JSON.stringify({ content, parentId: null }) }),
  deleteComment: (id: number) => request<void>(`/api/comment/${id}`, { method: 'DELETE' }),
  react: (postId: number, type: ReactionType) => request<ReactionResponse>(`/api/post/${postId}/reaction`, { method: 'POST', body: JSON.stringify({ type }) }),
  reactToComment: (commentId: number, type: ReactionType) => request<ReactionResponse>(`/api/comment/${commentId}/reaction`, { method: 'POST', body: JSON.stringify({ type }) }),
  notifications: (page = 0) => request<Page<Notification>>(`/api/notify/list?page=${page}&size=10&sort=createdAt,desc`),
  unreadCount: () => request<{ unreadCount: number }>('/api/notify/unreads'),
  readNotification: (id: number) => request<void>(`/api/notify/${id}/read`, { method: 'PUT' }),
  profile: () => request<Profile>('/api/user/me'),
};
