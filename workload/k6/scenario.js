import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const READ_RATIO = parseInt(__ENV.READ_RATIO || '80');
const SEED = parseInt(__ENV.SEED || '42');
const DURATION = __ENV.DURATION || '60s';
const VUS = parseInt(__ENV.VUS || '10');

const errorRate = new Rate('errors');
const readLatency = new Trend('read_latency', true);
const writeLatency = new Trend('write_latency', true);

export const options = {
  scenarios: {
    steady_load: {
      executor: 'constant-vus',
      vus: VUS,
      duration: DURATION,
    },
  },
  thresholds: {
    errors: ['rate<0.05'],
  },
};

const NUM_CLIENTES = 20;
const NUM_PRODUCTOS = 30;

let rngState = SEED;
function nextRng() {
  rngState = (rngState * 1103515245 + 12345) & 0x7fffffff;
  return rngState;
}

function randomInt(min, max) {
  return min + (nextRng() % (max - min + 1));
}

function doRead() {
  const op = randomInt(1, 100);
  let res;

  if (op <= 40) {
    const id = randomInt(1, NUM_CLIENTES);
    res = http.get(`${BASE_URL}/api/clientes/${id}`);
  } else if (op <= 80) {
    const id = randomInt(1, NUM_PRODUCTOS);
    res = http.get(`${BASE_URL}/api/productos/${id}`);
  } else {
    res = http.get(`${BASE_URL}/api/pedidos`);
  }

  check(res, { 'read status 2xx': (r) => r.status >= 200 && r.status < 300 });
  errorRate.add(res.status >= 400);
  readLatency.add(res.timings.duration);
}

function doWrite() {
  const op = randomInt(1, 100);
  let res;

  if (op <= 50) {
    const payload = JSON.stringify({
      clienteId: randomInt(1, NUM_CLIENTES),
      items: [
        { productoId: randomInt(1, 10), cantidad: 1 },
        { productoId: randomInt(11, 20), cantidad: 1 },
      ],
    });
    res = http.post(`${BASE_URL}/api/pedidos`, payload, {
      headers: { 'Content-Type': 'application/json' },
    });
  } else if (op <= 75) {
    const payload = JSON.stringify({
      nombre: `Cliente k6 ${randomInt(1, 99999)}`,
      email: `k6_${randomInt(1, 99999)}@test.com`,
      direccion: `Calle Test ${randomInt(1, 999)}`,
    });
    res = http.post(`${BASE_URL}/api/clientes`, payload, {
      headers: { 'Content-Type': 'application/json' },
    });
  } else {
    const payload = JSON.stringify({
      nombre: `Producto k6 ${randomInt(1, 99999)}`,
      descripcion: 'Producto de prueba k6',
      precio: randomInt(1000, 500000),
      stock: randomInt(10, 1000),
    });
    res = http.post(`${BASE_URL}/api/productos`, payload, {
      headers: { 'Content-Type': 'application/json' },
    });
  }

  check(res, { 'write status 2xx': (r) => r.status >= 200 && r.status < 300 });
  errorRate.add(res.status >= 400);
  writeLatency.add(res.timings.duration);
}

export default function () {
  rngState = SEED + __VU * 10000 + __ITER;

  const roll = randomInt(1, 100);
  if (roll <= READ_RATIO) {
    doRead();
  } else {
    doWrite();
  }

  sleep(0.1);
}
