package bashkirov.store_original.controller;

import bashkirov.store_original.model.Product;
import bashkirov.store_original.service.CategoryService;
import bashkirov.store_original.service.ProductService;
import bashkirov.store_original.validation.CategoryValidator;
import bashkirov.store_original.validation.ProductValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final ProductValidator productValidator;
    private final CategoryService categoryService;
    private final CategoryValidator categoryValidator;

    @GetMapping()
    public String search(
            @RequestParam(required = false, name = "key") String key,
            @RequestParam(required = false, name = "categoryId") Integer categoryId,
            Model model
    ) {
        model.addAttribute("categories", categoryService.getAll());
        model.addAttribute("key", key);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("searchList", productService.search(key, categoryId));
        return "product/products-page";
    }

    @GetMapping("/{id}")
    public String getById(
            @PathVariable("id") int id,
            Model model
    ) {

        model.addAttribute("productWithPhotos", productService.getProductWithPhotos(id));
        return "product/product-page";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/new")
    public String productNewPage(
            @ModelAttribute("productNew") Product productNew,
            Model model
    ) {
        model.addAttribute("categories", categoryService.getAll());
        return "product/product-new-page";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public String save(
            @RequestParam("multipartFile") MultipartFile multipartFile,
            @Valid @ModelAttribute("productNew") Product productNew,
            BindingResult bindingResult
    ) {

        productValidator.validate(productNew, bindingResult);
        if (bindingResult.hasErrors()) {
            return "product/product-new-page";
        }
        productService.save(productNew, multipartFile, true);
        return "redirect:/product";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/edit/{id}")
    public String productEditPage(
            @PathVariable("id") int id,
            @ModelAttribute("productUpdate") Product productUpdate
    ) {
        return "product/product-update-page";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public String update(
            @PathVariable("id") int id,
            @Valid @ModelAttribute("productUpdate") Product productUpdate,
            BindingResult bindingResult
    ) {
        productValidator.validate(productUpdate, bindingResult);
        if (bindingResult.hasErrors()) {
            return "product/product-update-page";
        }
        productService.update(id, productUpdate);
        return "redirect:/product/" + id;
    }

    @DeleteMapping("/{id}")
    public String delete(
            @PathVariable("id") int id
    ) {
        productService.delete(id);
        return "redirect:/product";
    }

    // html сторінки -> зміна продукту, категорії,


}