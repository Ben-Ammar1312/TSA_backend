package org.example.tas_backend.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import lombok.RequiredArgsConstructor;
import org.example.tas_backend.dtos.RevisionSummaryDTO;
import org.example.tas_backend.envers.RevInfo;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditHistoryService {
    private final EntityManager em;

    public <T> List<RevisionSummaryDTO> historyOf(
            Class<T> type, Object id, List<String> trackedProps) {

        var reader = AuditReaderFactory.get(em);
        var revs = reader.getRevisions(type, id);
        PersistenceUnitUtil idUtil = em.getEntityManagerFactory().getPersistenceUnitUtil();

        List<RevisionSummaryDTO> out = new ArrayList<>(revs.size());
        for (Number rev : revs) {
            RevInfo meta = reader.findRevision(RevInfo.class, rev);
            Instant at = Instant.ofEpochMilli(meta.getTimestamp());
            String by = meta.getActor();
            Object entityState = reader.find(type, id, rev);
            var bean = entityState != null ? new BeanWrapperImpl(entityState) : null;

            List<String> changed = new ArrayList<>();
            Map<String, String> changedValues = new HashMap<>();
            for (String p : trackedProps) {
                boolean changedHere = !reader.createQuery()
                        .forRevisionsOfEntity(type, false, true)
                        .add(AuditEntity.id().eq(id))
                        .add(AuditEntity.revisionNumber().eq(rev))
                        .add(AuditEntity.property(p).hasChanged())
                        .getResultList().isEmpty();
                if (changedHere) {
                    changed.add(p);
                    String value = bean != null ? formatValue(bean.getPropertyValue(p), idUtil) : "unavailable";
                    changedValues.put(p, value);
            }
            }

            out.add(new RevisionSummaryDTO(type.getSimpleName(), id, rev, at, by, changed, changedValues));
        }
        return out;
    }

    /** Latest changes feed across selected audited types. Keep the set small. */
    public List<RevisionSummaryDTO> latestChanges(Map<Class<?>, List<String>> types, int limitPerType) {
        var reader = AuditReaderFactory.get(em);
        var feed = new ArrayList<RevisionSummaryDTO>();
        PersistenceUnitUtil idUtil = em.getEntityManagerFactory().getPersistenceUnitUtil();

        for (var e : types.entrySet()) {
            Class<?> type = e.getKey();
            List<String> props = e.getValue();

            @SuppressWarnings("unchecked")
            List<Object[]> rows = reader.createQuery()
                    .forRevisionsOfEntity(type, false, true)
                    .addOrder(AuditEntity.revisionNumber().desc())
                    .setMaxResults(limitPerType)
                    .getResultList();

            for (Object[] row : rows) {
                Object entityAtRev = row[0];
                RevInfo meta = (RevInfo) row[1];
                var bean = entityAtRev != null ? new BeanWrapperImpl(entityAtRev) : null;


                Object id = em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entityAtRev);
                Number rev = meta.getId();
                Instant at = Instant.ofEpochMilli(meta.getTimestamp());
                String by = meta.getActor();

                List<String> changed = new ArrayList<>();
                Map<String, String> changedValues = new HashMap<>();
                for (String p : props) {
                    boolean changedHere = !reader.createQuery()
                            .forRevisionsOfEntity(type, false, true)
                            .add(AuditEntity.id().eq(id))
                            .add(AuditEntity.revisionNumber().eq(rev))
                            .add(AuditEntity.property(p).hasChanged())
                            .getResultList().isEmpty();
                    if (changedHere) {
                        changed.add(p);
                        String value = bean != null ? formatValue(bean.getPropertyValue(p), idUtil) : "unavailable";
                        changedValues.put(p, value);
                }
                }

                feed.add(new RevisionSummaryDTO(type.getSimpleName(), id, rev, at, by, changed, changedValues));
            }
        }

        feed.sort(Comparator.comparing(RevisionSummaryDTO::at).reversed());
        return feed;
    }

    private String formatValue(Object val, PersistenceUnitUtil idUtil) {
        if (val == null) return "null";
        try {
            if (val instanceof Iterable<?> iterable) {
                int count = 0;
                for (Object ignored : iterable) count++;
                return "items=" + count;
            }
            if (val.getClass().isArray()) {
                return "items=" + java.lang.reflect.Array.getLength(val);
            }
            // If it's an entity, show Class#id to avoid deep serialization
            Object maybeId = idUtil.getIdentifier(val);
            if (maybeId != null) {
                return val.getClass().getSimpleName() + "#" + maybeId;
            }
        } catch (Exception ignored) {
            // fallback below
        }
        String s = val.toString();
        if (s.length() > 140) s = s.substring(0, 137) + "...";
        return s;
    }
}
