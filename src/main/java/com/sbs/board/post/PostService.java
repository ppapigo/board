package com.sbs.board.post;

import com.sbs.board.auth.UserRepository;
import com.sbs.board.board.BoardRepository;
import com.sbs.board.global.entity.Board;
import com.sbs.board.global.entity.Post;
import com.sbs.board.global.entity.PostImage;
import com.sbs.board.global.entity.User;
import com.sbs.board.global.exception.ErrorCode;
import com.sbs.board.global.exception.ForbiddenException;
import com.sbs.board.global.exception.NotFoundException;
import com.sbs.board.global.exception.UnauthorizedException;
import com.sbs.board.post.dto.PostRequest;
import com.sbs.board.post.dto.PostDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public User requiredLogin(Long loginUserId) {
        if (loginUserId == null) {
            throw new UnauthorizedException(ErrorCode.LOGIN_REQUIRED);
        }

        return userRepository.findById(loginUserId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public PostDTO create(Long boardId, Long loginUserId, PostRequest request, List<MultipartFile>images) {
        System.out.println("Board ID: " + boardId);
        System.out.println("User ID: " + loginUserId);

        //User user = requiredLogin(loginUserId);

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOARD_NOT_FOUND));

        User user = userRepository.findById(loginUserId)
                .orElseThrow(()->new NotFoundException(ErrorCode.USER_NOT_FOUND));

        List<String> storedNames = new ArrayList<>();
        try{
            Post post = new Post();
            post.setTitle(request.getTitle());
            post.setBody(request.getBody());
            post.setBoard(board);
            post.setAuthor(user);

            //required == false 이므로 null이면 처리하지 않는다.
            if( images != null){
                int order = 0;
                for(MultipartFile file : images){
                    String storedName = fileStorageService.store(file);
                    storedNames.add(storedName);
                    PostImage image = PostImage.builder()
                                    .storedName(storedName) //실제 저장된 경로 + uuid파일명
                                    .originalName(file.getOriginalFilename())
                                    .contentType(file.getContentType())
                                    .size(file.getSize())
                                    .sortOrder(order++)
                                    .build();

                    post.addImage(image);
                }
            }

            Post savedPost = postRepository.save( post );
            return Post.toDTO( savedPost );
        }catch(RuntimeException ex){
            //삭제 처리를 해야함
           for(String fileName : storedNames){
               fileStorageService.delete(fileName);
           }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<PostDTO> list() {
        return postRepository.findAll().stream().map(
                Post::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public Page<PostDTO> findByBoardId(Long boardId, Pageable pageable) {
//        Board board = boardRepository.findById(boardId)
//                .orElseThrow(()-> new NotFoundException(ErrorCode.BOARD_NOT_FOUND));

        return postRepository.findByBoardId(boardId, pageable).map(Post::toDTO);
    }

    @Transactional
    public PostDTO getPost(Long loginUserId, Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() ->new NotFoundException(ErrorCode.POST_NOT_FOUND));

        //viewCount 증가
        if(!post.isAuthor(loginUserId) && loginUserId!=null){
            post.increaseViewCount();
        }

        return Post.toDTO(post, loginUserId);
    }

    @Transactional
    public PostDTO update(Long postId, @Valid PostRequest request) {

        Post post = postRepository.findById(postId)
                .orElseThrow(()->new NotFoundException(ErrorCode.POST_NOT_FOUND));

//        validateAuthor(post, loginUserId);
//
//        // 게시글 수정권한이 없다면 에러를 발생시킴
//        User user = post.getAuthor();
//        if (!Objects.equals(user.getId(), loginUserId)) {
//            throw new ForbiddenException(ErrorCode.POST_ACCESS_DENIED);
//        }

        post.setTitle(request.getTitle());
        post.setBody(request.getBody());

        Post savedPost = postRepository.save( post );

        return Post.toDTO( savedPost );
    }

    @Transactional
    public void delete(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(()->new NotFoundException(ErrorCode.POST_NOT_FOUND));

//        validateAuthor(post, loginUserId);
//
//        // 게시글 수정권한이 없다면 에러를 발생시킴
//        User user = post.getAuthor();
//        if (!Objects.equals(user.getId(), loginUserId)) {
//            throw new ForbiddenException(ErrorCode.POST_ACCESS_DENIED);
//        }

        postRepository.delete( post );
    }

    private void validateAuthor(Post post, Long userId) {
        if (!post.isAuthor(userId)) {
            throw new ForbiddenException(ErrorCode.POST_ACCESS_DENIED);
        }
    }

//    public PostDTO findById(Long id) {
//        return postRepository.findById(id).orElse(null);
//    }
}
