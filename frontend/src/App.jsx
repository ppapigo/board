import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, NavLink, Navigate, Route, Routes, useLocation, useNavigate, useParams } from 'react-router-dom';
import { ApiError, api, imageUrl } from './api';
import { getAccessToken, websocketUrl } from './api';
import { useAuth } from './auth';
import { pageMeta } from './types';
import { Client } from '@stomp/stompjs';
const date = new Intl.DateTimeFormat('ko-KR', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
const formatDate = (value) => { const parsed = new Date(value); return Number.isNaN(parsed.valueOf()) ? value : date.format(parsed); };
const messageOf = (error) => error instanceof ApiError || error instanceof Error ? error.message : '잠시 후 다시 시도해 주세요.';
function Icon({ name, size = 20 }) {
    const paths = {
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
        chat: <><path d="M21 12a8 8 0 0 1-8 8H6l-4 2 1.5-5A9 9 0 1 1 21 12Z"/><path d="M8 12h.01M12 12h.01M16 12h.01"/></>,
        live: <><path d="M8.5 8.5a5 5 0 0 0 0 7M5.5 5.5a9 9 0 0 0 0 13M15.5 8.5a5 5 0 0 1 0 7M18.5 5.5a9 9 0 0 1 0 13"/><circle cx="12" cy="12" r="2"/></>,
    };
    return <svg className="icon" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[name]}</svg>;
}
function Layout({ children }) {
    const { user, logout } = useAuth();
    const [unread, setUnread] = useState(0);
    useEffect(() => { if (user)
        api.unreadCount().then((r) => setUnread(r.unreadCount)).catch(() => undefined); }, [user]);
    return <div className="app-shell">
    <header className="site-header">
      <div className="header-inner">
        <Link to="/" className="brand" aria-label="모아 홈"><span className="brand-mark"><Icon name="logo" size={25}/></span><span>모아</span></Link>
        <nav className="primary-nav" aria-label="주요 메뉴">
          <NavLink to="/" end><Icon name="home"/>홈</NavLink>
          {user && <NavLink to="/chat"><Icon name="chat"/>채팅</NavLink>}
          {user && <NavLink to="/live"><Icon name="live"/>라이브</NavLink>}
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
function PageState({ loading, error, empty, onRetry, children }) {
    if (loading)
        return <div className="state"><span className="spinner"/><p>불러오는 중입니다</p></div>;
    if (error)
        return <div className="state error-state"><span>!</span><h3>문제가 생겼어요</h3><p>{error}</p>{onRetry && <button className="button outline" onClick={onRetry}>다시 시도</button>}</div>;
    if (empty)
        return <div className="state"><span className="empty-glyph">○</span><h3>{empty}</h3><p>첫 이야기를 남겨보세요.</p></div>;
    return <>{children}</>;
}
function HomePage() {
    const [boards, setBoards] = useState([]);
    const [posts, setPosts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [query, setQuery] = useState('');
    const load = useCallback(() => {
        setLoading(true);
        setError('');
        api.boards()
            .then(async (boards) => {
            const pages = await Promise.all(boards.map((board) => api.posts(board.id)));
            const recentPosts = pages
                .flatMap((page) => page.content)
                .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
                .slice(0, 6);
            setBoards(boards);
            setPosts(recentPosts);
        })
            .catch((e) => setError(messageOf(e)))
            .finally(() => setLoading(false));
    }, []);
    useEffect(load, [load]);
    const visible = boards.filter((board) => `${board.name} ${board.description ?? ''}`.toLowerCase().includes(query.toLowerCase()));
    return <>
    <section className="hero"><div className="hero-orb one"/><div className="hero-orb two"/><div className="hero-inner">
      <p className="eyebrow">MOA COMMUNITY</p><h1>생각을 나누면,<br /><em>이야기가 됩니다.</em></h1>
      <p className="hero-copy">관심사가 같은 사람들과 편안하게 대화해 보세요.<br />당신의 이야기를 기다리고 있습니다.</p>
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
function PostRow({ post }) {
    return <Link className="post-row" to={`/posts/${post.id}`}><div><span className="pill">{post.board}</span><h3>{post.title}</h3><p>{post.body || '내용이 없습니다.'}</p><div className="meta"><span>{post.author}</span><i /> <span>{formatDate(post.createdAt)}</span><i /> <span><Icon name="eye" size={15}/>{post.viewCount}</span></div></div>{post.images?.[0] && <img src={imageUrl(post.images[0].url)} alt={post.images[0].originalName || ''}/>}<Icon name="arrow"/></Link>;
}
function BoardPage() {
    const { boardId } = useParams();
    const id = Number(boardId);
    const { user } = useAuth();
    const [board, setBoard] = useState();
    const [posts, setPosts] = useState([]);
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [hasNext, setHasNext] = useState(true);
    const [loading, setLoading] = useState(true);
    const [loadingMore, setLoadingMore] = useState(false);
    const [error, setError] = useState('');
    const sentinelRef = useRef(null);
    const loadingRef = useRef(false);
    const nextPageRef = useRef(0);
    const generationRef = useRef(0);
    const loadPage = useCallback(async (pageNumber, reset = false) => {
        if (loadingRef.current)
            return;
        loadingRef.current = true;
        reset ? setLoading(true) : setLoadingMore(true);
        setError('');
        const generation = generationRef.current;
        try {
            const result = await api.pagedPosts(id, pageNumber);
            const meta = pageMeta(result);
            if (generation !== generationRef.current)
                return;
            setPosts((current) => reset ? result.content : [...current, ...result.content.filter((item) => !current.some((post) => post.id === item.id))]);
            setCurrentPage(meta.number);
            setTotalPages(meta.totalPages);
            nextPageRef.current = meta.number + 1;
            setHasNext(nextPageRef.current < meta.totalPages);
        }
        catch (e) {
            if (generation === generationRef.current)
                setError(messageOf(e));
        }
        finally {
            if (generation === generationRef.current) {
                loadingRef.current = false;
                setLoading(false);
                setLoadingMore(false);
            }
        }
    }, [id]);
    useEffect(() => {
        generationRef.current += 1;
        loadingRef.current = false;
        nextPageRef.current = 0;
        setPosts([]);
        setCurrentPage(0);
        setTotalPages(0);
        setHasNext(true);
        setError('');
        api.boards().then((boards) => setBoard(boards.find((b) => b.id === id))).catch((e) => setError(messageOf(e)));
        void loadPage(0, true);
    }, [id, loadPage]);
    useEffect(() => {
        const sentinel = sentinelRef.current;
        if (!sentinel || loading || !hasNext)
            return;
        const observer = new IntersectionObserver(([entry]) => { if (entry.isIntersecting)
            void loadPage(nextPageRef.current); }, { rootMargin: '300px' });
        observer.observe(sentinel);
        return () => observer.disconnect();
    }, [hasNext, loadPage, loading]);
    return <div className="page narrow"><div className="page-banner"><div><Link to="/" className="back-link">← 모든 게시판</Link><p className="eyebrow green">BOARD</p><h1>{board?.name || '게시판'}</h1><p>{board?.description || '함께 나누고 싶은 이야기를 올려주세요.'}</p></div>{user && <Link className="button accent" to={`/boards/${id}/new`}><Icon name="write"/>글쓰기</Link>}</div>
    <PageState loading={loading} error={!posts.length ? error : ''} empty={!loading && !error && !posts.length ? '아직 작성된 글이 없습니다' : undefined} onRetry={() => void loadPage(0, true)}><div className="post-list">{posts.map((post) => <Link className="post-row" to={`/posts/${post.id}`} key={post.id}><div><h3>{post.title}</h3><div className="meta"><span>{post.author}</span><i /><span>{formatDate(post.createdAt)}</span><i /><span><Icon name="eye" size={15}/>{post.viewCount}</span></div></div>{post.thumbnailUrl && <img src={imageUrl(post.thumbnailUrl)} alt=""/>}<Icon name="arrow"/></Link>)}</div></PageState>
    {error && posts.length > 0 && <div className="infinite-status error-state"><p>{error}</p><button className="button outline compact" onClick={() => void loadPage(nextPageRef.current)}>다시 시도</button></div>}
    {loadingMore && <div className="infinite-status"><span className="spinner"/><p>더 불러오는 중입니다</p></div>}
    {!loading && hasNext && <div ref={sentinelRef} className="scroll-sentinel" aria-hidden="true"/>}
    {!loading && !hasNext && posts.length > 0 && <p className="infinite-end">모든 게시글을 불러왔습니다.</p>}
    {!loading && totalPages > 1 && <Pagination current={currentPage} total={totalPages} onChange={async (page) => { await loadPage(page, true); window.scrollTo({ top: 0, behavior: 'smooth' }); }}/>}</div>;
}
function Pagination({ current, total, onChange }) {
    const start = Math.floor(current / 5) * 5;
    const pages = Array.from({ length: Math.min(5, total - start) }, (_, index) => start + index);
    return <nav className="pagination" aria-label="페이지 이동"><button disabled={current === 0} onClick={() => void onChange(current - 1)} aria-label="이전 페이지">←</button>{pages.map((page) => <button className={current === page ? 'active' : ''} onClick={() => void onChange(page)} aria-current={current === page ? 'page' : undefined} key={page}>{page + 1}</button>)}<button disabled={current + 1 >= total} onClick={() => void onChange(current + 1)} aria-label="다음 페이지">→</button></nav>;
}
function PostDetailPage() {
    const { postId } = useParams();
    const id = Number(postId);
    const { user } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();
    const [post, setPost] = useState();
    const [comments, setComments] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [commentText, setCommentText] = useState('');
    const [replyTo, setReplyTo] = useState();
    const [submitting, setSubmitting] = useState(false);
    const load = useCallback(() => { setLoading(true); setError(''); Promise.all([api.post(id), api.comments(id)]).then(([p, c]) => { setPost(p); setComments(c.content); }).catch((e) => setError(messageOf(e))).finally(() => setLoading(false)); }, [id]);
    useEffect(load, [load]);
    const requireLogin = () => { if (!user) {
        navigate('/login', { state: { from: location.pathname } });
        return false;
    } return true; };
    const react = async (type) => { if (!requireLogin() || !post)
        return; try {
        const r = await api.react(id, type);
        setPost({ ...post, like: r.likeCount, dislike: r.dislikeCount, myReaction: r.myReaction });
    }
    catch (e) {
        setError(messageOf(e));
    } };
    const submitComment = async (event) => { event.preventDefault(); if (!requireLogin() || !commentText.trim())
        return; setSubmitting(true); try {
        await api.createComment(id, commentText.trim(), replyTo?.id ?? null);
        setCommentText('');
        setReplyTo(undefined);
        const result = await api.comments(id);
        setComments(result.content);
    }
    catch (e) {
        setError(messageOf(e));
    }
    finally {
        setSubmitting(false);
    } };
    const remove = async () => { if (!post || !confirm('이 게시글을 삭제할까요?'))
        return; try {
        await api.deletePost(post.id);
        navigate(-1);
    }
    catch (e) {
        setError(messageOf(e));
    } };
    return <div className="page article-page"><PageState loading={loading} error={error} onRetry={load}>
    {post && <article><Link to="/" className="back-link">← 목록으로</Link><header className="article-header"><span className="pill">{post.board}</span><h1>{post.title}</h1><div className="article-byline"><span className="avatar">{post.author.slice(0, 1)}</span><div><strong>{post.author}</strong><div className="meta"><span>{formatDate(post.createdAt)}</span><i /><span><Icon name="eye" size={15}/>{post.viewCount}</span></div></div><div className="article-tools">{post.canEdit && <Link to={`/posts/${post.id}/edit`}><Icon name="edit"/>수정</Link>}{post.canDelete && <button onClick={() => void remove()}><Icon name="trash"/>삭제</button>}</div></div></header>
      {post.images?.length > 0 && <div className={`image-gallery count-${Math.min(post.images.length, 3)}`}>{post.images.sort((a, b) => a.sortOrder - b.sortOrder).map((image) => <img key={image.id} src={imageUrl(image.url)} alt={image.originalName || '게시글 첨부 이미지'}/>)}</div>}
      <div className="article-body">{post.body?.split('\n').map((line, i) => <p key={i}>{line || <br />}</p>)}</div>
      <div className="reaction-bar"><button className={post.myReaction === 'LIKE' ? 'selected' : ''} onClick={() => void react('LIKE')}><span>♡</span> 좋아요 <b>{post.like ?? 0}</b></button><button className={post.myReaction === 'DISLIKE' ? 'selected dislike' : ''} onClick={() => void react('DISLIKE')}><span>–</span> 아쉬워요 <b>{post.dislike ?? 0}</b></button></div>
      <section className="comments"><div className="comments-title"><h2>댓글 <em>{comments.length}</em></h2></div>
        <form className="comment-form" onSubmit={submitComment}>{replyTo && <div className="reply-label"><b>{replyTo.authorUserName}</b> 님에게 답글 작성 중 <button type="button" onClick={() => setReplyTo(undefined)}>취소</button></div>}<textarea maxLength={100} value={commentText} onChange={(e) => setCommentText(e.target.value)} placeholder={user ? '따뜻한 댓글을 남겨주세요.' : '로그인 후 댓글을 남길 수 있어요.'}/><div><span>{commentText.length}/100</span><button className="button dark compact" disabled={submitting || !commentText.trim()}>{submitting ? '등록 중…' : '댓글 등록'}</button></div></form>
        <div className="comment-list">{comments.length === 0 ? <div className="mini-empty">첫 댓글을 남겨보세요.</div> : comments.map((comment) => <CommentItem key={comment.id} comment={comment} currentName={user?.nickName} requireLogin={requireLogin} onReply={() => { if (requireLogin()) {
            setReplyTo(comment);
            document.querySelector('.comment-form textarea')?.focus();
        } }} onChanged={load}/>)}</div>
      </section></article>}
  </PageState></div>;
}
function CommentItem({ comment, currentName, requireLogin, onReply, onChanged, child = false }) {
    const [editing, setEditing] = useState(false);
    const [value, setValue] = useState(comment.content);
    const [reaction, setReaction] = useState({ likeCount: comment.likeCount ?? 0, dislikeCount: comment.dislikeCount ?? 0, myReaction: comment.myReaction ?? null });
    const [reacting, setReacting] = useState(false);
    const mine = currentName === comment.authorUserName;
    const save = async () => { try {
        await api.updateComment(comment.id, value);
        setEditing(false);
        onChanged();
    }
    catch (e) {
        alert(messageOf(e));
    } };
    const remove = async () => { if (!confirm('댓글을 삭제할까요?'))
        return; try {
        await api.deleteComment(comment.id);
        onChanged();
    }
    catch (e) {
        alert(messageOf(e));
    } };
    const reactToComment = async (type) => { if (!requireLogin() || reacting)
        return; setReacting(true); try {
        setReaction(await api.reactToComment(comment.id, type));
    }
    catch (e) {
        alert(messageOf(e));
    }
    finally {
        setReacting(false);
    } };
    return <div className={`comment-item ${child ? 'child' : ''}`}><span className="avatar small-avatar">{comment.authorUserName?.slice(0, 1) || '?'}</span><div className="comment-content"><div><strong>{comment.authorUserName}</strong><span>{formatDate(comment.createdAt)}</span></div>{editing ? <div className="inline-edit"><textarea maxLength={100} value={value} onChange={(e) => setValue(e.target.value)}/><button onClick={() => void save()}>저장</button><button onClick={() => setEditing(false)}>취소</button></div> : <p className={comment.deleted ? 'deleted' : ''}>{comment.deleted ? '삭제된 댓글입니다.' : comment.content}</p>}<div className="comment-actions">{!comment.deleted && <><button className={reaction.myReaction === 'LIKE' ? 'selected' : ''} disabled={reacting} onClick={() => void reactToComment('LIKE')} aria-pressed={reaction.myReaction === 'LIKE'}>♡ 좋아요 <b>{reaction.likeCount}</b></button><button className={reaction.myReaction === 'DISLIKE' ? 'selected dislike' : ''} disabled={reacting} onClick={() => void reactToComment('DISLIKE')} aria-pressed={reaction.myReaction === 'DISLIKE'}>– 아쉬워요 <b>{reaction.dislikeCount}</b></button></>}{!child && !comment.deleted && <button onClick={onReply}>답글</button>}{mine && !comment.deleted && <><button onClick={() => setEditing(true)}>수정</button><button onClick={() => void remove()}>삭제</button></>}</div>{comment.children?.map((reply) => <CommentItem key={reply.id} child comment={reply} currentName={currentName} requireLogin={requireLogin} onReply={() => undefined} onChanged={onChanged}/>)}</div></div>;
}
function PostEditorPage({ edit = false }) {
    const params = useParams();
    const boardId = Number(params.boardId);
    const postId = Number(params.postId);
    const navigate = useNavigate();
    const [title, setTitle] = useState('');
    const [body, setBody] = useState('');
    const [files, setFiles] = useState([]);
    const [existing, setExisting] = useState();
    const [error, setError] = useState('');
    const [saving, setSaving] = useState(false);
    useEffect(() => { if (edit)
        api.post(postId).then((post) => { setExisting(post); setTitle(post.title); setBody(post.body ?? ''); }).catch((e) => setError(messageOf(e))); }, [edit, postId]);
    const selectFiles = (list) => { const next = Array.from(list ?? []); if (next.length > 5) {
        setError('이미지는 최대 5개까지 첨부할 수 있어요.');
        return;
    } const invalid = next.find((f) => !['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(f.type) || f.size > 2 * 1024 * 1024); if (invalid) {
        setError('JPG, PNG, GIF, WEBP 이미지만 파일당 2MB까지 첨부할 수 있어요.');
        return;
    } setError(''); setFiles(next); };
    const submit = async (event) => { event.preventDefault(); if (title.trim().length < 5) {
        setError('제목을 5자 이상 입력해 주세요.');
        return;
    } if (!body.trim()) {
        setError('본문을 입력해 주세요.');
        return;
    } setSaving(true); try {
        const result = edit ? await api.updatePost(postId, title.trim(), body.trim()) : await api.createPost(boardId, title.trim(), body.trim(), files);
        navigate(`/posts/${result.id}`);
    }
    catch (e) {
        setError(messageOf(e));
    }
    finally {
        setSaving(false);
    } };
    return <div className="page form-page"><div className="form-heading"><p className="eyebrow green">{edit ? 'EDIT STORY' : 'NEW STORY'}</p><h1>{edit ? '이야기 다듬기' : '새 이야기 쓰기'}</h1><p>당신의 생각을 천천히 들려주세요.</p></div><form className="editor-card" onSubmit={submit}><label>제목 <span>{title.length}/200</span><input required minLength={5} maxLength={200} value={title} onChange={(e) => setTitle(e.target.value)} placeholder="어떤 이야기를 나누고 싶나요?"/></label><label>내용 <textarea required value={body} onChange={(e) => setBody(e.target.value)} placeholder="내용을 입력해 주세요."/></label>{!edit && <label className="upload-zone"><input type="file" accept="image/jpeg,image/png,image/gif,image/webp" multiple onChange={(e) => selectFiles(e.target.files)}/><Icon name="image" size={28}/><strong>사진을 여기에 놓거나 눌러 선택하세요</strong><span>JPG, PNG, GIF, WEBP · 각 2MB 이하 · 최대 5장</span></label>}{edit && existing?.images?.length ? <p className="notice">첨부 이미지 수정은 현재 지원되지 않아 기존 이미지가 유지됩니다.</p> : null}{files.length > 0 && <div className="file-list">{files.map((file, i) => <div key={`${file.name}-${i}`}><span>{file.name}</span><button type="button" onClick={() => setFiles(files.filter((_, n) => n !== i))}>제거</button></div>)}</div>}{error && <p className="form-error" role="alert">{error}</p>}<div className="form-actions"><button type="button" className="button ghost" onClick={() => navigate(-1)}>취소</button><button className="button dark" disabled={saving}>{saving ? '저장 중…' : edit ? '수정 완료' : '이야기 올리기'}</button></div></form></div>;
}
function AuthPage({ signup = false }) {
    const { login } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [nickName, setNickName] = useState('');
    const [error, setError] = useState(() => new URLSearchParams(location.search).has('oauthError') ? '소셜 로그인에 실패했습니다. 다시 시도해 주세요.' : '');
    const [busy, setBusy] = useState(false);
    const submit = async (event) => { event.preventDefault(); setBusy(true); setError(''); try {
        if (signup)
            await api.signup(email, password, nickName);
        await login(email, password);
        navigate('/', { replace: true });
    }
    catch (e) {
        setError(messageOf(e));
    }
    finally {
        setBusy(false);
    } };
    return <div className="auth-layout"><section className="auth-art"><Link className="brand light" to="/"><span className="brand-mark"><Icon name="logo" size={25}/></span>모아</Link><div><p className="eyebrow">WELCOME TO MOA</p><h1>{signup ? <>당신의 첫 이야기를<br />기다리고 있어요.</> : <>다시 만나<br />반가워요.</>}</h1><p>서로의 생각이 모여 더 넓은 세상이 됩니다.</p></div><blockquote>“좋은 대화는 작은 호기심에서 시작됩니다.”</blockquote></section><section className="auth-panel"><div className="auth-card"><p className="eyebrow green">{signup ? 'JOIN US' : 'SIGN IN'}</p><h2>{signup ? '회원가입' : '로그인'}</h2><p>{signup ? '모아에서 새로운 이야기를 시작하세요.' : '이메일로 모아에 들어오세요.'}</p><form onSubmit={submit}>{signup && <label>닉네임<input value={nickName} onChange={(e) => setNickName(e.target.value)} required placeholder="어떻게 불러드릴까요?"/></label>}<label>이메일<input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required placeholder="name@example.com"/></label><label>비밀번호<input type="password" minLength={8} maxLength={30} value={password} onChange={(e) => setPassword(e.target.value)} required placeholder="8자 이상 입력해 주세요"/></label>{error && <p className="form-error" role="alert">{error}</p>}<button className="button accent full" disabled={busy}>{busy ? '잠시만요…' : signup ? '가입하고 시작하기' : '로그인'}</button></form><div className="divider"><span>또는</span></div><div className="social-row"><a className="button social kakao" href="/oauth2/authorization/kakao">Kakao</a><a className="button social" href="/oauth2/authorization/google">Google</a></div><p className="auth-switch">{signup ? '이미 계정이 있나요?' : '아직 계정이 없나요?'} <Link to={signup ? '/login' : '/signup'}>{signup ? '로그인' : '회원가입'}</Link></p></div></section></div>;
}
function OAuthCallbackPage() {
    const { restoreSession } = useAuth();
    const navigate = useNavigate();
    const [error, setError] = useState('');
    useEffect(() => { void restoreSession().then((ok) => { if (ok)
        navigate('/', { replace: true });
    else
        setError('로그인 정보를 확인하지 못했습니다. 다시 로그인해 주세요.'); }); }, [navigate, restoreSession]);
    if (error)
        return <div className="state fullscreen"><h1>로그인 실패</h1><p>{error}</p><Link className="button dark" to="/login">로그인으로 돌아가기</Link></div>;
    return <div className="state fullscreen"><span className="spinner"/><p>로그인을 완료하고 있어요.</p></div>;
}
function NotificationsPage() {
    const navigate = useNavigate();
    const [items, setItems] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const load = useCallback(() => { setLoading(true); api.notifications().then((p) => setItems(p.content)).catch((e) => setError(messageOf(e))).finally(() => setLoading(false)); }, []);
    useEffect(load, [load]);
    const open = async (item) => { if (!item.read) {
        try {
            await api.readNotification(item.id);
        }
        catch { /* navigation remains useful */ }
    } navigate(`/posts/${item.postId}`); };
    return <div className="page narrow"><div className="simple-heading"><p className="eyebrow green">NOTIFICATIONS</p><h1>새로운 소식</h1><p>내 이야기에 이어진 대화를 확인하세요.</p></div><PageState loading={loading} error={error} empty={!items.length ? '새로운 알림이 없습니다' : undefined} onRetry={load}><div className="notification-list">{items.map((item) => <button onClick={() => void open(item)} className={item.read ? 'read' : ''} key={item.id}><span className="avatar">{item.actorUsername.slice(0, 1)}</span><div><p>{item.message}</p><span>{formatDate(item.createdAt)}</span></div>{!item.read && <i />}<Icon name="arrow"/></button>)}</div></PageState></div>;
}
function ProfilePage() {
    const { user } = useAuth();
    const [profile, setProfile] = useState();
    const [error, setError] = useState('');
    useEffect(() => { api.profile().then(setProfile).catch((e) => setError(messageOf(e))); }, []);
    return <div className="page profile-page"><div className="profile-card"><div className="profile-cover"/><div className="profile-identity"><span className="avatar profile-avatar">{profile?.nickName?.slice(0, 1) || '?'}</span><div><p className="eyebrow green">MY PROFILE</p><h1>{profile?.nickName || '내 정보'}</h1><span>{user?.email}</span></div></div>{error ? <p className="form-error">{error}</p> : <dl><div><dt>닉네임</dt><dd>{profile?.nickName || '—'}</dd></div><div><dt>전화번호</dt><dd>{profile?.phoneNumber || '등록되지 않음'}</dd></div><div><dt>생년월일</dt><dd>{profile?.birth || '등록되지 않음'}</dd></div><div><dt>함께한 날</dt><dd>{profile?.createdAt ? formatDate(profile.createdAt) : '—'}</dd></div></dl>}</div></div>;
}
function AdminBoardsPage() {
    const [boards, setBoards] = useState([]);
    const [editing, setEditing] = useState();
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [error, setError] = useState('');
    const load = useCallback(() => { api.boards().then(setBoards).catch((e) => setError(messageOf(e))); }, []);
    useEffect(load, [load]);
    const submit = async (e) => { e.preventDefault(); try {
        if (editing)
            await api.updateBoard(editing.id, { name, description });
        else
            await api.createBoard({ name, description });
        setEditing(undefined);
        setName('');
        setDescription('');
        load();
    }
    catch (err) {
        setError(messageOf(err));
    } };
    const edit = (board) => { setEditing(board); setName(board.name); setDescription(board.description ?? ''); };
    const remove = async (board) => { if (!confirm(`'${board.name}' 게시판을 삭제할까요?`))
        return; try {
        await api.deleteBoard(board.id);
        load();
    }
    catch (e) {
        setError(messageOf(e));
    } };
    return <div className="page narrow"><div className="simple-heading"><p className="eyebrow green">ADMIN</p><h1>게시판 관리</h1></div><div className="admin-layout"><form className="admin-form" onSubmit={submit}><h2>{editing ? '게시판 수정' : '새 게시판'}</h2><label>이름<input required value={name} onChange={(e) => setName(e.target.value)}/></label><label>설명<textarea value={description} onChange={(e) => setDescription(e.target.value)}/></label>{error && <p className="form-error">{error}</p>}<button className="button dark">{editing ? '수정 저장' : '게시판 만들기'}</button>{editing && <button type="button" className="button ghost" onClick={() => { setEditing(undefined); setName(''); setDescription(''); }}>취소</button>}</form><div className="admin-list">{boards.map((board) => <div key={board.id}><div><strong>{board.name}</strong><p>{board.description}</p></div><button onClick={() => edit(board)}><Icon name="edit"/></button><button onClick={() => void remove(board)}><Icon name="trash"/></button></div>)}</div></div></div>;
}
function ChatRoomsPage() {
    const navigate = useNavigate();
    const [rooms, setRooms] = useState([]);
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [loading, setLoading] = useState(true);
    const [creating, setCreating] = useState(false);
    const [error, setError] = useState('');
    const load = useCallback(() => {
        setLoading(true);
        api.chatRooms().then((result) => setRooms(result.content ?? [])).catch((e) => setError(messageOf(e))).finally(() => setLoading(false));
    }, []);
    useEffect(load, [load]);
    useEffect(() => {
        const token = getAccessToken();
        if (!token)
            return;
        const client = new Client({
            brokerURL: websocketUrl(),
            connectHeaders: { Authorization: `Bearer ${token}` },
            reconnectDelay: 3000,
            onConnect: () => client.subscribe('/topic/rooms', load),
        });
        client.activate();
        return () => { void client.deactivate(); };
    }, [load]);
    const createRoom = async (event) => {
        event.preventDefault();
        if (!name.trim() || creating)
            return;
        setCreating(true);
        setError('');
        try {
            const room = await api.createChatRoom({ name: name.trim(), description: description.trim() });
            navigate(`/chat/${room.id}`);
        }
        catch (e) {
            setError(messageOf(e));
        }
        finally {
            setCreating(false);
        }
    };
    return <div className="page chat-page">
      <div className="chat-heading"><div><p className="eyebrow green">LIVE CHAT</p><h1>함께 이야기해요</h1><p>관심 있는 방에 들어가 실시간으로 대화를 나눠보세요.</p></div></div>
      <div className="chat-lobby">
        <form className="chat-create" onSubmit={createRoom}><h2>새 채팅방</h2><label>방 이름<input maxLength={50} required value={name} onChange={(e) => setName(e.target.value)} placeholder="예: 오늘의 개발 이야기"/></label><label>소개<textarea maxLength={200} value={description} onChange={(e) => setDescription(e.target.value)} placeholder="어떤 이야기를 나누는 방인가요?"/></label>{error && <p className="form-error">{error}</p>}<button className="button accent" disabled={creating}>{creating ? '만드는 중…' : '채팅방 만들기'}</button></form>
        <section className="chat-room-section"><div className="section-heading"><div><p className="eyebrow green">ROOMS</p><h2>채팅방 목록</h2></div><span>{rooms.length}개</span></div><PageState loading={loading} error={error && !rooms.length ? error : ''} empty={!loading && !rooms.length ? '아직 열린 채팅방이 없습니다' : undefined} onRetry={load}><div className="chat-room-grid">{rooms.map((room) => <Link className="chat-room-card" to={`/chat/${room.id}`} key={room.id}><div className="room-live"><span/>{room.onlineCount}명 접속 중</div><h3>{room.name}</h3><p>{room.description || '자유롭게 이야기를 나누는 공간입니다.'}</p><div><span>방장 {room.ownerUsername}</span><span>멤버 {room.memberCount}명</span></div></Link>)}</div></PageState></section>
      </div>
    </div>;
}
function ChatRoomPage() {
    const { roomId } = useParams();
    const id = Number(roomId);
    const { user } = useAuth();
    const navigate = useNavigate();
    const clientRef = useRef(null);
    const endRef = useRef(null);
    const [room, setRoom] = useState();
    const [messages, setMessages] = useState([]);
    const [members, setMembers] = useState([]);
    const [content, setContent] = useState('');
    const [connected, setConnected] = useState(false);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const appendMessage = useCallback((incoming) => {
        setMessages((current) => current.some((item) => item.id === incoming.id) ? current : [...current, incoming]);
    }, []);
    useEffect(() => {
        let active = true;
        let client;
        const connect = async () => {
            setLoading(true);
            try {
                const joined = await api.joinChatRoom(id);
                const [history, memberList] = await Promise.all([api.chatMessages(id), api.chatMembers(id)]);
                if (!active)
                    return;
                setRoom(joined);
                setMessages([...(history.messages ?? [])].reverse());
                setMembers(memberList);
                const token = getAccessToken();
                if (!token)
                    throw new Error('로그인 토큰이 없습니다. 다시 로그인해 주세요.');
                client = new Client({
                    brokerURL: websocketUrl(),
                    connectHeaders: { Authorization: `Bearer ${token}` },
                    reconnectDelay: 3000,
                    onConnect: () => {
                        setConnected(true);
                        client.subscribe(`/topic/room/${id}`, (frame) => appendMessage(JSON.parse(frame.body)));
                    },
                    onStompError: (frame) => setError(frame.headers.message || '채팅 연결에 실패했습니다.'),
                    onWebSocketClose: () => setConnected(false),
                });
                clientRef.current = client;
                client.activate();
            }
            catch (e) {
                if (active)
                    setError(messageOf(e));
            }
            finally {
                if (active)
                    setLoading(false);
            }
        };
        void connect();
        return () => {
            active = false;
            clientRef.current = null;
            if (client)
                void client.deactivate();
        };
    }, [id, appendMessage]);
    useEffect(() => { endRef.current?.scrollIntoView({ behavior: 'smooth' }); }, [messages]);
    const send = (event) => {
        event.preventDefault();
        const value = content.trim();
        if (!value || !clientRef.current?.connected)
            return;
        clientRef.current.publish({ destination: `/app/room/${id}/message`, body: JSON.stringify({ content: value }) });
        setContent('');
    };
    const leave = async () => {
        const owner = room?.ownerUsername === user?.email;
        const message = owner ? '방장이 나가면 채팅방과 모든 메시지가 삭제됩니다. 삭제할까요?' : '채팅방에서 나갈까요?';
        if (!confirm(message))
            return;
        setError('');
        try {
            if (owner)
                await api.deleteChatRoom(id);
            else
                await api.leaveChatRoom(id);
            navigate('/chat', { replace: true });
        }
        catch (e) {
            setError(messageOf(e));
        }
    };
    return <div className="page chat-room-page"><PageState loading={loading} error={error && !room ? error : ''} onRetry={() => window.location.reload()}>
      {room && <div className="chat-shell"><header className="chat-room-header"><Link to="/chat" className="back-link">← 채팅방 목록</Link><div><div><h1>{room.name}</h1><p>{room.description}</p></div><div className={`connection-state ${connected ? 'online' : ''}`}><span/>{connected ? '실시간 연결됨' : '연결 중'}</div><button className="mobile-room-exit" onClick={() => void leave()}>{room.ownerUsername === user?.email ? '삭제' : '나가기'}</button></div></header>
        <div className="chat-columns"><aside className="member-panel"><h2>참여자 <em>{members.length}</em></h2>{members.map((member) => <div className="member-row" key={member.userId}><span className="avatar small-avatar">{member.email.slice(0, 1).toUpperCase()}</span><div><strong>{member.email}</strong><small className={member.online ? 'online' : ''}>{member.online ? '접속 중' : '오프라인'}</small></div></div>)}<button className="button ghost compact" onClick={() => void leave()}>{room.ownerUsername === user?.email ? '채팅방 삭제' : '채팅방 나가기'}</button></aside>
          <section className="message-panel"><div className="message-list">{messages.length === 0 && <div className="chat-empty"><Icon name="chat" size={34}/><p>첫 메시지를 보내 대화를 시작해보세요.</p></div>}{messages.map((message) => message.type === 'TALK' ? <div className={`message ${message.senderUserId === user?.id ? 'mine' : ''}`} key={message.id}><div className="message-meta"><strong>{message.senderEmail}</strong><span>{formatDate(message.createdAt)}</span></div><p>{message.content}</p></div> : <div className="system-message" key={message.id}>{message.content}</div>)}<div ref={endRef}/></div>
            {error && <p className="chat-error">{error}</p>}<form className="message-form" onSubmit={send}><input maxLength={1000} value={content} onChange={(e) => setContent(e.target.value)} placeholder={connected ? '메시지를 입력하세요' : '서버에 연결하는 중입니다'} disabled={!connected}/><button className="button dark" disabled={!connected || !content.trim()}>보내기</button></form></section>
        </div>
      </div>}
    </PageState></div>;
}
function LiveLobbyPage() {
    const navigate = useNavigate();
    const [events, setEvents] = useState([]);
    const [title, setTitle] = useState('');
    const [code, setCode] = useState('');
    const [loading, setLoading] = useState(true);
    const [creating, setCreating] = useState(false);
    const [error, setError] = useState('');
    const load = useCallback(() => {
        setLoading(true);
        api.liveEvents().then(setEvents).catch((e) => setError(messageOf(e))).finally(() => setLoading(false));
    }, []);
    useEffect(load, [load]);
    const create = async (event) => {
        event.preventDefault();
        if (!title.trim() || creating)
            return;
        setCreating(true);
        setError('');
        try {
            const created = await api.createLiveEvent(title.trim());
            navigate(`/live/${created.code}`);
        }
        catch (e) {
            setError(messageOf(e));
        }
        finally {
            setCreating(false);
        }
    };
    const join = (event) => {
        event.preventDefault();
        const normalized = code.trim().toUpperCase();
        if (normalized)
            navigate(`/live/${normalized}`);
    };
    return <div className="page live-page"><div className="live-hero"><div><p className="eyebrow">MOA LIVE</p><h1>질문과 투표를<br/>한곳에서 실시간으로</h1><p>참가 코드를 공유하고, 질문을 모으고, 모두의 선택을 바로 확인하세요.</p></div><form className="live-join" onSubmit={join}><label>참가 코드</label><div><input maxLength={12} value={code} onChange={(e) => setCode(e.target.value.toUpperCase())} placeholder="코드 입력"/><button className="button accent">참여하기</button></div></form></div>
      <div className="live-lobby"><form className="live-create" onSubmit={create}><p className="eyebrow green">HOST</p><h2>새 라이브 열기</h2><label>라이브 제목<input required maxLength={100} value={title} onChange={(e) => setTitle(e.target.value)} placeholder="예: 10월 팀 타운홀"/></label>{error && <p className="form-error">{error}</p>}<button className="button dark" disabled={creating}>{creating ? '만드는 중…' : '라이브 시작'}</button></form>
        <section><div className="section-heading"><div><p className="eyebrow green">MY LIVE</p><h2>내가 만든 라이브</h2></div><span>{events.length}개</span></div><PageState loading={loading} error={error && !events.length ? error : ''} empty={!loading && !events.length ? '아직 만든 라이브가 없습니다' : undefined} onRetry={load}><div className="live-event-list">{events.map((item) => <Link to={`/live/${item.code}`} className="live-event-card" key={item.id}><span className={`live-status ${item.status === 'OPEN' ? 'open' : ''}`}>{item.status === 'OPEN' ? '진행 중' : '종료'}</span><div><h3>{item.title}</h3><p>{formatDate(item.createdAt)} · 참가 코드 <strong>{item.code}</strong></p></div><Icon name="arrow"/></Link>)}</div></PageState></section>
      </div>
    </div>;
}
function LiveEventPage() {
    const { code = '' } = useParams();
    const navigate = useNavigate();
    const [snapshot, setSnapshot] = useState();
    const [question, setQuestion] = useState('');
    const [pollTitle, setPollTitle] = useState('');
    const [pollOptions, setPollOptions] = useState(['', '']);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const load = useCallback(async (quiet = false) => {
        if (!quiet)
            setLoading(true);
        try {
            setSnapshot(await api.liveEvent(code));
            setError('');
        }
        catch (e) {
            setError(messageOf(e));
        }
        finally {
            if (!quiet)
                setLoading(false);
        }
    }, [code]);
    useEffect(() => { void load(); }, [load]);
    useEffect(() => {
        if (!snapshot || snapshot.event.status !== 'OPEN')
            return;
        const source = new EventSource(`/api/live/events/${encodeURIComponent(code)}/stream`);
        const refresh = () => void load(true);
        ['question_created', 'question_liked', 'question_deleted', 'poll.created', 'poll.voted', 'poll.closed', 'event_closed'].forEach((name) => source.addEventListener(name, refresh));
        source.onerror = () => { if (source.readyState === EventSource.CLOSED) setError('실시간 연결이 종료되었습니다.'); };
        return () => source.close();
    }, [code, load, snapshot?.event.status]);
    const ask = async (event) => {
        event.preventDefault();
        if (!question.trim())
            return;
        try {
            await api.createLiveQuestion(code, question.trim());
            setQuestion('');
            await load(true);
        }
        catch (e) { setError(messageOf(e)); }
    };
    const toggleLike = async (id) => { try { await api.likeLiveQuestion(id); await load(true); } catch (e) { setError(messageOf(e)); } };
    const removeQuestion = async (id) => { try { await api.deleteLiveQuestion(id); await load(true); } catch (e) { setError(messageOf(e)); } };
    const createPoll = async (event) => {
        event.preventDefault();
        const options = pollOptions.map((item) => item.trim()).filter(Boolean);
        if (!pollTitle.trim() || options.length < 2)
            return;
        try {
            await api.createLivePoll(code, pollTitle.trim(), options);
            setPollTitle('');
            setPollOptions(['', '']);
            await load(true);
        }
        catch (e) { setError(messageOf(e)); }
    };
    const vote = async (pollId, optionId) => { try { await api.voteLivePoll(pollId, optionId); await load(true); } catch (e) { setError(messageOf(e)); } };
    const closePoll = async (id) => { try { await api.closeLivePoll(id); await load(true); } catch (e) { setError(messageOf(e)); } };
    const closeEvent = async () => { if (!confirm('라이브를 종료할까요?')) return; try { await api.closeLiveEvent(code); await load(true); } catch (e) { setError(messageOf(e)); } };
    const data = snapshot;
    return <div className="page live-session"><PageState loading={loading} error={!data ? error : ''} onRetry={() => void load()}>{data && <>
      <header className="live-session-header"><button className="back-link" onClick={() => navigate('/live')}>← 라이브 목록</button><div className="live-title-row"><div><span className={`live-status ${data.event.status === 'OPEN' ? 'open' : ''}`}>{data.event.status === 'OPEN' ? 'LIVE' : '종료됨'}</span><h1>{data.event.title}</h1><p>호스트 {data.event.hostUsername}</p></div><div className="live-code"><span>참가 코드</span><strong>{data.event.code}</strong></div>{data.event.host && data.event.status === 'OPEN' && <button className="button outline" onClick={() => void closeEvent()}>라이브 종료</button>}</div></header>
      {error && <p className="live-error">{error}</p>}<div className="live-grid"><section className="live-questions"><div className="section-heading"><div><p className="eyebrow green">Q&amp;A</p><h2>실시간 질문</h2></div><span>{data.questions.length}개</span></div>{data.event.status === 'OPEN' && <form className="question-form" onSubmit={ask}><input maxLength={100} value={question} onChange={(e) => setQuestion(e.target.value)} placeholder="궁금한 내용을 질문해 보세요"/><button className="button dark" disabled={!question.trim()}>질문 등록</button></form>}<div className="question-list">{data.questions.length === 0 && <div className="mini-empty">아직 등록된 질문이 없습니다.</div>}{data.questions.map((item, index) => <article className="question-card" key={item.id}><span className="question-rank">{index + 1}</span><div><p>{item.content}</p><small>{item.authorUsername} · {formatDate(item.createdAt)}</small></div><button className={item.likedByMe ? 'liked' : ''} disabled={data.event.status !== 'OPEN'} onClick={() => void toggleLike(item.id)}>♥ <b>{item.likeCount}</b></button>{(item.mine || data.event.host) && <button className="question-delete" onClick={() => void removeQuestion(item.id)}>×</button>}</article>)}</div></section>
        <aside className="live-polls"><div className="section-heading"><div><p className="eyebrow green">POLL</p><h2>실시간 투표</h2></div></div>{data.event.host && data.event.status === 'OPEN' && <form className="poll-create" onSubmit={createPoll}><input maxLength={100} value={pollTitle} onChange={(e) => setPollTitle(e.target.value)} placeholder="투표 제목"/>{pollOptions.map((option, index) => <input key={index} maxLength={50} value={option} onChange={(e) => setPollOptions((current) => current.map((item, i) => i === index ? e.target.value : item))} placeholder={`선택지 ${index + 1}`}/>)}<div>{pollOptions.length < 5 && <button type="button" className="text-button" onClick={() => setPollOptions((current) => [...current, ''])}>+ 선택지 추가</button>}<button className="button accent compact" disabled={!pollTitle.trim() || pollOptions.filter((item) => item.trim()).length < 2}>투표 만들기</button></div></form>}<div className="poll-list">{data.polls.length === 0 && <div className="mini-empty">진행 중인 투표가 없습니다.</div>}{data.polls.map((poll) => <article className="poll-card" key={poll.id}><div className="poll-card-title"><h3>{poll.title}</h3><span>{poll.totalVotes}명 참여</span></div>{poll.options.map((option) => { const percent = poll.totalVotes ? Math.round(option.count * 100 / poll.totalVotes) : 0; return <button className={poll.myOptionId === option.id ? 'selected' : ''} disabled={poll.status !== 'OPEN' || Boolean(poll.myOptionId)} onClick={() => void vote(poll.id, option.id)} key={option.id}><span className="poll-fill" style={{ width: `${percent}%` }}/><span>{option.text}</span><b>{option.count} · {percent}%</b></button>; })}{data.event.host && poll.status === 'OPEN' && <button className="text-button close-poll" onClick={() => void closePoll(poll.id)}>투표 마감</button>}</article>)}</div></aside></div>
    </>}</PageState></div>;
}
function Protected({ children, admin = false }) {
    const { user, restoring } = useAuth();
    const location = useLocation();
    if (restoring)
        return <div className="state fullscreen"><span className="spinner"/></div>;
    if (!user)
        return <Navigate to="/login" replace state={{ from: location.pathname }}/>;
    if (admin && user.role !== 'ADMIN')
        return <Navigate to="/" replace/>;
    return <>{children}</>;
}
function NotFoundPage() { return <div className="state fullscreen"><span className="empty-glyph">404</span><h1>길을 잃었어요</h1><p>찾으시는 페이지가 없거나 이동했습니다.</p><Link className="button dark" to="/">홈으로 가기</Link></div>; }
export default function App() {
    return <Routes>
    <Route path="/login" element={<AuthPage />}/><Route path="/signup" element={<AuthPage signup/>}/><Route path="/oauth/callback" element={<OAuthCallbackPage />}/>
    <Route path="*" element={<Layout><Routes>
      <Route path="/" element={<HomePage />}/><Route path="/boards/:boardId" element={<BoardPage />}/><Route path="/posts/:postId" element={<PostDetailPage />}/>
      <Route path="/boards/:boardId/new" element={<Protected><PostEditorPage /></Protected>}/><Route path="/posts/:postId/edit" element={<Protected><PostEditorPage edit/></Protected>}/>
      <Route path="/notifications" element={<Protected><NotificationsPage /></Protected>}/><Route path="/me" element={<Protected><ProfilePage /></Protected>}/><Route path="/admin/boards" element={<Protected admin><AdminBoardsPage /></Protected>}/>
      <Route path="/chat" element={<Protected><ChatRoomsPage /></Protected>}/><Route path="/chat/:roomId" element={<Protected><ChatRoomPage /></Protected>}/>
      <Route path="/live" element={<Protected><LiveLobbyPage /></Protected>}/><Route path="/live/:code" element={<Protected><LiveEventPage /></Protected>}/>
      <Route path="*" element={<NotFoundPage />}/>
    </Routes></Layout>}/>
  </Routes>;
}
