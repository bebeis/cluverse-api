// 고정된 검사 범위에서 V1/V2의 승격 결과와 기대 집합을 대조한다.
import http from 'k6/http';
import { check } from 'k6';
import { Gauge } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const BENCHMARK_HEADER = __ENV.BENCHMARK_HEADER || 'X-Benchmark-Token';
const BENCHMARK_TOKEN = __ENV.BENCHMARK_TOKEN || '';
const SIZE = Number(__ENV.SIZE || 100);

function parseIds(text, name) {
    const values = String(text || '')
        .split(',')
        .map((value) => value.trim())
        .filter(Boolean)
        .map(Number);
    if (values.length === 0 || values.some((value) => !Number.isSafeInteger(value) || value <= 0)) {
        throw new Error(`${name}에는 양의 postId를 쉼표로 전달해야 합니다.`);
    }
    return [...new Set(values)];
}

const EXPECTED_POST_IDS = parseIds(__ENV.EXPECTED_POST_IDS, 'EXPECTED_POST_IDS');
const CHECK_POST_IDS = parseIds(__ENV.CHECK_POST_IDS, 'CHECK_POST_IDS');
const expectedSet = new Set(EXPECTED_POST_IDS);
const scopeSet = new Set(CHECK_POST_IDS);

for (const postId of expectedSet) {
    if (!scopeSet.has(postId)) throw new Error(`기대 ID ${postId}가 CHECK_POST_IDS에 없습니다.`);
}

const headers = { Accept: 'application/json' };
if (BENCHMARK_TOKEN) headers[BENCHMARK_HEADER] = BENCHMARK_TOKEN;

export const options = {
    scenarios: {
        recall_once: {
            executor: 'shared-iterations',
            vus: 1,
            iterations: 1,
            maxDuration: '2m',
        },
    },
    thresholds: {
        popularity_recall_ratio: ['value==1'],
        popularity_false_promotion_ratio: ['value==0'],
        popularity_result_set_match: ['value==1'],
    },
};

const recallRatio = new Gauge('popularity_recall_ratio');
const falsePromotionRatio = new Gauge('popularity_false_promotion_ratio');
const resultSetMatch = new Gauge('popularity_result_set_match');

function assertApiResponse(response, label) {
    const ok = check(response, {
        [`${label} returned 2xx`]: (res) => res.status >= 200 && res.status < 300,
        [`${label} returned ApiResponse`]: (res) => {
            try {
                const code = Number(res.json('code'));
                return code >= 200 && code < 300;
            } catch (_error) {
                return false;
            }
        },
    });
    if (!ok) throw new Error(`${label} 실패: HTTP ${response.status}, ${String(response.body).slice(0, 300)}`);
}

function collectPostIds(value, output) {
    if (Array.isArray(value)) {
        value.forEach((item) => collectPostIds(item, output));
        return;
    }
    if (value === null || typeof value !== 'object') return;
    if (Number.isSafeInteger(Number(value.postId))) output.add(Number(value.postId));
    Object.values(value).forEach((item) => collectPostIds(item, output));
}

function queryResult(version) {
    const response = http.get(
        `${BASE_URL}/api/${version}/popular-posts/recent?size=${encodeURIComponent(String(SIZE))}`,
        { headers, tags: { name: 'popularity_recall_query', version } },
    );
    assertApiResponse(response, `${version} recent query`);
    const ids = new Set();
    collectPostIds(response.json('data'), ids);
    return new Set([...ids].filter((postId) => scopeSet.has(postId)));
}

function intersectionSize(left, right) {
    let count = 0;
    for (const value of left) if (right.has(value)) count += 1;
    return count;
}

function sameSet(left, right) {
    return left.size === right.size && intersectionSize(left, right) === left.size;
}

export default function () {
    const v1Run = http.post(`${BASE_URL}/api/v1/popular-posts/promotion-runs`, null, {
        headers,
        tags: { name: 'popularity_recall_v1_run', version: 'v1' },
    });
    assertApiResponse(v1Run, 'v1 promotion run');

    for (const postId of CHECK_POST_IDS) {
        const response = http.post(
            `${BASE_URL}/api/v2/popular-posts/${postId}/promotion-checks`,
            null,
            { headers, tags: { name: 'popularity_recall_v2_check', version: 'v2' } },
        );
        assertApiResponse(response, 'v2 promotion check');
    }

    const v1Result = queryResult('v1');
    const v2Result = queryResult('v2');
    const truePositiveCount = intersectionSize(v2Result, expectedSet);
    const falsePositiveCount = [...v2Result].filter((postId) => !expectedSet.has(postId)).length;
    const recall = expectedSet.size === 0 ? 1 : truePositiveCount / expectedSet.size;
    const falseRate = v2Result.size === 0 ? 0 : falsePositiveCount / v2Result.size;
    const matched = sameSet(v1Result, v2Result);

    recallRatio.add(recall);
    falsePromotionRatio.add(falseRate);
    resultSetMatch.add(matched ? 1 : 0);

    console.log(JSON.stringify({
        expected: [...expectedSet].sort((a, b) => a - b),
        v1: [...v1Result].sort((a, b) => a - b),
        v2: [...v2Result].sort((a, b) => a - b),
        recall,
        falsePromotionRatio: falseRate,
        resultSetMatch: matched,
    }));
}
