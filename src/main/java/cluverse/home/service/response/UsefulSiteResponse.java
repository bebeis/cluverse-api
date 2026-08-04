package cluverse.home.service.response;

import cluverse.home.domain.UsefulSite;

public record UsefulSiteResponse(
        String name,
        String description,
        String url
) {
    public static UsefulSiteResponse from(UsefulSite site) {
        return new UsefulSiteResponse(site.name(), site.description(), site.url());
    }
}
