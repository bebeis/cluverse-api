# Devlog 9 AWS scale evidence

## Environment

- Measured at: 2026-08-22~23 Asia/Seoul
- Application: two t3.small ECS tasks
- Database: MySQL 8.0 on t3.small
- Phase 1 fixture: 1,000 benchmark posts, 10,000 benchmark comments, 20% hot comments
- Phase 2 fixture: 800,001 benchmark posts, 1,000,000 benchmark comments, 20% hot comments
- Phase 2 distribution: 200,000 comments on one hot post and one comment on each of the other 800,000 posts

## Phase 1: V1 to V2

| version | API result | DB evidence |
|---|---:|---:|
| V1 request-time aggregate | one request timed out at 60,038.6ms | 171.6s EXPLAIN ANALYZE; about 3.02M post/author lookups |
| V2 loose-index snapshot | p95 54.0ms, p99 153.9ms | 17.2ms snapshot query; 2,803 grouped rows |

V1 was intentionally limited to one request because the query continued running after the upstream timeout.

## Phase 2: V2 to V3

| version | p95 | p99 | max | throughput | failure | DB elapsed | core index rows |
|---|---:|---:|---:|---:|---:|---:|---:|
| V2 index aggregate + cache | 29.6ms | 85.8ms | 2,135.3ms | 183.85 req/s | 0% | 1,483.0ms | 1,000,000 |
| V3 activity projection | 33.5ms | 152.0ms | 294.9ms | 182.52 req/s | 0% | 5.24ms | 10 |

At one million comments and 801,804 total commented-post groups, MySQL changed V2 from a loose index grouping plan to a full covering-index scan followed by temporary aggregation and filesort. The synchronous per-instance Caffeine refresh caused the V2 maximum API latency spike. V3 did not improve the common cache-hit p95; it removed the periodic query whose cost grew with the number of comment groups.

Prometheus over the cold-check and 70-second V2 interval recorded 6 cache misses, 13,656 hits, and 0 fallbacks, for a 99.96% hit rate. The rare misses explain why p95 and p99 hide the refresh stall while the maximum and the cold EXPLAIN expose it.

## Integrity

- Benchmark posts: 800,001
- Benchmark comments: 1,000,000
- Benchmark activity rows: 800,001
- Benchmark source-to-projection mismatches: 0
- V2/V3 top-10 API responses equal: true
- Global mismatches outside this fixture: 843, all owned by `benchmark-popularity-inline`

The global mismatch count is not attributed to the home-feed fixture.

## Capture files

- Phase 1 V1 HTML: `script/home-feed/results/raw/2026-08-22-235501-read-v1-comments10000-posts1000.html`
- Phase 1 V2 HTML: `script/home-feed/results/raw/2026-08-22-235620-read-v2-comments10000-posts1000.html`
- Phase 2 V2 HTML: `script/home-feed/results/raw/2026-08-23-004820-read-v2-comments1000000-posts800001.html`
- Phase 2 V3 HTML: `script/home-feed/results/raw/2026-08-23-004708-read-v3-comments1000000-posts800001.html`
- Phase 2 chart PNG: `script/home-feed/results/2026-08-23-aws-1m/home-feed-scale-evolution.png`
- Phase 2 chart SVG: `script/home-feed/results/2026-08-23-aws-1m/home-feed-scale-evolution.svg`
- Blog asset PNG: `docs/blog/assets/devlog-9/home-feed-scale-evolution.png`

## Grafana windows

- V3: `from=1787413623000&to=1787413693000&timezone=Asia%2FSeoul&orgId=1`
- V2 cold refresh: `from=1787413696000&to=1787413775000&timezone=Asia%2FSeoul&orgId=1`
- Combined V2/V3: `from=1787413623000&to=1787413775000&timezone=Asia%2FSeoul&orgId=1`

Dashboard path: `/d/ffvym9f3ov20wc/cluverse-home-recent-comments-three-stages`
