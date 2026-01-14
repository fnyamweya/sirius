package com.sirius.api.service;

import com.sirius.api.dto.LedgerEntriesQueryRequest;
import com.sirius.api.dto.LedgerEntryResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class LedgerQueryService {

    private final JdbcTemplate jdbc;

    public LedgerQueryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<LedgerEntryResponse> query(String marketId, UUID orgId, LedgerEntriesQueryRequest req) {
        int page = req.page() == null ? 0 : req.page();
        int size = req.size() == null ? 100 : req.size();

        StringBuilder sql = new StringBuilder();
        List<Object> args = new ArrayList<>();

        sql.append("select id, account_id, transfer_id, direction, currency, amount_minor, occurred_at, entry_hash ")
           .append("from ledger_entries where market_id = ? and org_id = ? ");
        args.add(marketId);
        args.add(orgId);

        if (req.account_id() != null) {
            sql.append("and account_id = ? ");
            args.add(req.account_id());
        }
        if (req.transfer_id() != null) {
            sql.append("and transfer_id = ? ");
            args.add(req.transfer_id());
        }
        if (req.direction() != null && !req.direction().isBlank()) {
            sql.append("and direction = ? ");
            args.add(req.direction().toUpperCase());
        }
        if (req.from() != null) {
            sql.append("and occurred_at >= ? ");
            args.add(Timestamp.from(req.from()));
        }
        if (req.to() != null) {
            sql.append("and occurred_at <= ? ");
            args.add(Timestamp.from(req.to()));
        }

        sql.append("order by occurred_at desc ");
        sql.append("limit ? offset ? ");
        args.add(size);
        args.add((long) page * size);

        return jdbc.query(sql.toString(), (rs, rowNum) -> new LedgerEntryResponse(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("account_id")),
                rs.getString("transfer_id") == null ? null : UUID.fromString(rs.getString("transfer_id")),
                rs.getString("direction"),
                rs.getString("currency"),
                rs.getLong("amount_minor"),
                rs.getTimestamp("occurred_at").toInstant(),
                Base64.getEncoder().encodeToString(rs.getBytes("entry_hash"))
        ), args.toArray());
    }
}
