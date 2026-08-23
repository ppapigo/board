import { useCallback, useEffect, useState, type FormEvent, type ReactNode } from 'react';
import { Link, NavLink, Navigate, Route, Routes, useLocation, useNavigate, useParams } from 'react-router-dom';
import { ApiError, api, imageUrl } from './api';
import { useAuth } from './auth';
import type { Board, Comment, Notification, Page, Post, Profile, ReactionType } from './types';
import { pageMeta } from './types';

const date = new Intl.DateTimeFormat('ko-KR', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
const formatDate = (value: string) => { const parsed = new Date(value); return Number.isNaN(parsed.valueOf()) ? value : date.format(parsed); };
const messageOf = (error: unknown) => error instanceof ApiError ? error.message : '잠시 후 다시 시도해 주세요.';

function Icon({ name, size = 20 }: { name: 'logo' | 'home' | 'bell' | 'user' | 'write' | 'eye' | 'comment' | 'heart' | 'search' | 'arrow' | 'image' | 'trash' | 'edit'; size?: number }) {
  const paths: Record<string, ReactNode> = {
    logo: <><path d="M6 5.5h12v10H9l-3 3v-13Z"/><path d="M10 9h4M10 12h3"/></>,
    home: <><path d="m3 11 9-8 9 8"/><path d="M5 10v10h14V10M9 20v-6h6v6"/></>,
    bell: <><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9"/><path d="M10 21h4"/></>,
    user: <><circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/></>,
    write: <><path d="M12 20h9"/><path d="m16.5 3.5 4 4L9 19l-5 1 1-5Z"/></>,
    eye: <><path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/></>,
    comment: <path d="M21 15a4 4 0 0 1-4 4H8l-5 3V7a4 4 0 0 1 4-4h10a4 4 0 0 1 4 4Z"/>,
    heart: <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8l1.1 1.1L12 21l7.8-7.5 1.1-1.1a5.5 5.5 0 0 0-.1-7.8Z"/>,
    search: <><circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/></>,
    arrow: <><path d="m9 18 6-6-6-6"/></>,
    image: <><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="m21 15-5-5L5 21"/></>,
    trash: <><path d="M3 6h18M8 6V4h8v2M19 6l-1 15H6L5 6M10 11v6M14 11v6"/></>,
    edit: <><path d="M12 20h9"/><path d="m16.5 3.5 4 4L9 19l-5 1 1-5Z"/></>,
  };
  return <svg className="icon" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[name]}</svg>;
}

function Layout({ children }: { children: ReactNode }) {
  const { user, logout } = useAuth();
  const [unread, setUnread] = useState(0);
  useEffect(() => { if (user) api.unreadCount().then((r) => setUnread(r.unreadCount)).catch(() => undefined); }, [user]);
  return <div className="app-shell">
    <header className="site-header">
      <div className="header-inner">
        <Link to="/" className="brand" aria-label="모아 홈"><span className="brand-mark"><Icon name="logo" size={25}/></span><span>모아</span></Link>
        <nav className="primary-nav" aria-label="주요 메뉴">
          <NavLink to="/" end><Icon name="home"/>홈</NavLink>
          {user && <NavLink to="/notifications"><span className="nav-icon"><Icon name="bell"/>{unread > 0 && <b>{unread > 99 ? '99+' : unread}</b>}</span>알림</NavLink>}
          {user && <NavLink to="/me"><Icon name="user"/>내 정보</NavLink>}
        </nav>
        <div className="header-actions">
          {user ? <><span className="hello"><strong>{user.nickName}</strong> 님</span><button className="button ghost compact" onClick={() => void logout()}>로그아웃</button></>
            : <><Link className="button ghost compact" to="/login">로그인</Link><Link className="button dark compact" to="/signup">가입하기</Link></>}
        </div>
      </div>
    </header>
    <main>{children}</main>
    <footer><div><Link to="/" className="brand small"><span className="brand-mark"><Icon name="logo" size={19}/></span>모아</Link><p>좋은 이야기가 오래 머무는 커뮤니티</p></div><span>© 2026 MOA COMMUNITY</span></footer>
  </div>;
}

function PageState({ loading, error, empty, onRetry, children }: { loading?: boolean; error?: string; empty?: string; onRetry?: () => void; children: ReactNode }) {
  if (loading) return <div className="state"><span className="spinner"/><p>불러오는 중입니다</p></div>;
  if (error) return <div className="state error-state"><span>!</span><h3>문제가 생겼어요</h3><p>{error}</p>{onRetry && <button className="button outline" onClick={onRetry}>다시 시도</button>}</div>;
  if (empty) return <div className="state"><span className="empty-glyph">○</span><h3>{empty}</h3><p>첫 이야기를 남겨보세요.</p></div>;
  return <>{children}</>;
}

function HomePage() {
  const [boards, setBoards] = useState<Board[]>([]); const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true); const [error, setError] = useState(''); const [query, setQuery] = useState('');
  const load = useCallback(() => { setLoading(true); setError(''); Promise.all([api.boards(), api.allPosts()]).then(([b, p]) => { setBoards(b); setPosts(p.slice().reverse().slice(0, 6)); }).catch((e) => setError(messageOf(e))).finally(() => setLoading(false)); }, []);
  useEffect(load, [load]);
  const visible = boards.filter((board) => `${board.name} ${board.description ?? ''}`.toLowerCase().includes(query.toLowerCase()));
  return <>
    <section className="hero"><div className="hero-orb one"/><div className="hero-orb two"/><div className="hero-inner">
      <p className="eyebrow">MOA COMMUNITY</p><h1>생각을 나누면,<br/><em>이야기가 됩니다.</em></h1>
      <p className="hero-copy">관심사가 같은 사람들과 편안하게 대화해 보세요.<br/>당신의 이야기를 기다리고 있습니다.</p>
      <div className="search-box"><Icon name="search"/><input aria-label="게시판 검색" value={query} onChange={(e) => setQuery(e.target.value)} placeholder="어떤 이야기를 찾고 있나요?"/></div>
    </div></section>
    <div className="page home-content">
      <div className="section-heading"><div><p className="eyebrow green">EXPLORE</p><h2>이야기 둘러보기</h2></div><span>{visible.length}개의 게시판</span></div>
      <PageState loading={loading} error={error} empty={!loading && !error && visible.length === 0 ? '아직 게시판이 없습니다' : undefined} onRetry={load}>
        <div className="board-grid">{visible.map((board, i) => <Link className="board-card" to={`/boards/${board.id}`} key={board.id}>
          <span className={`board-number tone-${i % 4}`}>{String(i + 1).padStart(2, '0')}</span><div><h3>{board.name}</h3><p>{board.description || '새로운 이야기를 함께 시작해 보세요.'}</p></div><span className="circle-arrow"><Icon name="arrow"/></span>
        </Link>)}</div>
      </PageState>
      {posts.length > 0 && <section className="recent"><div className="section-heading"><div><p className="eyebrow green">RECENT</p><h2>새로 올라온 이야기</h2></div></div><div className="recent-list">{posts.map((post) => <PostRow post={post} key={post.id}/>)}</div></section>}
    </div>
  </>;
}

function PostRow({ post }: { post: Post }) {
  return <Link className="post-row" to={`/posts/${post.id}`}><div><span className="pill">{post.board}</span><h3>{post.title}</h3><p>{post.body || '내용이 없습니다.'}</p><div className="meta"><span>{post.author}</span><i/> <span>{formatDate(post.createdAt)}</span><i/> <span><Icon name="eye" size={15}/>{post.viewCount}</span></div></div>{post.images?.[0] && <img src={imageUrl(post.images[0].url)} alt={post.images[0].originalName || ''}/>}<Icon name="arrow"/></Link>;
}

function BoardPage() {
  const { boardId } = useParams(); const id = Number(boardId); const { user } = useAuth();
  const [board, setBoard] = useState<Board>(); const [page, setPage] = useState<Page<Post>>(); const [current, setCurrent] = useState(0); const [loading, setLoading] = useState(true); const [error, setError] = useState('');
  const load = useCallback(() => { setLoading(true); setError(''); Promise.all([api.boards(), api.posts(id, current)]).then(([boards, posts]) => { setBoard(boards.find((b) => b.id === id)); setPage(posts); }).catch((e) => setError(messageOf(e))).finally(() => setLoading(false)); }, [id, current]);
  useEffect(load, [load]); const meta = page ? pageMeta(page) : null;
  return <div className="page narrow"><div className="page-banner"><div><Link to="/" className="back-link">← 모든 게시판</Link><p className="eyebrow green">BOARD</p><h1>{board?.name || '게시판'}</h1><p>{board?.description || '함께 나누고 싶은 이야기를 올려주세요.'}</p></div>{user && <Link className="button accent" to={`/boards/${id}/new`}><Icon name="write"/>글쓰기</Link>}</div>
    <PageState loading={loading} error={error} empty={!loading && !error && !page?.content.length ? '아직 작성된 글이 없습니다' : undefined} onRetry={load}><div className="post-list">{page?.content.map((post) => <PostRow post={post} key={post.id}/>)}</div></PageState>
    {meta && meta.totalPages > 1 && <Pagination current={meta.number} total={meta.totalPages} onChange={setCurrent}/>}</div>;
}

function Pagination({ current, total, onChange }: { current: number; total: number; onChange: (page: number) => void }) {
  return <nav className="pagination" aria-label="페이지 이동"><button disabled={current === 0} onClick={() => onChange(current - 1)}>←</button>{Array.from({ length: total }, (_, i) => <button className={current === i ? 'active' : ''} onClick={() => onChange(i)} key={i}>{i + 1}</button>)}<button disabled={current + 1 >= total} onClick={() => onChange(current + 1)}>→</button></nav>;
}

function PostDetailPage() {
  const { postId } = useParams(); const id = Number(postId); const { user } = useAuth(); const navigate = useNavigate(); const location = useLocation();
  const [post, setPost] = useState<Post>(); const [comments, setComments] = useState<Comment[]>([]); const [loading, setLoading] = useState(true); const [error, setError] = useState(''); const [commentText, setCommentText] = useState(''); const [replyTo, setReplyTo] = useState<Comment>(); const [submitting, setSubmitting] = useState(false);
  const load = useCallback(() => { setLoading(true); setError(''); Promise.all([api.post(id), api.comments(id)]).then(([p, c]) => { setPost(p); setComments(c.content); }).catch((e) => setError(messageOf(e))).finally(() => setLoading(false)); }, [id]);
  useEffect(load, [load]);
  const requireLogin = () => { if (!user) { navigate('/login', { state: { from: location.pathname } }); return false; } return true; };
  const react = async (type: ReactionType) => { if (!requireLogin() || !post) return; try { const r = await api.react(id, type); setPost({ ...post, like: r.likeCount, dislike: r.dislikeCount, myReaction: r.myReaction }); } catch (e) { setError(messageOf(e)); } };
  const submitComment = async (event: FormEvent) => { event.preventDefault(); if (!requireLogin() || !commentText.trim()) return; setSubmitting(true); try { await api.createComment(id, commentText.trim(), replyTo?.id ?? null); setCommentText(''); setReplyTo(undefined); const result = await api.comments(id); setComments(result.content); } catch (e) { setError(messageOf(e)); } finally { setSubmitting(false); } };
  const remove = async () => { if (!post || !confirm('이 게시글을 삭제할까요?')) return; try { await api.deletePost(post.id); navigate(-1); } catch (e) { setError(messageOf(e)); } };
  return <div className="page article-page"><PageState loading={loading} error={error} onRetry={load}>
    {post && <article><Link to="/" className="back-link">← 목록으로</Link><header className="article-header"><span className="pill">{post.board}</span><h1>{post.title}</h1><div className="article-byline"><span className="avatar">{post.author.slice(0, 1)}</span><div><strong>{post.author}</strong><div className="meta"><span>{formatDate(post.createdAt)}</span><i/><span><Icon name="eye" size={15}/>{post.viewCount}</span></div></div><div className="article-tools">{post.canEdit && <Link to={`/posts/${post.id}/edit`}><Icon name="edit"/>수정</Link>}{post.canDelete && <button onClick={() => void remove()}><Icon name="trash"/>삭제</button>}</div></div></header>
      {post.images?.length > 0 && <div className={`image-gallery count-${Math.min(post.images.length, 3)}`}>{post.images.sort((a,b) => a.sortOrder-b.sortOrder).map((image) => <img key={image.id} src={imageUrl(image.url)} alt={image.originalName || '게시글 첨부 이미지'}/>)}</div>}
      <div className="article-body">{post.body?.split('\n').map((line, i) => <p key={i}>{line || <br/>}</p>)}</div>
      <div className="reaction-bar"><button className={post.myReaction === 'LIKE' ? 'selected' : ''} onClick={() => void react('LIKE')}><span>♡</span> 좋아요 <b>{post.like ?? 0}</b></button><button className={post.myReaction === 'DISLIKE' ? 'selected dislike' : ''} onClick={() => void react('DISLIKE')}><span>–</span> 아쉬워요 <b>{post.dislike ?? 0}</b></button></div>
      <section className="comments"><div className="comments-title"><h2>댓글 <em>{comments.length}</em></h2></div>
        <form className="comment-form" onSubmit={submitComment}>{replyTo && <div className="reply-label"><b>{replyTo.authorUserName}</b> 님에게 답글 작성 중 <button type="button" onClick={() => setReplyTo(undefined)}>취소</button></div>}<textarea maxLength={100} value={commentText} onChange={(e) => setCommentText(e.target.value)} placeholder={user ? '따뜻한 댓글을 남겨주세요.' : '로그인 후 댓글을 남길 수 있어요.'}/><div><span>{commentText.length}/100</span><button className="button dark compact" disabled={submitting || !commentText.trim()}>{submitting ? '등록 중…' : '댓글 등록'}</button></div></form>
        <div className="comment-list">{comments.length === 0 ? <div className="mini-empty">첫 댓글을 남겨보세요.</div> : comments.map((comment) => <CommentItem key={comment.id} comment={comment} currentName={user?.nickName} requireLogin={requireLogin} onReply={() => { if (requireLogin()) { setReplyTo(comment); document.querySelector<HTMLTextAreaElement>('.comment-form textarea')?.focus(); } }} onChanged={load}/>)}</div>
      </section></article>}
  </PageState></div>;
}

function CommentItem({ comment, currentName, requireLogin, onReply, onChanged, child = false }: { comment: Comment; currentName?: string; requireLogin: () => boolean; onReply: () => void; onChanged: () => void; child?: boolean }) {
  const [editing, setEditing] = useState(false); const [value, setValue] = useState(comment.content); const [reaction, setReaction] = useState({ likeCount: comment.likeCount ?? 0, dislikeCount: comment.dislikeCount ?? 0, myReaction: comment.myReaction ?? null as ReactionType | null }); const [reacting, setReacting] = useState(false); const mine = currentName === comment.authorUserName;
  const save = async () => { try { await api.updateComment(comment.id, value); setEditing(false); onChanged(); } catch (e) { alert(messageOf(e)); } };
  const remove = async () => { if (!confirm('댓글을 삭제할까요?')) return; try { await api.deleteComment(comment.id); onChanged(); } catch (e) { alert(messageOf(e)); } };
  const reactToComment = async (type: ReactionType) => { if (!requireLogin() || reacting) return; setReacting(true); try { setReaction(await api.reactToComment(comment.id, type)); } catch (e) { alert(messageOf(e)); } finally { setReacting(false); } };
  return <div className={`comment-item ${child ? 'child' : ''}`}><span className="avatar small-avatar">{comment.authorUserName?.slice(0, 1) || '?'}</span><div className="comment-content"><div><strong>{comment.authorUserName}</strong><span>{formatDate(comment.createdAt)}</span></div>{editing ? <div className="inline-edit"><textarea maxLength={100} value={value} onChange={(e) => setValue(e.target.value)}/><button onClick={() => void save()}>저장</button><button onClick={() => setEditing(false)}>취소</button></div> : <p className={comment.deleted ? 'deleted' : ''}>{comment.deleted ? '삭제된 댓글입니다.' : comment.content}</p>}<div className="comment-actions">{!comment.deleted && <><button className={reaction.myReaction === 'LIKE' ? 'selected' : ''} disabled={reacting} onClick={() => void reactToComment('LIKE')} aria-pressed={reaction.myReaction === 'LIKE'}>♡ 좋아요 <b>{reaction.likeCount}</b></button><button className={reaction.myReaction === 'DISLIKE' ? 'selected dislike' : ''} disabled={reacting} onClick={() => void reactToComment('DISLIKE')} aria-pressed={reaction.myReaction === 'DISLIKE'}>– 아쉬워요 <b>{reaction.dislikeCount}</b></button></>}{!child && !comment.deleted && <button onClick={onReply}>답글</button>}{mine && !comment.deleted && <><button onClick={() => setEditing(true)}>수정</button><button onClick={() => void remove()}>삭제</button></>}</div>{comment.children?.map((reply) => <CommentItem key={reply.id} child comment={reply} currentName={currentName} requireLogin={requireLogin} onReply={() => undefined} onChanged={onChanged}/>)}</div></div>;
}

function PostEditorPage({ edit = false }: { edit?: boolean }) {
  const params = useParams(); const boardId = Number(params.boardId); const postId = Number(params.postId); const navigate = useNavigate();
  const [title, setTitle] = useState(''); const [body, setBody] = useState(''); const [files, setFiles] = useState<File[]>([]); const [existing, setExisting] = useState<Post>(); const [error, setError] = useState(''); const [saving, setSaving] = useState(false);
  useEffect(() => { if (edit) api.post(postId).then((post) => { setExisting(post); setTitle(post.title); setBody(post.body ?? ''); }).catch((e) => setError(messageOf(e))); }, [edit, postId]);
  const selectFiles = (list: FileList | null) => { const next = Array.from(list ?? []); if (next.length > 5) { setError('이미지는 최대 5개까지 첨부할 수 있어요.'); return; } const invalid = next.find((f) => !['image/jpeg','image/png','image/gif','image/webp'].includes(f.type) || f.size > 2 * 1024 * 1024); if (invalid) { setError('JPG, PNG, GIF, WEBP 이미지만 파일당 2MB까지 첨부할 수 있어요.'); return; } setError(''); setFiles(next); };
  const submit = async (event: FormEvent) => { event.preventDefault(); if (title.trim().length < 5) { setError('제목을 5자 이상 입력해 주세요.'); return; } if (!body.trim()) { setError('본문을 입력해 주세요.'); return; } setSaving(true); try { const result = edit ? await api.updatePost(postId, title.trim(), body.trim()) : await api.createPost(boardId, title.trim(), body.trim(), files); navigate(`/posts/${result.id}`); } catch (e) { setError(messageOf(e)); } finally { setSaving(false); } };
  return <div className="page form-page"><div className="form-heading"><p className="eyebrow green">{edit ? 'EDIT STORY' : 'NEW STORY'}</p><h1>{edit ? '이야기 다듬기' : '새 이야기 쓰기'}</h1><p>당신의 생각을 천천히 들려주세요.</p></div><form className="editor-card" onSubmit={submit}><label>제목 <span>{title.length}/200</span><input required minLength={5} maxLength={200} value={title} onChange={(e) => setTitle(e.target.value)} placeholder="어떤 이야기를 나누고 싶나요?"/></label><label>내용 <textarea required value={body} onChange={(e) => setBody(e.target.value)} placeholder="내용을 입력해 주세요."/></label>{!edit && <label className="upload-zone"><input type="file" accept="image/jpeg,image/png,image/gif,image/webp" multiple onChange={(e) => selectFiles(e.target.files)}/><Icon name="image" size={28}/><strong>사진을 여기에 놓거나 눌러 선택하세요</strong><span>JPG, PNG, GIF, WEBP · 각 2MB 이하 · 최대 5장</span></label>}{edit && existing?.images?.length ? <p className="notice">첨부 이미지 수정은 현재 지원되지 않아 기존 이미지가 유지됩니다.</p> : null}{files.length > 0 && <div className="file-list">{files.map((file, i) => <div key={`${file.name}-${i}`}><span>{file.name}</span><button type="button" onClick={() => setFiles(files.filter((_, n) => n !== i))}>제거</button></div>)}</div>}{error && <p className="form-error" role="alert">{error}</p>}<div className="form-actions"><button type="button" className="button ghost" onClick={() => navigate(-1)}>취소</button><button className="button dark" disabled={saving}>{saving ? '저장 중…' : edit ? '수정 완료' : '이야기 올리기'}</button></div></form></div>;
}

function AuthPage({ signup = false }: { signup?: boolean }) {
  const { login } = useAuth(); const navigate = useNavigate(); const [email, setEmail] = useState(''); const [password, setPassword] = useState(''); const [nickName, setNickName] = useState(''); const [error, setError] = useState(''); const [busy, setBusy] = useState(false);
  const submit = async (event: FormEvent) => { event.preventDefault(); setBusy(true); setError(''); try { if (signup) await api.signup(email, password, nickName); await login(email, password); navigate('/', { replace: true }); } catch (e) { setError(messageOf(e)); } finally { setBusy(false); } };
  return <div className="auth-layout"><section className="auth-art"><Link className="brand light" to="/"><span className="brand-mark"><Icon name="logo" size={25}/></span>모아</Link><div><p className="eyebrow">WELCOME TO MOA</p><h1>{signup ? <>당신의 첫 이야기를<br/>기다리고 있어요.</> : <>다시 만나<br/>반가워요.</>}</h1><p>서로의 생각이 모여 더 넓은 세상이 됩니다.</p></div><blockquote>“좋은 대화는 작은 호기심에서 시작됩니다.”</blockquote></section><section className="auth-panel"><div className="auth-card"><p className="eyebrow green">{signup ? 'JOIN US' : 'SIGN IN'}</p><h2>{signup ? '회원가입' : '로그인'}</h2><p>{signup ? '모아에서 새로운 이야기를 시작하세요.' : '이메일로 모아에 들어오세요.'}</p><form onSubmit={submit}>{signup && <label>닉네임<input value={nickName} onChange={(e) => setNickName(e.target.value)} required placeholder="어떻게 불러드릴까요?"/></label>}<label>이메일<input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required placeholder="name@example.com"/></label><label>비밀번호<input type="password" minLength={8} maxLength={30} value={password} onChange={(e) => setPassword(e.target.value)} required placeholder="8자 이상 입력해 주세요"/></label>{error && <p className="form-error" role="alert">{error}</p>}<button className="button accent full" disabled={busy}>{busy ? '잠시만요…' : signup ? '가입하고 시작하기' : '로그인'}</button></form><div className="divider"><span>또는</span></div><div className="social-row"><a className="button social kakao" href="/oauth2/authorization/kakao">Kakao</a><a className="button social" href="/oauth2/authorization/google">Google</a></div><p className="auth-switch">{signup ? '이미 계정이 있나요?' : '아직 계정이 없나요?'} <Link to={signup ? '/login' : '/signup'}>{signup ? '로그인' : '회원가입'}</Link></p></div></section></div>;
}

function NotificationsPage() {
  const navigate = useNavigate(); const [items, setItems] = useState<Notification[]>([]); const [loading, setLoading] = useState(true); const [error, setError] = useState('');
  const load = useCallback(() => { setLoading(true); api.notifications().then((p) => setItems(p.content)).catch((e) => setError(messageOf(e))).finally(() => setLoading(false)); }, []); useEffect(load, [load]);
  const open = async (item: Notification) => { if (!item.read) { try { await api.readNotification(item.id); } catch { /* navigation remains useful */ } } navigate(`/posts/${item.postId}`); };
  return <div className="page narrow"><div className="simple-heading"><p className="eyebrow green">NOTIFICATIONS</p><h1>새로운 소식</h1><p>내 이야기에 이어진 대화를 확인하세요.</p></div><PageState loading={loading} error={error} empty={!items.length ? '새로운 알림이 없습니다' : undefined} onRetry={load}><div className="notification-list">{items.map((item) => <button onClick={() => void open(item)} className={item.read ? 'read' : ''} key={item.id}><span className="avatar">{item.actorUsername.slice(0,1)}</span><div><p>{item.message}</p><span>{formatDate(item.createdAt)}</span></div>{!item.read && <i/>}<Icon name="arrow"/></button>)}</div></PageState></div>;
}

function ProfilePage() {
  const { user } = useAuth(); const [profile, setProfile] = useState<Profile>(); const [error, setError] = useState(''); useEffect(() => { api.profile().then(setProfile).catch((e) => setError(messageOf(e))); }, []);
  return <div className="page profile-page"><div className="profile-card"><div className="profile-cover"/><div className="profile-identity"><span className="avatar profile-avatar">{profile?.nickName?.slice(0,1) || '?'}</span><div><p className="eyebrow green">MY PROFILE</p><h1>{profile?.nickName || '내 정보'}</h1><span>{user?.email}</span></div></div>{error ? <p className="form-error">{error}</p> : <dl><div><dt>닉네임</dt><dd>{profile?.nickName || '—'}</dd></div><div><dt>전화번호</dt><dd>{profile?.phoneNumber || '등록되지 않음'}</dd></div><div><dt>생년월일</dt><dd>{profile?.birth || '등록되지 않음'}</dd></div><div><dt>함께한 날</dt><dd>{profile?.createdAt ? formatDate(profile.createdAt) : '—'}</dd></div></dl>}</div></div>;
}

function AdminBoardsPage() {
  const [boards, setBoards] = useState<Board[]>([]); const [editing, setEditing] = useState<Board>(); const [name, setName] = useState(''); const [description, setDescription] = useState(''); const [error, setError] = useState('');
  const load = useCallback(() => { api.boards().then(setBoards).catch((e) => setError(messageOf(e))); }, []); useEffect(load, [load]);
  const submit = async (e: FormEvent) => { e.preventDefault(); try { if (editing) await api.updateBoard(editing.id, { name, description }); else await api.createBoard({ name, description }); setEditing(undefined); setName(''); setDescription(''); load(); } catch (err) { setError(messageOf(err)); } };
  const edit = (board: Board) => { setEditing(board); setName(board.name); setDescription(board.description ?? ''); };
  const remove = async (board: Board) => { if (!confirm(`'${board.name}' 게시판을 삭제할까요?`)) return; try { await api.deleteBoard(board.id); load(); } catch (e) { setError(messageOf(e)); } };
  return <div className="page narrow"><div className="simple-heading"><p className="eyebrow green">ADMIN</p><h1>게시판 관리</h1></div><div className="admin-layout"><form className="admin-form" onSubmit={submit}><h2>{editing ? '게시판 수정' : '새 게시판'}</h2><label>이름<input required value={name} onChange={(e) => setName(e.target.value)}/></label><label>설명<textarea value={description} onChange={(e) => setDescription(e.target.value)}/></label>{error && <p className="form-error">{error}</p>}<button className="button dark">{editing ? '수정 저장' : '게시판 만들기'}</button>{editing && <button type="button" className="button ghost" onClick={() => { setEditing(undefined); setName(''); setDescription(''); }}>취소</button>}</form><div className="admin-list">{boards.map((board) => <div key={board.id}><div><strong>{board.name}</strong><p>{board.description}</p></div><button onClick={() => edit(board)}><Icon name="edit"/></button><button onClick={() => void remove(board)}><Icon name="trash"/></button></div>)}</div></div></div>;
}

function Protected({ children, admin = false }: { children: ReactNode; admin?: boolean }) {
  const { user, restoring } = useAuth(); const location = useLocation();
  if (restoring) return <div className="state fullscreen"><span className="spinner"/></div>;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }}/>;
  if (admin && user.role !== 'ADMIN') return <Navigate to="/" replace/>;
  return <>{children}</>;
}

function NotFoundPage() { return <div className="state fullscreen"><span className="empty-glyph">404</span><h1>길을 잃었어요</h1><p>찾으시는 페이지가 없거나 이동했습니다.</p><Link className="button dark" to="/">홈으로 가기</Link></div>; }

export default function App() {
  return <Routes>
    <Route path="/login" element={<AuthPage/>}/><Route path="/signup" element={<AuthPage signup/>}/>
    <Route path="*" element={<Layout><Routes>
      <Route path="/" element={<HomePage/>}/><Route path="/boards/:boardId" element={<BoardPage/>}/><Route path="/posts/:postId" element={<PostDetailPage/>}/>
      <Route path="/boards/:boardId/new" element={<Protected><PostEditorPage/></Protected>}/><Route path="/posts/:postId/edit" element={<Protected><PostEditorPage edit/></Protected>}/>
      <Route path="/notifications" element={<Protected><NotificationsPage/></Protected>}/><Route path="/me" element={<Protected><ProfilePage/></Protected>}/><Route path="/admin/boards" element={<Protected admin><AdminBoardsPage/></Protected>}/>
      <Route path="*" element={<NotFoundPage/>}/>
    </Routes></Layout>}/>
  </Routes>;
}
