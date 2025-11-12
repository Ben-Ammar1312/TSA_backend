package org.example.tas_backend.envers;

import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;


public class UserRevisionListener implements RevisionListener {
    @Override public void newRevision(Object entity) {
        RevInfo rev = (RevInfo) entity;

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            String fullName =
                    coalesce(jwt.getClaimAsString("name"),
                            join(jwt.getClaimAsString("given_name"), jwt.getClaimAsString("family_name")),
                            jwt.getClaimAsString("preferred_username"),
                            jwt.getSubject()); // fallback

            rev.setActor(fullName);
            // optional: keep the stable id too
            rev.setActorId(jwt.getSubject());
        } else {
            rev.setActor("anonymous");
            rev.setActorId("anonymous");
        }
    }

    private static String coalesce(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return v;
        return "unknown";
    }
    private static String join(String a, String b) {
        a = a == null ? "" : a.trim();
        b = b == null ? "" : b.trim();
        return (a + " " + b).trim();
    }
}