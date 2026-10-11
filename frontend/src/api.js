// @ts-ignore
const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');
let accessToken = null;
let reissuePromise = null;
export class ApiError extends Error {
    status;
    code;
    constructor(status, code, message) {
        super(message);
        this.status = status;
        this.code = code;
        this.name = 'ApiError';
    }
}
export function setAccessToken(token) {
    accessToken = token ? token.replace(/^Bearer\s+/i, '') : null;
}
export function getAccessToken() {
    return accessToken;
}
export function websocketUrl() {
    const configured = (import.meta.env.VITE_WS_BASE_URL ?? '').replace(/\/$/, '');
    if (configured)
        return `${configured}/ws`;
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    return `${protocol}//${window.location.host}/ws`;
}
export function imageUrl(path) {
    if (!path)
        return '';
    if (/^https?:\/\//.test(path))
        return path;
    return `${API_BASE}${path.startsWith('/') ? path : `/${path}`}`;
}
async function parseError(response) {
    try {
        const body = await response.json();
        return new ApiError(response.status, body.code ?? 'UNKNOWN_ERROR', body.message ?? '요청을 처리하지 못했습니다.');
    }
    catch {
        return new ApiError(response.status, 'HTTP_ERROR', `요청을 처리하지 못했습니다. (${response.status})`);
    }
}
async function refreshAccessToken() {
    if (!reissuePromise) {
        reissuePromise = fetch(`${API_BASE}/api/auth/reissue`, {
            method: 'POST', credentials: 'include',
        }).then(async (response) => {
            if (!response.ok)
                return null;
            const data = await response.json();
            setAccessToken(data.accessToken);
            return data;
        }).catch(() => null).finally(() => { reissuePromise = null; });
    }
    return reissuePromise;
}
async function request(path, init = {}, retry = true) {
    const headers = new Headers(init.headers);
    if (accessToken)
        headers.set('Authorization', `Bearer ${accessToken}`);
    if (init.body && !(init.body instanceof FormData) && !headers.has('Content-Type')) {
        headers.set('Content-Type', 'application/json');
    }
    const response = await fetch(`${API_BASE}${path}`, { ...init, headers, credentials: 'include' });
    if (response.status === 401 && retry && !path.startsWith('/api/auth/')) {
        if (await refreshAccessToken())
            return request(path, init, false);
    }
    if (!response.ok)
        throw await parseError(response);
    if (response.status === 204 || response.headers.get('content-length') === '0')
        return undefined;
    const text = await response.text();
    if (!text)
        return undefined;
    return response.headers.get('content-type')?.includes('application/json')
        ? JSON.parse(text)
        : text;
}
export const api = {
    restore: refreshAccessToken,
    signup: (email, password, nickName) => request('/api/auth/signup', {
        method: 'POST', body: JSON.stringify({ email, password, nick_name: nickName, role: 'USER' }),
    }),
    login: (email, password) => request('/api/auth/login', {
        method: 'POST', body: JSON.stringify({ email, password }),
    }),
    logout: () => request('/api/auth/logout', { method: 'POST' }),
    boards: () => request('/api/board/all'),
    createBoard: (data) => request('/api/board/new', { method: 'POST', body: JSON.stringify(data) }),
    updateBoard: (id, data) => request(`/api/board/${id}/update`, { method: 'PUT', body: JSON.stringify(data) }),
    deleteBoard: (id) => request(`/api/board/${id}`, { method: 'DELETE' }),
    posts: (boardId, page = 0) => request(`/api/post/${boardId}/all?page=${page}&size=10&sort=createdAt,desc`),
    pagedPosts: (boardId, page = 0) => request(`/api/post/${boardId}/page?page=${page}&size=10&sort=createdAt,desc`),
    post: (id) => request(`/api/post/${id}`),
    createPost: (boardId, title, body, images) => {
        const form = new FormData();
        form.append('post', new Blob([JSON.stringify({ title, body })], { type: 'application/json' }));
        images.forEach((file) => form.append('images', file));
        return request(`/api/post/${boardId}/new`, { method: 'POST', body: form });
    },
    updatePost: (id, title, body) => request(`/api/post/${id}/update`, { method: 'PUT', body: JSON.stringify({ title, body }) }),
    deletePost: (id) => request(`/api/post/${id}`, { method: 'DELETE' }),
    comments: (postId, page = 0) => request(`/api/comment/post/${postId}/list?page=${page}&size=10&sort=createdAt,desc`),
    createComment: (postId, content, parentId) => request(`/api/comment/posts/${postId}/new`, { method: 'POST', body: JSON.stringify({ content, parentId }) }),
    updateComment: (id, content) => request(`/api/comment/${id}`, { method: 'PUT', body: JSON.stringify({ content, parentId: null }) }),
    deleteComment: (id) => request(`/api/comment/${id}`, { method: 'DELETE' }),
    react: (postId, type) => request(`/api/post/${postId}/reaction`, { method: 'POST', body: JSON.stringify({ type }) }),
    reactToComment: (commentId, type) => request(`/api/comment/${commentId}/reaction`, { method: 'POST', body: JSON.stringify({ type }) }),
    notifications: (page = 0) => request(`/api/notify/list?page=${page}&size=10&sort=createdAt,desc`),
    unreadCount: () => request('/api/notify/unreads'),
    readNotification: (id) => request(`/api/notify/${id}/read`, { method: 'PUT' }),
    profile: () => request('/api/user/me'),
    chatRooms: (page = 0) => request(`/api/chat/rooms?page=${page}&size=50&sort=createdAt,desc`),
    chatRoom: (id) => request(`/api/chat/rooms/${id}`),
    createChatRoom: (data) => request('/api/chat/rooms', { method: 'POST', body: JSON.stringify(data) }),
    joinChatRoom: (id) => request(`/api/chat/rooms/${id}/join`, { method: 'POST' }),
    leaveChatRoom: (id) => request(`/api/chat/rooms/${id}/leave`, { method: 'DELETE' }),
    deleteChatRoom: (id) => request(`/api/chat/rooms/${id}`, { method: 'DELETE' }),
    chatMembers: (id) => request(`/api/chat/rooms/${id}/members`),
    chatMessages: (id, before) => request(`/api/chat/rooms/${id}/messages?size=50${before ? `&before=${before}` : ''}`),
};
