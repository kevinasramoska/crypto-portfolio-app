package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Replay existing active positions from retained input, not the old rounded holding average. */
public class V7__rebuild_precise_positions extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        var connection = context.getConnection();
        try (var ledger = connection.prepareStatement("SELECT id, user_id, coin_id, type, quantity, price_usd "
                + "FROM transactions WHERE voided_at IS NULL ORDER BY user_id, coin_id, ledger_sequence");
             var profit = connection.prepareStatement("UPDATE transactions SET realised_profit_usd = ? WHERE id = ?");
             var holding = connection.prepareStatement("UPDATE holdings SET cost_basis_usd = ?, average_buy_price_usd = ? "
                     + "WHERE user_id = ? AND coin_id = ?")) {
            ledger.setFetchSize(500);
            long user = -1, coin = -1;
            BigDecimal quantity = BigDecimal.ZERO, cost = BigDecimal.ZERO;
            try (var rows = ledger.executeQuery()) {
                while (rows.next()) {
                    long nextUser = rows.getLong("user_id"), nextCoin = rows.getLong("coin_id");
                    if (nextUser != user || nextCoin != coin) {
                        updateHolding(holding, user, coin, quantity, cost);
                        user = nextUser;
                        coin = nextCoin;
                        quantity = BigDecimal.ZERO;
                        cost = BigDecimal.ZERO;
                    }
                    BigDecimal amount = rows.getBigDecimal("quantity");
                    BigDecimal value = amount.multiply(rows.getBigDecimal("price_usd"));
                    BigDecimal realised = BigDecimal.ZERO;
                    if ("BUY".equals(rows.getString("type"))) {
                        quantity = quantity.add(amount);
                        cost = cost.add(value);
                    } else {
                        if (quantity.compareTo(amount) < 0) throw new SQLException("Invalid legacy ledger: historical oversell");
                        BigDecimal allocated = amount.compareTo(quantity) == 0 ? cost
                                : cost.multiply(amount).divide(quantity, 20, RoundingMode.HALF_UP);
                        realised = value.subtract(allocated);
                        quantity = quantity.subtract(amount);
                        cost = cost.subtract(allocated);
                    }
                    profit.setBigDecimal(1, realised);
                    profit.setLong(2, rows.getLong("id"));
                    profit.executeUpdate();
                }
            }
            updateHolding(holding, user, coin, quantity, cost);
        }
    }

    private void updateHolding(PreparedStatement statement, long user, long coin, BigDecimal quantity, BigDecimal cost)
            throws SQLException {
        if (user < 0 || quantity.signum() == 0) return;
        statement.setBigDecimal(1, cost);
        statement.setBigDecimal(2, cost.divide(quantity, 12, RoundingMode.HALF_UP));
        statement.setLong(3, user);
        statement.setLong(4, coin);
        statement.executeUpdate();
    }
}
