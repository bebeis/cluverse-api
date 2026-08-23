# devlog-9 home-feed AWS measurement

## Conditions

- API: `https://api.cluverse.cona.team`
- Application: two t3.small ECS tasks
- Database: MySQL 8.0 on t3.small
- Total rows at final verification: posts 3,029,701 / comments 11,865 / activity rows 2,803
- Benchmark fixture: posts 1,000 / comments 10,000 plus 62 successful write-test comments
- Comment distribution: 20% concentrated on one post
- Read load: closed model, 4 VUs, 60 seconds for V2/V3
- V1 safety limit: 1 VU, one request; upstream returned failure at 60 seconds
- Write load: 2 req/s, 30 seconds

V1 was not loaded with 4 VUs because a single request exceeded the 60-second upstream boundary and the database query continued after the client response. The V1 result is a timeout boundary, not a percentile distribution comparable to V2/V3.

## Read API

| Version | Structure | p95 | p99 | throughput | success rate |
|---|---|---:|---:|---:|---:|
| V1 | request-time GROUP BY | 60,038.6ms timeout | 60,038.6ms timeout | 0.02 req/s | 0% |
| V2 | Loose Index Scan + snapshot cache | 54.0ms | 153.9ms | 163.59 req/s | 100% |
| V3 | write-time activity projection | 39.3ms | 152.1ms | 177.85 req/s | 100% |

At the same 4-VU load, V3 versus V2 improved p95 by 27.2% and throughput by 8.7%. Their p99 values were effectively the same. V1 cannot be expressed as a normal p95 improvement because only one timed-out request was allowed for safety.

## EXPLAIN ANALYZE

| Version | execution time | compared index rows | additional observation |
|---|---:|---:|---|
| V1 | 171,634ms | 11,820 comment rows | about 3.02 million post and author lookups; temporary aggregation and sort |
| V2 | 17.2ms | 2,803 grouped rows | Loose Index Scan; cold snapshot refresh cost |
| V3 | 0.101ms | 10 activity rows | latest-activity index prefix only |

V2's API result is warm-cache latency, while its EXPLAIN value is the cold snapshot refresh query. These values must not be treated as the same operation.

## V2 cache

- 90-second Prometheus window hit increase: 11,803.28
- total cache-request increase: 11,808.08
- estimated hit ratio: 99.96%
- fallback increase: 0

## Write guardrail and consistency

| Metric | Result |
|---|---:|
| comment write p95 | 193.2ms |
| comment write p99 | 207.2ms |
| write success rate | 100% (61/61) |
| benchmark activity mismatch before writes | 0 |
| benchmark activity mismatch after writes | 1 |

The mismatched row had the correct `last_comment_id` but `last_commented_at` was one second earlier than the source comment. V2 returned `2026-08-22T15:10:09`, while V3 returned `2026-08-22T15:10:08` for the same post. Therefore the blog must not claim zero source/projection mismatches until this timestamp precision issue is fixed and remeasured.

## Grafana

- Dashboard: `Cluverse Home Recent Comments Three Stages`
- URL path: `/d/ffvym9f3ov20wc/cluverse-home-recent-comments-three-stages`
- V1–V3 combined: `from=1787410502000&to=1787410707000&timezone=Asia%2FSeoul&orgId=1`
- Write: `from=1787411376000&to=1787411406000&timezone=Asia%2FSeoul&orgId=1`

## Primary artifacts

- V1 k6: `../raw/2026-08-22-235501-read-v1-comments10000-posts1000.html`
- V2 k6: `../raw/2026-08-22-235620-read-v2-comments10000-posts1000.html`
- V3 k6: `../raw/2026-08-22-235727-read-v3-comments10000-posts1000.html`
- write k6: `../raw/2026-08-23-000936-write-v3.html`
- normalized measurements: `measurements.csv`
- comparison image: `home-feed-aws-comparison.png`

