// Ponte entre o painel e a API Laravel. O token fica num cookie HttpOnly
// (o navegador nunca o lê); o servidor do painel o envia como Bearer à API.
// Usado pelas rotas do Next (app/api/**) e pelo servidor local (scripts/local-server.mjs).

const COOKIE = 'gym_token';
const MAX_AGE = 8 * 60 * 60;

export type PanelUser = {username: string; displayName: string; role: 'global_admin' | 'unit_admin'; unit: string | null};

type ApiUser = {
  name: string;
  username: string | null;
  email: string;
  is_global_admin: boolean;
  gyms: {name: string; roles: string[]}[];
};

function apiUrl(): string {
  const env = (globalThis as {process?: {env?: Record<string, string | undefined>}}).process?.env;
  return (env?.LARAVEL_API_URL || 'http://academia-api.test/api').replace(/\/$/, '');
}

function json(status: number, data: unknown, cookie?: string): Response {
  const headers = new Headers({'content-type': 'application/json', 'cache-control': 'no-store'});
  if (cookie) headers.set('set-cookie', cookie);
  return new Response(JSON.stringify(data), {status, headers});
}

function tokenFrom(req: Request): string | null {
  const pair = (req.headers.get('cookie') || '').split(';').map(s => s.trim()).find(s => s.startsWith(COOKIE + '='));
  return pair ? decodeURIComponent(pair.slice(COOKIE.length + 1)) : null;
}

function cookie(req: Request, value: string, maxAge: number): string {
  const secure = new URL(req.url).protocol === 'https:' ? '; Secure' : '';
  return `${COOKIE}=${encodeURIComponent(value)}; HttpOnly; SameSite=Strict; Path=/; Max-Age=${maxAge}${secure}`;
}

function sameOrigin(req: Request): boolean {
  const origin = req.headers.get('origin');
  return !origin || origin === new URL(req.url).origin;
}

async function callApi(path: string, init: RequestInit & {token?: string | null} = {}): Promise<{status: number; body: Record<string, unknown>}> {
  const headers = new Headers({accept: 'application/json', 'content-type': 'application/json'});
  if (init.token) headers.set('authorization', `Bearer ${init.token}`);
  let res: Response;
  try {
    res = await fetch(apiUrl() + path, {...init, headers});
  } catch {
    return {status: 503, body: {message: 'Não foi possível conectar à API. Verifique se ela está no ar.'}};
  }
  const body = await res.json().catch(() => ({})) as Record<string, unknown>;
  return {status: res.status, body};
}

/** Converte o erro do Laravel ({message, errors}) no formato do painel ({error}). */
function apiError(status: number, body: Record<string, unknown>, fallback: string): Response {
  const errors = body.errors as Record<string, string[]> | undefined;
  const message = (errors && Object.values(errors)[0]?.[0]) || (typeof body.message === 'string' && body.message) || fallback;
  return json(status >= 500 && status !== 503 ? 502 : status, {error: message});
}

export function toPanelUser(user: ApiUser): PanelUser | null {
  if (user.is_global_admin) return {username: user.username || user.email, displayName: user.name, role: 'global_admin', unit: null};
  const gym = user.gyms.find(g => g.roles.includes('admin'));
  return gym ? {username: user.username || user.email, displayName: user.name, role: 'unit_admin', unit: gym.name} : null;
}

export async function handleLogin(req: Request): Promise<Response> {
  if (req.method !== 'POST') return json(405, {error: 'Método não permitido.'});
  if (!sameOrigin(req)) return json(403, {error: 'Origem inválida.'});
  const input = await req.json().catch(() => null) as {username?: unknown; password?: unknown} | null;
  if (typeof input?.username !== 'string' || typeof input.password !== 'string' || !input.username.trim()) {
    return json(400, {error: 'Informe usuário e senha.'});
  }

  const {status, body} = await callApi('/login', {
    method: 'POST',
    body: JSON.stringify({login: input.username.trim(), password: input.password, device_name: 'painel-web'}),
  });
  if (status === 429) return json(429, {error: 'Muitas tentativas. Aguarde um minuto e tente novamente.'});
  if (status !== 200) return apiError(status === 422 ? 401 : status, body, 'Usuário ou senha incorretos.');

  const token = body.token as string;
  if (!toPanelUser(body.user as ApiUser)) {
    await callApi('/logout', {method: 'POST', token});
    return json(403, {error: 'Acesso exclusivo para administradores.'});
  }
  return json(200, {ok: true}, cookie(req, token, MAX_AGE));
}

export async function handleMe(req: Request): Promise<Response> {
  const token = tokenFrom(req);
  if (!token) return json(401, {error: 'Entre na sua conta.'});
  const {status, body} = await callApi('/me', {token});
  if (status !== 200) return apiError(status, body, 'Entre na sua conta.');
  const user = toPanelUser(body as unknown as ApiUser);
  return user ? json(200, user) : json(403, {error: 'Acesso exclusivo para administradores.'});
}

export async function handleLogout(req: Request): Promise<Response> {
  if (req.method !== 'POST') return json(405, {error: 'Método não permitido.'});
  if (!sameOrigin(req)) return json(403, {error: 'Origem inválida.'});
  const token = tokenFrom(req);
  if (token) await callApi('/logout', {method: 'POST', token});
  return json(200, {ok: true}, cookie(req, '', 0));
}

/** GET → dados do painel; POST {kind, record} → salva um registro. */
export async function handleGym(req: Request): Promise<Response> {
  const token = tokenFrom(req);
  if (!token) return json(401, {error: 'Entre na sua conta para continuar.'});

  if (req.method === 'GET') {
    const {status, body} = await callApi('/admin/data', {token});
    return status === 200 ? json(200, body) : apiError(status, body, 'Não foi possível carregar os dados. Tente novamente.');
  }
  if (req.method === 'POST') {
    if (!sameOrigin(req)) return json(403, {error: 'Origem inválida.'});
    const raw = await req.text();
    if (raw.length > 200_000) return json(413, {error: 'Dados grandes demais.'});
    const {status, body} = await callApi('/admin/records', {method: 'POST', token, body: raw});
    return status === 200 ? json(200, body) : apiError(status, body, 'Não foi possível salvar. Seus dados continuam no formulário.');
  }
  return json(405, {error: 'Método não permitido.'});
}
