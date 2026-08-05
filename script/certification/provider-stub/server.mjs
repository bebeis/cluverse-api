import { readFileSync } from 'node:fs';
import { createServer } from 'node:http';

const host = process.env.STUB_HOST || '127.0.0.1';
const port = positiveInteger(process.env.STUB_PORT || '19091', 'STUB_PORT');
const fixtureTemplate = readFileSync(new URL('./fixture.json', import.meta.url), 'utf8');

const defaults = {
  delayMs: nonNegativeInteger(process.env.STUB_DELAY_MS || '100', 'STUB_DELAY_MS'),
  status: 200,
  responseMode: 'valid',
};
const state = { ...defaults, calls: 0, callsByYear: {} };

function positiveInteger(value, name) {
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed <= 0) throw new Error(`${name} must be a positive integer`);
  return parsed;
}

function nonNegativeInteger(value, name) {
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed < 0) throw new Error(`${name} must be a non-negative integer`);
  return parsed;
}

function json(response, status, body) {
  response.writeHead(status, { 'content-type': 'application/json; charset=utf-8' });
  response.end(JSON.stringify(body));
}

async function readJson(request) {
  let raw = '';
  for await (const chunk of request) {
    raw += chunk;
    if (raw.length > 16_384) throw new Error('request body too large');
  }
  return raw ? JSON.parse(raw) : {};
}

function metrics() {
  return {
    calls: state.calls,
    callsByYear: state.callsByYear,
    delayMs: state.delayMs,
    status: state.status,
    responseMode: state.responseMode,
  };
}

function providerBody(year) {
  if (state.responseMode === 'empty') {
    return {
      response: {
        header: { resultCode: '00', resultMsg: 'NORMAL SERVICE' },
        body: { items: { item: [] } },
      },
    };
  }
  if (state.responseMode === 'provider-error') {
    return {
      response: {
        header: { resultCode: '99', resultMsg: 'STUB PROVIDER ERROR' },
        body: { items: { item: [] } },
      },
    };
  }
  return JSON.parse(fixtureTemplate.replaceAll('{{YEAR}}', year));
}

const server = createServer(async (request, response) => {
  const url = new URL(request.url, `http://${request.headers.host}`);

  try {
    if (url.pathname === '/_metrics' && request.method === 'GET') {
      json(response, 200, metrics());
      return;
    }
    if (url.pathname === '/_reset' && request.method === 'POST') {
      state.calls = 0;
      state.callsByYear = {};
      json(response, 200, metrics());
      return;
    }
    if (url.pathname === '/_control' && request.method === 'POST') {
      const command = await readJson(request);
      const responseMode = command.responseMode ?? defaults.responseMode;
      if (!['valid', 'empty', 'provider-error', 'malformed'].includes(responseMode)) {
        throw new Error('responseMode must be valid, empty, provider-error, or malformed');
      }
      state.delayMs = nonNegativeInteger(String(command.delayMs ?? defaults.delayMs), 'delayMs');
      state.status = positiveInteger(String(command.status ?? defaults.status), 'status');
      if (state.status > 599) throw new Error('status must be between 1 and 599');
      state.responseMode = responseMode;
      json(response, 200, metrics());
      return;
    }
    if (url.pathname !== '/B490007/qualExamSchd/getQualExamSchdList' || request.method !== 'GET') {
      json(response, 404, { message: 'not found' });
      return;
    }

    const year = url.searchParams.get('implYy') || 'unknown';
    state.calls += 1;
    state.callsByYear[year] = (state.callsByYear[year] || 0) + 1;
    setTimeout(() => {
      if (state.responseMode === 'malformed' && state.status === 200) {
        response.writeHead(200, { 'content-type': 'application/json; charset=utf-8' });
        response.end('{"response":');
        return;
      }
      json(response, state.status, providerBody(year));
    }, state.delayMs);
  } catch (error) {
    json(response, 400, { message: error.message });
  }
});

server.listen(port, host, () => {
  process.stdout.write(`certification provider stub: http://${host}:${port}, delay=${defaults.delayMs}ms\n`);
});

for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () => server.close(() => process.exit(0)));
}
