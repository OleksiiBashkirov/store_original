package bashkirov.store_original.controller;

import bashkirov.store_original.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/comment")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping
    public String save(
            @RequestParam("productId") int productId,
            @RequestParam("comment") String comment
    ) {
        commentService.save(productId, comment);
        return "redirect:/product/" + productId;
    }

    @DeleteMapping
    public String delete(
            @RequestParam("productId") int productId
    ) {
        commentService.delete(productId);
        return "redirect:/product/" + productId;
    }
}
