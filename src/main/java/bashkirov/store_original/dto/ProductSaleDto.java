package bashkirov.store_original.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductSaleDto {
    private ProductPhotoDto productPhotoDto;

    @Min(value = 1, message = "Sale price cannot be less 0")
    private Double salePrice;

    @Min(value = 1, message = "Hours cannot be less 1")
    private int hours;

    @Override
    public String toString() {
        if (salePrice != null && hours != 0) {
            int hoursLeft = (int) Duration.between(
                    LocalDateTime.now(),
                    productPhotoDto.getProduct().getDateExpired()).toHours();

            String url = "http://localhost:8080/product/" + productPhotoDto.getProduct().getId();

            StringBuilder sb = new StringBuilder();

            sb
                    .append("https://bashkirovbank.fra1.digitaloceanspaces.com/").append(productPhotoDto.getProductPhoto().getUrl())
                    .append("\nПродукт: ").append(productPhotoDto.getProduct().getTitle())
                    .append(", \nстара ціна: ").append(productPhotoDto.getProduct().getPrice())
                    .append(", \nНОВА ЦІНА:").append(productPhotoDto.getProduct().getSalePrice())
                    .append(", \nакція діє ще: ").append(hoursLeft).append(" годин \n")
                    .append(url).append("\n");

            return sb.toString();
        }
        return "";
    }
}
