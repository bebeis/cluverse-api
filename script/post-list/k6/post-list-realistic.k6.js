// 최종 목록 구조의 요청 분포를 재현한다.
// - 일반 목록 탐색은 V3 offset API
// - 날짜로 과거 글에 진입한 뒤의 탐색은 V4 cursor API
// RATE는 세션 시작률이 아니라 두 API를 합한 목표 HTTP RPS다.

import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { pickPage } from './lib/traffic-profile.js';

if (__ENV.VERSION) {
    throw new Error('realistic은 V3/V4 혼합 시나리오입니다. VERSION을 지정하지 마세요.');
}

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const BOARD_ID = Number(__ENV.BOARD_ID || 2001001);
const SIZE = Number(__ENV.SIZE || 20);
const CATEGORY = __ENV.CATEGORY;

const TOTAL_RATE = Number(__ENV.RATE || 100);
const V4_REQUEST_SHARE = Number(__ENV.V4_REQUEST_SHARE || 0.05);
const DURATION = __ENV.DURATION || '1m';
const GRACEFUL_STOP = __ENV.GRACEFUL_STOP || '10s';
const TOTAL_PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || Math.max(20, TOTAL_RATE));
const TOTAL_MAX_VUS = Number(__ENV.MAX_VUS || Math.max(100, TOTAL_RATE * 3));

const V3_MAX_PAGE = Number(__ENV.V3_MAX_PAGE || 500);
const CURSOR_MAX_DEPTH = Number(__ENV.CURSOR_MAX_DEPTH || 10);
const DATE_DAYS_BACK_MIN = Number(__ENV.DATE_DAYS_BACK_MIN || 1);
const DATE_DAYS_BACK_MAX = Number(__ENV.DATE_DAYS_BACK_MAX || 14);
const DATE_ANCHORS = (__ENV.DATE_ANCHORS || '')
    .split(',')
    .map((value) => value.trim())
    .filter(Boolean);

if (!Number.isInteger(TOTAL_RATE) || TOTAL_RATE < 2) {
    throw new Error('RATE는 2 이상의 정수여야 합니다.');
}
if (!(V4_REQUEST_SHARE > 0 && V4_REQUEST_SHARE < 1)) {
    throw new Error('V4_REQUEST_SHARE는 0보다 크고 1보다 작아야 합니다.');
}
if (TOTAL_MAX_VUS < TOTAL_PRE_ALLOCATED_VUS) {
    throw new Error('MAX_VUS는 PRE_ALLOCATED_VUS 이상이어야 합니다.');
}
if (!Number.isInteger(V3_MAX_PAGE) || V3_MAX_PAGE < 1 || V3_MAX_PAGE > 500) {
    throw new Error('V3_MAX_PAGE는 1~500 사이의 정수여야 합니다.');
}
if (!Number.isInteger(CURSOR_MAX_DEPTH) || CURSOR_MAX_DEPTH < 1) {
    throw new Error('CURSOR_MAX_DEPTH는 1 이상의 정수여야 합니다.');
}
if (
    !Number.isInteger(DATE_DAYS_BACK_MIN)
    || !Number.isInteger(DATE_DAYS_BACK_MAX)
    || DATE_DAYS_BACK_MIN < 0
    || DATE_DAYS_BACK_MAX < DATE_DAYS_BACK_MIN
) {
    throw new Error('DATE_DAYS_BACK_MIN/MAX 범위가 올바르지 않습니다.');
}

const V4_RATE = Math.max(1, Math.min(TOTAL_RATE - 1, Math.round(TOTAL_RATE * V4_REQUEST_SHARE)));
const V3_RATE = TOTAL_RATE - V4_RATE;

function splitPool(total, targetRate) {
    return Math.max(1, Math.round(total * targetRate / TOTAL_RATE));
}

export const options = {
    scenarios: {
        v3_offset_requests: {
            executor: 'constant-arrival-rate',
            exec: 'v3Request',
            rate: V3_RATE,
            timeUnit: '1s',
            duration: DURATION,
            preAllocatedVUs: splitPool(TOTAL_PRE_ALLOCATED_VUS, V3_RATE),
            maxVUs: splitPool(TOTAL_MAX_VUS, V3_RATE),
            gracefulStop: GRACEFUL_STOP,
            tags: { pagination: 'offset' },
        },
        v4_cursor_requests: {
            executor: 'constant-arrival-rate',
            exec: 'v4Request',
            rate: V4_RATE,
            timeUnit: '1s',
            duration: DURATION,
            preAllocatedVUs: splitPool(TOTAL_PRE_ALLOCATED_VUS, V4_RATE),
            maxVUs: splitPool(TOTAL_MAX_VUS, V4_RATE),
            gracefulStop: GRACEFUL_STOP,
            tags: { pagination: 'cursor' },
        },
    },
    thresholds: {
        post_list_realistic_failures: ['rate<0.05'],
        'post_list_realistic_duration{route:v3_offset}': ['max>=0'],
        'post_list_realistic_duration{route:v4_entry}': ['max>=0'],
        'post_list_realistic_duration{route:v4_next}': ['max>=0'],
        'post_list_realistic_requests{route:v3_offset}': ['count>=0'],
        'post_list_realistic_requests{route:v4_entry}': ['count>=0'],
        'post_list_realistic_requests{route:v4_next}': ['count>=0'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
    tags: { workload: 'v3-v4-realistic' },
};

const requestDuration = new Trend('post_list_realistic_duration', true);
const requestFailures = new Rate('post_list_realistic_failures');
const requestCount = new Counter('post_list_realistic_requests');
const selectedPage = new Trend('post_list_realistic_v3_page');
const cursorSessionDepth = new Trend('post_list_realistic_v4_session_depth');

let cursor = null;
let remainingCursorRequests = 0;
let currentCursorDepth = 0;

function randomInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

function pickDateAnchor() {
    if (DATE_ANCHORS.length > 0) {
        return DATE_ANCHORS[randomInt(0, DATE_ANCHORS.length - 1)];
    }
    const date = new Date();
    date.setUTCDate(date.getUTCDate() - randomInt(DATE_DAYS_BACK_MIN, DATE_DAYS_BACK_MAX));
    return date.toISOString().slice(0, 10);
}

function readData(response) {
    try {
        return response.json('data');
    } catch (_error) {
        return null;
    }
}

function record(response, route) {
    const data = readData(response);
    const successful = check(response, {
        [`${route} status is 200`]: (result) => result.status === 200,
        [`${route} has posts array`]: () => data != null && Array.isArray(data.posts),
    });
    requestDuration.add(response.timings.duration, { route });
    requestFailures.add(!successful, { route });
    requestCount.add(1, { route });
    return successful ? data : null;
}

function commonParams() {
    const params = [
        `boardId=${encodeURIComponent(String(BOARD_ID))}`,
        `size=${encodeURIComponent(String(SIZE))}`,
    ];
    if (CATEGORY) params.push(`category=${encodeURIComponent(CATEGORY)}`);
    return params;
}

export function v3Request() {
    const page = pickPage(V3_MAX_PAGE);
    const params = commonParams();
    params.push(`page=${encodeURIComponent(String(page))}`);
    const response = http.get(`${BASE_URL}/api/v3/posts?${params.join('&')}`, {
        tags: { name: 'post_list_v3_offset', route: 'v3_offset' },
    });
    selectedPage.add(page);
    record(response, 'v3_offset');
}

function resetCursorSession() {
    if (currentCursorDepth > 0) cursorSessionDepth.add(currentCursorDepth);
    cursor = null;
    remainingCursorRequests = 0;
    currentCursorDepth = 0;
}

function enterCursorSession() {
    const targetDepth = pickPage(CURSOR_MAX_DEPTH);
    const params = commonParams();
    params.push(`date=${encodeURIComponent(pickDateAnchor())}`);
    const response = http.get(`${BASE_URL}/api/v4/posts?${params.join('&')}`, {
        tags: { name: 'post_list_v4_entry', route: 'v4_entry' },
    });
    const data = record(response, 'v4_entry');
    currentCursorDepth = 1;
    remainingCursorRequests = targetDepth - 1;
    if (data == null || remainingCursorRequests === 0 || !data.hasNext || !data.nextCursor) {
        resetCursorSession();
        return;
    }
    cursor = data.nextCursor;
}

function moveCursor() {
    const params = commonParams();
    params.push('direction=NEXT');
    params.push(`cursorCreatedAt=${encodeURIComponent(cursor.createdAt)}`);
    params.push(`cursorPostId=${encodeURIComponent(String(cursor.postId))}`);
    const response = http.get(`${BASE_URL}/api/v4/posts?${params.join('&')}`, {
        tags: { name: 'post_list_v4_next', route: 'v4_next' },
    });
    const data = record(response, 'v4_next');
    currentCursorDepth += 1;
    remainingCursorRequests -= 1;
    if (data == null || remainingCursorRequests === 0 || !data.hasNext || !data.nextCursor) {
        resetCursorSession();
        return;
    }
    cursor = data.nextCursor;
}

export function v4Request() {
    if (cursor == null) {
        enterCursorSession();
        return;
    }
    moveCursor();
}
