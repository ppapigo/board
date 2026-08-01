export type ReactionType = 'LIKE' | 'DISLIKE';
export type NotificationType = 'COMMENT_ON_POST' | 'REPLY_ON_COMMENT';

export interface Board { id: number; name: string; description: string | null; createdAt: string }
export interface PostImage { id: number; url: string; originalName: string | null; sortOrder: number }
export interface Post {
  id: number; title: string; author: string; board: string; body: string | null;
  viewCount: number; images: PostImage[]; like: number | null; dislike: number | null;
  myReaction: ReactionType | null; createdAt: string; canEdit: boolean; canDelete: boolean;
}
export interface Comment {
  id: number; authorUserName: string; content: string; parent: number | null;
  createdAt: string; deleted: boolean; children?: Comment[];
  likeCount?: number; dislikeCount?: number; myReaction?: ReactionType | null;
}
export interface ReactionResponse {
  likeCount: number; dislikeCount: number; myReaction: ReactionType | null;
}
export interface User {
  id: number; email: string; nickName: string; role: 'USER' | 'ADMIN' | null;
}
export interface Profile { nickName: string; phoneNumber: string | null; birth: string; createdAt: string }
export interface Notification {
  id: number; type: NotificationType; message: string; actorUsername: string;
  postId: number; commentId: number; read: boolean; createdAt: string;
}
export interface Page<T> {
  content: T[];
  page?: { size: number; number: number; totalElements: number; totalPages: number };
  number?: number; size?: number; totalElements?: number; totalPages?: number;
}
export interface ApiErrorBody { code: string; message: string; timestamp: string }

export function pageMeta<T>(page: Page<T>) {
  return page.page ?? {
    size: page.size ?? 10,
    number: page.number ?? 0,
    totalElements: page.totalElements ?? page.content.length,
    totalPages: page.totalPages ?? 1,
  };
}
