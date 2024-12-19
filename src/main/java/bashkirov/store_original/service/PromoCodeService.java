package bashkirov.store_original.service;

import bashkirov.store_original.model.PromoCode;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PromoCodeService {
    private final JdbcTemplate jdbcTemplate;

    public Optional<PromoCode> getValidPromoCode(String code, Integer categoryId) {
        return jdbcTemplate.query(
                "select * from promo_code where code = ? and expiration_date > NOW()" +
                        "and (category_id IS NULL or category_id = ?)",
                new Object[]{code, categoryId},
                getPromoCodeRowMapper()
        ).stream().findAny();
    }

    public List<PromoCode> getAllValidPromoCodes() {
        return jdbcTemplate.query(
                "select * from promo_code where expiration_date > NOW() order by id",
                getPromoCodeRowMapper()
        );
    }

    public void savePromoCode(PromoCode promoCode) {
        jdbcTemplate.update(
                "insert into promo_code(code, discount, expiration_date, is_percentage, category_id) values (?,?,?,?,?)",
                promoCode.getCode(),
                promoCode.getDiscount(),
                promoCode.getExpirationDate(),
                promoCode.isPercentage(),
                promoCode.getCategoryId()
        );
    }

    public PromoCode getPromoCodeById(int promoCodeId) {
        return jdbcTemplate.query(
                "select * from promo_code where id = ?",
                new Object[]{promoCodeId},
                getPromoCodeRowMapper()
        ).stream().findAny().orElseThrow();
    }

    public List<PromoCode> getAllPromoCodes(){
        return jdbcTemplate.query(
                "select * from promo_code order by id desc",
                getPromoCodeRowMapper()
        );
    }

    @NotNull
    private static RowMapper<PromoCode> getPromoCodeRowMapper() {
        return (rs, rowNum) -> {
            PromoCode promoCode = new PromoCode();
            promoCode.setId(rs.getInt("id"));
            promoCode.setCode(rs.getString("code"));
            promoCode.setDiscount(rs.getDouble("discount"));
            promoCode.setExpirationDate(rs.getTimestamp("expiration_date").toLocalDateTime());
            promoCode.setPercentage(rs.getBoolean("is_percentage"));
            promoCode.setCategoryId(rs.getInt("category_id"));
            return promoCode;
        };
    }
}
