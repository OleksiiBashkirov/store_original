package bashkirov.store_original.service;

import bashkirov.store_original.model.Comment;
import bashkirov.store_original.model.Person;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final JdbcTemplate jdbcTemplate;
    private final PersonDetailsService personDetailsService;

    public Optional<Comment> getOptionalComment(int productId) {
        Person person = personDetailsService.getCurrentUser();
        return jdbcTemplate.query(
                "select * from comment where person_id = ? AND product_id = ?",
                new Object[]{person.getId(), productId},
                (rs, rowNum) -> {
                    Comment comment = new Comment();
                    comment.setPersonId(rs.getInt("person_id"));
                    comment.setProductId(rs.getInt("product_id"));
                    comment.setComment(rs.getString("comment"));
                    return comment;
                }
        ).stream().findAny();
    }

    public List<Comment> getAllProductComments(int productId) {
        Person person = personDetailsService.getCurrentUser();
        return jdbcTemplate.query(
                "select * from comment where product_id = ? AND person_id != ? order by created_at DESC",
                new Object[]{productId, person.getId()},
                new BeanPropertyRowMapper<>(Comment.class)
        );
    }

    public void save(int productId, String comment) {
        Person person = personDetailsService.getCurrentUser();
        jdbcTemplate.update(
                "insert into comment(person_id, product_id, comment, created_at) values (?,?,?,?)",
                person.getId(),
                productId,
                comment,
                LocalDateTime.now()
        );
    }

    public void delete(int productId) {
        Person person = personDetailsService.getCurrentUser();
        jdbcTemplate.update(
                "delete from comment where person_id = ? and product_id =?",
                person.getId(),
                productId
        );
    }

}
