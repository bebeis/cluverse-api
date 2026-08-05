import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

import {
  apiResponseOk,
  authenticatedParams,
  envInteger,
  envNumber,
  pickPost,
  randomItem,
  readBaseUrl,
  readPositiveIds,
  seconds,
} from './support.js';

const PROFILE = (__ENV.PROFILE || 'spike').toLowerCase();
if (!['capacity', 'spike', 'smoke'].includes(PROFILE)) {
  throw new Error('PROFILE은 capacity, spike, smoke 중 하나여야 합니다.');
}

const BASE_URL = readBaseUrl();
const RUN_ID = __ENV.RUN_ID || 'manual';
const MEMBER_IDS = readPositiveIds('MEMBER_IDS', '1');
const BOARD_IDS = readPositiveIds('BOARD_IDS', '1');
const POST_IDS = readPositiveIds('POST_IDS', '1');
const HOT_POST_COUNT = envInteger('HOT_POST_COUNT', 10, 1);
const HOT_POST_SHARE = envNumber('HOT_POST_SHARE', 0.8, 0);
if (HOT_POST_SHARE > 1) throw new Error('HOT_POST_SHARE는 0 이상 1 이하여야 합니다.');

const FLOW_DEFINITIONS = [
  { name: 'home_recent', kind: 'read', weight: envNumber('WEIGHT_HOME_RECENT', 15, 0) },
  { name: 'popular_posts', kind: 'read', weight: envNumber('WEIGHT_POPULAR_POSTS', 10, 0) },
  { name: 'post_list', kind: 'read', weight: envNumber('WEIGHT_POST_LIST', 25, 0) },
  { name: 'post_detail', kind: 'read', weight: envNumber('WEIGHT_POST_DETAIL', 20, 0) },
  { name: 'comment_list', kind: 'read', weight: envNumber('WEIGHT_COMMENT_LIST', 15, 0) },
  { name: 'view_count', kind: 'write', weight: envNumber('WEIGHT_VIEW_COUNT', 10, 0) },
  { name: 'comment_write', kind: 'write', weight: envNumber('WEIGHT_COMMENT_WRITE', 5, 0) },
].filter((flow) => flow.weight > 0);

const TOTAL_WEIGHT = FLOW_DEFINITIONS.reduce((sum, flow) => sum + flow.weight, 0);
if (TOTAL_WEIGHT <= 0) throw new Error('적어도 하나의 요청 가중치는 0보다 커야 합니다.');
const MUTATES_DATA = FLOW_DEFINITIONS.some((flow) => flow.kind === 'write' || flow.name === 'post_detail');
if (MUTATES_DATA && __ENV.ALLOW_DATA_MUTATION !== '1') {
  throw new Error('쓰기 또는 조회수 증가가 포함된 부하에는 ALLOW_DATA_MUTATION=1이 필요합니다. test fixture에서만 실행하세요.');
}

const READ_P95_MS = envNumber('READ_P95_MS', 300, 1);
const READ_P99_MS = envNumber('READ_P99_MS', 800, 1);
const WRITE_P95_MS = envNumber('WRITE_P95_MS', 500, 1);
const WRITE_P99_MS = envNumber('WRITE_P99_MS', 1500, 1);
const SUCCESS_RATE = envNumber('SUCCESS_RATE', 0.999, 0);

function capacityScenarios() {
  const rates = (__ENV.CAPACITY_RATES || '25,50,100,150,200')
    .split(',')
    .map((value) => Number(value.trim()));
  if (rates.length === 0 || rates.some((rate) => !Number.isInteger(rate) || rate <= 0)) {
    throw new Error('CAPACITY_RATES에는 쉼표로 구분한 양의 정수가 필요합니다.');
  }
  const stepSeconds = envInteger('STEP_DURATION_SECONDS', 600, 1);
  const maxRate = Math.max(...rates);
  const stages = [{ target: rates[0], duration: seconds(stepSeconds) }];
  rates.slice(1).forEach((rate) => {
    stages.push({ target: rate, duration: '1s' });
    if (stepSeconds > 1) stages.push({ target: rate, duration: seconds(stepSeconds - 1) });
  });
  return {
    capacity: {
      executor: 'ramping-arrival-rate',
      exec: 'runMixedFlow',
      startRate: rates[0],
      timeUnit: '1s',
      stages,
      preAllocatedVUs: envInteger('PRE_ALLOCATED_VUS', Math.max(maxRate, 100), 1),
      maxVUs: envInteger('MAX_VUS', Math.max(maxRate * 4, 400), 1),
      gracefulStop: '0s',
      tags: { phase: 'capacity' },
    },
  };
}

function spikeScenarios() {
  const normalRate = envInteger('NORMAL_RATE', 50, 1);
  const spikeRate = envInteger('SPIKE_RATE', normalRate * 5, normalRate);
  const baselineSeconds = envInteger('BASELINE_SECONDS', 120, 1);
  const rampSeconds = envInteger('RAMP_SECONDS', 10, 1);
  const spikeSeconds = envInteger('SPIKE_SECONDS', 120, 1);
  const recoverySeconds = envInteger('RECOVERY_SECONDS', 120, 1);
  return {
    spike: {
      executor: 'ramping-arrival-rate',
      exec: 'runMixedFlow',
      startRate: normalRate,
      timeUnit: '1s',
      preAllocatedVUs: envInteger('PRE_ALLOCATED_VUS', Math.max(spikeRate, 100), 1),
      maxVUs: envInteger('MAX_VUS', Math.max(spikeRate * 4, 400), 1),
      gracefulStop: '0s',
      stages: [
        { target: normalRate, duration: seconds(baselineSeconds) },
        { target: spikeRate, duration: seconds(rampSeconds) },
        { target: spikeRate, duration: seconds(spikeSeconds) },
        { target: normalRate, duration: '1s' },
        { target: normalRate, duration: seconds(recoverySeconds) },
      ],
      tags: { phase: 'spike' },
    },
  };
}

function smokeScenarios() {
  return {
    smoke: {
      executor: 'constant-arrival-rate',
      exec: 'runMixedFlow',
      rate: envInteger('SMOKE_RATE', 1, 1),
      timeUnit: '1s',
      duration: seconds(envInteger('SMOKE_SECONDS', 10, 1)),
      preAllocatedVUs: 2,
      maxVUs: 10,
      gracefulStop: '0s',
      tags: { phase: 'smoke' },
    },
  };
}

export const options = {
  scenarios: PROFILE === 'capacity' ? capacityScenarios() : PROFILE === 'spike' ? spikeScenarios() : smokeScenarios(),
  thresholds: {
    dropped_iterations: ['count==0'],
    core_request_success: [`rate>=${SUCCESS_RATE}`],
    core_read_duration: [`p(95)<${READ_P95_MS}`, `p(99)<${READ_P99_MS}`],
    core_write_duration: [`p(95)<${WRITE_P95_MS}`, `p(99)<${WRITE_P99_MS}`],
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  tags: { experiment: 'spike-traffic', profile: PROFILE, run_id: RUN_ID },
};

const coreReadDuration = new Trend('core_read_duration', true);
const coreWriteDuration = new Trend('core_write_duration', true);
const coreRequestSuccess = new Rate('core_request_success');
const flowRequests = new Counter('flow_requests');
const flowFailures = new Counter('flow_failures');

const flowDurations = {
  home_recent: new Trend('home_recent_duration', true),
  popular_posts: new Trend('popular_posts_duration', true),
  post_list: new Trend('post_list_duration', true),
  post_detail: new Trend('post_detail_duration', true),
  comment_list: new Trend('comment_list_duration', true),
  view_count: new Trend('view_count_write_duration', true),
  comment_write: new Trend('comment_write_duration', true),
};

function pickFlow() {
  let cursor = Math.random() * TOTAL_WEIGHT;
  for (const flow of FLOW_DEFINITIONS) {
    if (cursor < flow.weight) return flow;
    cursor -= flow.weight;
  }
  return FLOW_DEFINITIONS[FLOW_DEFINITIONS.length - 1];
}

function requestFor(flow, memberId, boardId, postId) {
  const tags = { name: flow.name, flow: flow.name, kind: flow.kind };
  const params = authenticatedParams(memberId, tags, flow.name === 'comment_write');
  switch (flow.name) {
    case 'home_recent':
      return http.get(`${BASE_URL}/api/v3/home/recent-commented-posts`, params);
    case 'popular_posts':
      return http.get(`${BASE_URL}/api/v2/popular-posts/recent?size=10`, params);
    case 'post_list':
      return http.get(`${BASE_URL}/api/v4/posts?boardId=${boardId}&size=20`, params);
    case 'post_detail':
      return http.get(`${BASE_URL}/api/v1/posts/${postId}`, params);
    case 'comment_list':
      return http.get(`${BASE_URL}/api/v2/comments?postId=${postId}&limit=20`, params);
    case 'view_count':
      return http.post(`${BASE_URL}/api/v4/posts/${postId}/view-count`, null, params);
    case 'comment_write': {
      const content = `[loadtest:${RUN_ID}] vu=${exec.vu.idInTest} iteration=${exec.scenario.iterationInTest}`;
      return http.post(
        `${BASE_URL}/api/v1/comments?postId=${postId}`,
        JSON.stringify({ parentCommentId: null, content, isAnonymous: false }),
        params,
      );
    }
    default:
      throw new Error(`지원하지 않는 flow입니다: ${flow.name}`);
  }
}

export function runMixedFlow() {
  const flow = pickFlow();
  const memberId = randomItem(MEMBER_IDS);
  const boardId = randomItem(BOARD_IDS);
  const postId = pickPost(POST_IDS, HOT_POST_COUNT, HOT_POST_SHARE);
  const response = requestFor(flow, memberId, boardId, postId);
  const expectedStatus = flow.name === 'comment_write' ? 201 : 200;
  const ok = check(response, {
    [`${flow.name} ApiResponse success`]: (value) => apiResponseOk(value, expectedStatus),
  });

  const tags = { flow: flow.name, kind: flow.kind };
  flowRequests.add(1, tags);
  if (!ok) flowFailures.add(1, tags);
  flowDurations[flow.name].add(response.timings.duration);
  coreRequestSuccess.add(ok, tags);
  if (flow.kind === 'read') coreReadDuration.add(response.timings.duration, tags);
  else coreWriteDuration.add(response.timings.duration, tags);
}
