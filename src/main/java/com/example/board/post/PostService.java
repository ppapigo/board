package com.example.board.post;

import com.example.board.auth.UserRepository;
import com.example.board.board.BoardRepository;
import com.example.board.entity.Board;
import com.example.board.entity.Post;
import com.example.board.entity.User;
import com.example.board.global.IngestResult;
import com.example.board.post.dto.CreatePost;
import com.example.board.post.dto.PostDTO;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    @Transactional
    public IngestResult create(Long boardId, Long loginUserId, CreatePost request){
        Board board = boardRepository.findById(boardId)
                .orElse(null);

        User user = userRepository.findById(loginUserId)
                .orElse(null);

        Post post = new Post();
        post.setTitle(request.getTitle());
        post.setBody(request.getBody());
        post.setBoard(board);
        post.setAuthor(user);

        postRepository.save( post );

        return new IngestResult("OK", "OK");

    }

    @Transactional(readOnly = true)
    public List<PostDTO> list(){
        return postRepository.findAll().stream().map(
                Post::toDTO).toList();
    }

    /*    public PostDTO findById(Long id){
            return postRepository.findById(id).orElse(null);
        } */
}
