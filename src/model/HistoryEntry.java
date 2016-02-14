package model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.time.*;

/** Stores the values for Applications and environment */
public class HistoryEntry {

    private UUID uuid;
    private Instant retrieved;
    private Instant periodEnd;
    private String environment;
    private String name;

    public HistoryEntry() {
        this.setUUID((UUID) null);
        this.setRetrieved(null);
        this.setPeriodEnd(null);
        this.setEnvironment(null);
        this.setName(null);
    }

    public HistoryEntry(HistoryEntry entry) {
        this.setUUID(entry.getUUID());
        this.setRetrieved(entry.getRetrieved());
        this.setPeriodEnd(entry.getPeriodEnd());
        this.setEnvironment(entry.getEnvironment());
        this.setName(entry.getName());
    }

    public HistoryEntry(ResultSet rs) throws SQLException {
        this.setUUID(rs.getString("ID"));
        this.setRetrieved(rs.getTimestamp("RETRIEVED").toInstant());
        this.setPeriodEnd(rs.getTimestamp("PERIOD_END").toInstant());
        this.setEnvironment(rs.getString("ENVIRONMENT"));
        this.setName(null);
    }

    public final UUID getUUID() {
        return uuid;
    }

    public final Instant getRetrieved() {
        return retrieved;
    }

    public final Instant getPeriodEnd() {
        return periodEnd;
    }

    public final String getEnvironment() {
        return environment;
    }

    public final String getName() {
        return name;
    }

    private  void setUUID(String uuid) {
        this.setUUID(UUID.fromString(uuid));
    }

    private  void setUUID(UUID uuid) {
        this.uuid = uuid;

        if (this.uuid == null) {
            this.uuid = UUID.randomUUID();
        }
    }

    public final void setRetrieved(Instant retrieved) {
        this.retrieved = retrieved;

        if (this.retrieved == null) {
            this.retrieved =  Instant.now(null);
        }
    }

    public final void setPeriodEnd(Instant periodEnd) {
        this.periodEnd = periodEnd;

        if (this.periodEnd == null) {
            this.periodEnd = Instant.now(null);
        }
    }

    public final void setEnvironment(String environment) {
        this.environment = environment;

        if (this.environment == null) {
            this.environment = "";
        }
    }

    public final void setName(String name) {
        this.name = name;

        if (this.name == null) {
            this.name = "";
        }
    }

    @Override
    public String toString() {
        return String.format("{ '%s', '%s', '%s', '%s', '%s' }", uuid,
                retrieved, periodEnd, environment, name);
    }
}
