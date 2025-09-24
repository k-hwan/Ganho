import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 20 },
    { duration: '2m',  target: 100 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

const BASE = 'http://host.docker.internal:8080';

export function setup() {
  // 필요 시 로그인: 성공하면 토큰 반환, 실패하면 null
  try {
    const res = http.post(`${BASE}/api/auth/login`, JSON.stringify({
      loginId: 'test',
      password: 'test'
    }), { headers: { 'Content-Type': 'application/json' } });

    const body = res.json();
    const token = body?.data?.accessToken || body?.accessToken || null;
    return { token };
  } catch (e) {
    return { token: null };
  }
}

export default function (data) {
  const headers = data.token ? { Authorization: `Bearer ${data.token}` } : {};

  // 1) 헬스체크(무인증)
  const h = http.get(`${BASE}/actuator/health`);
  check(h, { 'health 200': (r) => r.status === 200 });

  // 2) 스웨거 문서(무인증)
  const s = http.get(`${BASE}/v3/api-docs`);
  check(s, { 'docs 200': (r) => r.status === 200 });

  // 3) 메인 API 예시(인증 필요 시 토큰 포함) — 실제 엔드포인트로 교체하세요
  const api = http.get(`${BASE}/api/notifications/button-patterns`, { headers });
  check(api, { 'api 2xx': (r) => r.status >= 200 && r.status < 300 });

  sleep(1);
}


