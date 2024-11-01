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

    public Optional<PromoCode> getValidPromoCode(String code) {
        return jdbcTemplate.query(
                "select * from promo_code where code = ? and expiration_date > NOW()",
                new Object[]{code},
                getPromoCodeRowMapper()
        ).stream().findAny();
    }

    public List<PromoCode> getAllValidPromoCodes() {
        return jdbcTemplate.query(
                "select * from promo_code where expiration_date > NOW()",
                getPromoCodeRowMapper()
        );
    }

    public void savePromoCode(PromoCode promoCode) {
        jdbcTemplate.update(
                "insert into promo_code(code, discount, expiration_date, is_percentage) values (?,?,?,?)",
                promoCode.getCode(),
                promoCode.getDiscount(),
                promoCode.getExpirationDate(),
                promoCode.isPercentage()
        );
    }

    public PromoCode getPromoCodeById(int promocodeId) {
        return jdbcTemplate.query(
                "select * from promo_code where id = ?",
                new Object[]{promocodeId},
                getPromoCodeRowMapper()
        ).stream().findAny().orElseThrow();
    }

    public List<PromoCode> getAllPromoCode(){
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
            return promoCode;
        };
    }
}
