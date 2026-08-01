import { readFileSync } from 'node:fs';
import { createServer } from 'node:http';

const host = process.env.STUB_HOST || '127.0.0.1';
const port = Number(process.env.STUB_PORT || 19090);
const delayMs = Math.max(0, Number(process.env.STUB_DELAY_MS || 300));
const fixture = readFileSync(new URL('./fixture.json', import.meta.url), 'utf8');
let searchCalls = 0;

const server = createServer((request, response) => {
  const url = new URL(request.url, `http://${request.headers.host}`);
  if (url.pathname === '/_metrics') {
    response.writeHead(200, { 'content-type': 'application/json' });
    response.end(JSON.stringify({ searchCalls, delayMs }));
    return;
  }
  if (url.pathname === '/_reset' && request.method === 'POST') {
    searchCalls = 0;
    response.writeHead(204);
    response.end();
    return;
  }
  if (url.pathname !== '/v1/search/local.json') {
    response.writeHead(404);
    response.end();
    return;
  }
  searchCalls += 1;
  setTimeout(() => {
    response.writeHead(200, { 'content-type': 'application/json; charset=utf-8' });
    response.end(fixture);
  }, delayMs);
});

server.listen(port, host, () => {
  process.stdout.write(`local-map provider stub: http://${host}:${port}, delay=${delayMs}ms\n`);
});

for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () => server.close(() => process.exit(0)));
}
