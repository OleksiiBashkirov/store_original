package bashkirov.store_original.controller;

import bashkirov.store_original.enumeration.Role;
import bashkirov.store_original.model.Category;
import bashkirov.store_original.security.PersonDetails;
import bashkirov.store_original.service.CategoryService;
import bashkirov.store_original.validation.CategoryValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/category")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;
    private final CategoryValidator categoryValidator;

    @GetMapping
    public String getAll(
            Model model,
            @AuthenticationPrincipal PersonDetails personDetails
    ) {
        if (personDetails.person().getRole().equals(Role.ROLE_ADMIN)) {
            model.addAttribute("admin", true);
        }
        model.addAttribute("categoriesAll", categoryService.getAll());
        return "category/categories-page";
    }

    @GetMapping("/new")
    public String categoryPage(
            @ModelAttribute("categoryNew") Category categoryNew,
            @AuthenticationPrincipal PersonDetails personDetails,
            Model model) {
        if (personDetails.person().getRole().equals(Role.ROLE_ADMIN)) {
            model.addAttribute("admin", true);
        }

        return "category/category-new-page";
    }

    @PostMapping
    public String save(
            @Valid @ModelAttribute("categoryNew") Category categoryNew,
            BindingResult bindingResult
    ) {
        categoryValidator.validate(categoryNew, bindingResult);
        if (bindingResult.hasErrors()) {
            return "category/category-new-page";
        }
        categoryService.save(categoryNew);
        return "redirect:/category";
    }
}
