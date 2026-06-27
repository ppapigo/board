package com.example.board.post;

import com.example.board.auth.UserRepository;
import com.example.board.board.BoardRepository;
import com.example.board.global.entity.Board;
import com.example.board.global.entity.Post;
import com.example.board.global.entity.User;
import com.example.board.global.IngestResult;
import com.example.board.global.exception.ErrorCode;
import com.example.board.global.exception.ForbiddenException;
import com.example.board.global.exception.NotFoundException;
import com.example.board.global.exception.UnauthorizedException;
import com.example.board.post.dto.PostRequest;
import com.example.board.post.dto.PostDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    public User requiredLogin(Long loginUserId){
        if(loginUserId == null){
            throw new UnauthorizedException(ErrorCode.LOGIN_REQUIRED);
        }
        return userRepository.findById(loginUserId)
                .orElseThrow(()->new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public PostDTO create(Long boardId, Long loginUserId, PostRequest request){
        System.out.println("Board ID: "+boardId);
        System.out.println("User ID: "+loginUserId);

        User user = requiredLogin(loginUserId);

        Board board = boardRepository.findById(boardId)
                .orElseThrow(()->new NotFoundException(ErrorCode.BOARD_NOT_FOUND));


        Post post = new Post();
        post.setTitle(request.getTitle());
        post.setBody(request.getBody());
        post.setBoard(board);
        post.setAuthor(user);

        postRepository.save( post );

        return Post.toDTO(post);

    }

    @Transactional(readOnly = true)
    public List<PostDTO> list(){
        return postRepository.findAll().stream().map(
                Post::toDTO).toList();
    }

    /*    public PostDTO findById(Long id){
            return postRepository.findById(id).orElse(null);
        } */
    @Transactional(readOnly = true)
    public PostDTO getPost(Long loginUserId,Long id) {
        Post post = postRepository.findById(id).orElseThrow(()->new NotFoundException((ErrorCode.POST_NOT_FOUND)));
        return Post.toDTO(post, loginUserId);
    }


    @Transactional
    public PostDTO update(
            Long loginUserId, Long postId,@Valid PostRequest request
    ){
        Post post = postRepository.findById(postId)
                        .orElseThrow(()->new NotFoundException(ErrorCode.POST_NOT_FOUND));

        validateAuthor(post, loginUserId);

        //게시물 수정권한이 없다면 에러를 발생
        User user = post.getAuthor();
        if(!Objects.equals(user.getId(), loginUserId))
            throw new ForbiddenException(ErrorCode.POST_ACCESS_DENIED);

        post.setTitle(request.getTitle());
        post.setBody(request.getBody());

        Post savedPost = postRepository.save( post );

        return Post.toDTO(savedPost);
    }


    @Transactional
    public void delete(Long loginUserId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(()->new NotFoundException(ErrorCode.POST_NOT_FOUND));

        validateAuthor(post, loginUserId);

        //게시물 수정권한이 없다면 에러를 발생
        User user = post.getAuthor();
        if(!Objects.equals(user.getId(), loginUserId))
            throw new ForbiddenException(ErrorCode.POST_ACCESS_DENIED);

        postRepository.delete( post );
    }

    public void validateAuthor(Post post, Long userId){
        if(!post.isAuthor(userId)){
            throw new ForbiddenException(ErrorCode.POST_ACCESS_DENIED);
        }
    }

    public List<PostDTO> findByBoardId(Long boardId) {
       /* Board board = boardRepository.findById(boardId)
                .orElseThrow(()->new NotFoundException(ErrorCode.BOARD_NOT_FOUND));*/

        return postRepository.findByBoardId(boardId).stream()
                .map(Post::toDTO).toList();
    }
}
