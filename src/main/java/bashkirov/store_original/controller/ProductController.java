package bashkirov.store_original.controller;

import bashkirov.store_original.dto.ProductPhotoDto;
import bashkirov.store_original.dto.ProductSaleDto;
import bashkirov.store_original.enumeration.Role;
import bashkirov.store_original.model.Product;
import bashkirov.store_original.security.PersonDetails;
import bashkirov.store_original.service.*;
import bashkirov.store_original.validation.ProductSaleValidator;
import bashkirov.store_original.validation.ProductValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final ProductValidator productValidator;
    private final CategoryService categoryService;
    private final PhotoService photoService;
    private final CartItemService cartItemService;
    private final CommentService commentService;
    private final ProductSaleValidator productSaleValidator;

    @GetMapping()
    public String search(
            @RequestParam(required = false, name = "key") String key,
            @RequestParam(required = false, name = "categoryId") Integer categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model,
            @AuthenticationPrincipal PersonDetails personDetails
    ) {
        boolean isAdmin = personDetails != null &&
                personDetails.person().getRole().equals(Role.ROLE_ADMIN);
        model.addAttribute("admin", isAdmin);

        int totalProducts = (categoryId == null) ?
                productService.countProducts() :
                productService.countProductsByCategory(categoryId);
        int totalPages = (int) Math.ceil((double) totalProducts / size);

        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPage", page);
        model.addAttribute("categories", categoryService.getAll());
        model.addAttribute("key", key);
        model.addAttribute("categoryId", categoryId);

//        if (categoryId == null) {
//            model.addAttribute("searchList", productService.search(key, null, page, size));
//        } else {
//            model.addAttribute("searchList", productService.getAllByCategoryId(categoryId, page, size));
//        }
        //знижки
        List<ProductPhotoDto> products = productService.searchWithDiscount(key, categoryId, page, size);
        model.addAttribute("searchList", products);


        model.addAttribute("productSaleDtoList", productService.getAllProductSaleDto());


        return "product/products-page";
    }

    @GetMapping("/{id}")
    public String getById(
            @PathVariable("id") int id,
            Model model,
            @AuthenticationPrincipal PersonDetails personDetails
    ) {
        boolean isAdmin = personDetails != null &&
                personDetails.person().getRole().equals(Role.ROLE_ADMIN);
        model.addAttribute("admin", isAdmin);

        Product product = productService.getById(id);
        Optional<ProductSaleDto> productSaleDto = productService.getOptionalProductSaleDto(product.getId());
        model.addAttribute("product", product);
        model.addAttribute("hasProductSaleDto", productSaleDto.isPresent());
        model.addAttribute("productSaleDto", productSaleDto.orElse(null));
        model.addAttribute("actualPrice", productService.getActualPrice(product));

        if (personDetails != null) {
            model.addAttribute("commentUser",
                    commentService.getOptionalUserComment(id).orElse(null));
        }

        model.addAttribute("comments", commentService.getAllProductComments(id));
        model.addAttribute("isPresentInCart", cartItemService.isProductPresentInCart(id));
        model.addAttribute("productWithPhotos", productService.getProductWithPhotos(id));
        return "product/product-page";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/cancel-sale/{id}")
    public String cancelSale(
            @PathVariable("id") int productId
    ) {
        productService.cancelSaleProduct(productId);
        return "redirect:/product/" + productId;
    }

    @GetMapping("/sales")
    public String showAllSaleProducts(
            Model model,
            @AuthenticationPrincipal PersonDetails personDetails

    ) {
        boolean isAdmin = personDetails != null &&
                personDetails.person().getRole().equals(Role.ROLE_ADMIN);

        model.addAttribute("admin", isAdmin);
        model.addAttribute("productSaleDtoList", productService.getAllProductSaleDto());
        return "sale/sales-page";
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
            @RequestParam("multipartFilesArray") MultipartFile[] multipartFiles,
            @Valid @ModelAttribute("productNew") Product productNew,
            BindingResult bindingResult
    ) {

        productValidator.validate(productNew, bindingResult);
        if (bindingResult.hasErrors()) {
            return "product/product-new-page";
        }
        productService.save(productNew, multipartFile, true, multipartFiles);
        return "redirect:/product";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/edit/{id}")
    public String productEditPage(
            @PathVariable("id") int id,
            Model model
    ) {
        model.addAttribute("photoPrimary", photoService.getPrimaryPhotoByProductId(id));
        model.addAttribute("photoAll", productService.getAllPhotoByProductId(id));
        model.addAttribute("productUpdate", productService.getById(id));
        model.addAttribute("categoriesUpdate", categoryService.getAll());
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

    @DeleteMapping("/delete-photo/{photoId}")
    public String deletePhotoById(
            @RequestParam("productId") int productId,
            @PathVariable("photoId") int photoId
    ) {
        productService.delete(photoId);
        return "redirect:/product/edit/" + productId;
    }

    @PutMapping("/edit-primary-photo/{photoId}")
    public String setPhotoPrimary(
            @RequestParam("productId") int productId,
            @PathVariable("photoId") int photoId
    ) {
        photoService.setPrimary(photoId, productId);
        return "redirect:/product/edit/" + productId;
    }

    @PutMapping("/add-photos/{productId}")
    public String addPhotos(
            @PathVariable("productId") int productId,
            @RequestParam("multipartFilesArray") MultipartFile[] multipartFiles

    ) {
        Product product = productService.getById(productId);
        photoService.saveAll(multipartFiles, product);
        return "redirect:/product/edit/" + productId;
    }

    @DeleteMapping("/{id}")
    public String delete(
            @PathVariable("id") int id
    ) {
        productService.delete(id);
        return "redirect:/product";
    }

    @GetMapping("/sale/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String salePage(
            @PathVariable("id") int productId,
//            @ModelAttribute("productPhotoDto") ProductPhotoDto productPhotoDto,
//            @ModelAttribute("productSaleDto") ProductSaleDto productSaleDto,
            Model model
    ) {
        Product product = productService.getById(productId);
        ProductPhotoDto productPhotoDtoById = new ProductPhotoDto(product, photoService.getPrimaryPhotoByProductId(product.getId()));
        ProductSaleDto productSaleDto = new ProductSaleDto(productPhotoDtoById, product.getPrice(),1);

        model.addAttribute("product", product);
        model.addAttribute("productPhotoDto", productPhotoDtoById);
        model.addAttribute("productSaleDto", productSaleDto);
        return "sale/sale-page";
    }

    @PutMapping("/sale/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateSaleProduct(
            @PathVariable("id") int productId,
//            @ModelAttribute("productPhotoDto") ProductPhotoDto productPhotoDto,
            @Valid @ModelAttribute("productSaleDto") ProductSaleDto productSaleDto,
            BindingResult bindingResult
//            Model model
    ) {
//        productSaleValidator.validate(productSaleDto, bindingResult);
//        if (bindingResult.hasErrors()) {
//            return "sale/sale-page";
//        }
//        model.addAttribute("product", productService.getById(productId));
        productService.addSaleProduct(productSaleDto);
        return "redirect:/product/" + productId;
//                + productSaleDto.getProductPhotoDto().getProduct().getId();
    }


    // ДЗ:
    // (+) додати кнопки, додати логіку:
    // (+) бачити звичайне інфо продукта
    // (+)скасувати акцію -> кнопка
    // (+)доробити чат
//********************************************
    // * сторінки зробити,
    // * якщо є акція, бачити інфо акції, якщо немає акції,
//********************************************

    // наступне заняття: телеграм

}