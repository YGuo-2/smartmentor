const BASE_URL = '/api';

let token = localStorage.getItem('sm_token') || '';

export function setToken(t) {
  token = t;
  localStorage.setItem('sm_token', t);
}

export function clearTokens() {
  token = '';
  localStorage.removeItem('sm_token');
}

export function getAccessToken() {
  return token;
}

function buildApiUrl(path, params) {
  if (params instanceof URLSearchParams) {
    const queryString = params.toString();
    if (!queryString) return BASE_URL + path;
    return BASE_URL + path + (path.includes('?') ? '&' : '?') + queryString;
  }
  if (typeof params === 'string') {
    const queryString = params.replace(/^\?/, '');
    if (!queryString) return BASE_URL + path;
    return BASE_URL + path + (path.includes('?') ? '&' : '?') + queryString;
  }

  const query = new URLSearchParams();
  Object.entries(params || {}).forEach(([key, value]) => {
    if (value === undefined || value === null) return;
    if (Array.isArray(value)) {
      value.forEach(item => {
        if (item !== undefined && item !== null) query.append(key, String(item));
      });
      return;
    }
    query.set(key, String(value));
  });

  const queryString = query.toString();
  if (!queryString) return BASE_URL + path;
  return BASE_URL + path + (path.includes('?') ? '&' : '?') + queryString;
}

async function request(path, options = {}) {
  const url = BASE_URL + path;
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  };
  if (token) {
    headers['Authorization'] = 'Bearer ' + token;
  }
  const res = await fetch(url, { ...options, headers });

  const contentType = res.headers.get('content-type') || '';
  const rawText = await res.text();
  let data = null;

  if (rawText && (contentType.includes('application/json') || contentType.includes('+json'))) {
    try {
      data = JSON.parse(rawText);
    } catch (e) {
      throw createHttpError(res, '服务器返回了无法解析的 JSON 响应', rawText);
    }
  } else if (rawText) {
    if (res.status === 401) {
      handleUnauthorized();
    }
    throw createHttpError(res, buildNonJsonMessage(res, rawText), rawText);
  }

  if (!res.ok) {
    const statusMessage = data?.message || `请求失败（HTTP ${res.status}）`;
    if (res.status === 401) {
      handleUnauthorized();
    }
    throw createHttpError(res, statusMessage, data);
  }

  if (!data) {
    return null;
  }

  if (data.code === 401) {
    handleUnauthorized();
    throw createAppError(data, '登录已过期，请重新登录');
  }
  if (data.code !== 200) {
    throw createAppError(data, data.message || '请求失败');
  }
  return data.data;
}

export function parseSSEEvents(buffer) {
  const events = [];
  const normalized = String(buffer || '').replace(/\r\n/g, '\n');
  const parts = normalized.split('\n\n');
  const remaining = parts.pop() || '';

  for (const part of parts) {
    if (!part.trim()) continue;
    let eventType = 'message';
    const dataLines = [];

    for (const line of part.split('\n')) {
      if (line.startsWith(':')) continue;
      if (line.startsWith('event:')) {
        eventType = line.slice(6).trim() || 'message';
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).replace(/^ /, ''));
      }
    }

    const rawData = dataLines.join('\n');
    if (rawData === '') continue;

    const parsedData = parseSseData(rawData);
    if (eventType === 'message') {
      if (parsedData && typeof parsedData === 'object' && !Array.isArray(parsedData)) {
        events.push({
          type: eventType,
          data: Object.prototype.hasOwnProperty.call(parsedData, 'content')
            ? parsedData
            : { ...parsedData, content: String(parsedData.content || rawData) }
        });
      } else {
        events.push({ type: eventType, data: { content: String(parsedData) } });
      }
      continue;
    }

    if (parsedData && typeof parsedData === 'object' && !Array.isArray(parsedData)) {
      events.push({ type: eventType, data: parsedData });
    } else {
      events.push({ type: eventType, data: { message: String(parsedData), raw: rawData } });
    }
  }

  return { events, remaining };
}

export async function* streamSse(path, options = {}) {
  const headers = {
    Accept: 'text/event-stream',
    ...(options.headers || {})
  };
  if (token) {
    headers.Authorization = 'Bearer ' + token;
  }

  const res = await fetch(buildApiUrl(path, options.params), {
    method: options.method || 'GET',
    headers,
    signal: options.signal
  });

  if (!res.ok) {
    await throwHttpResponse(res);
  }
  if (!res.body) {
    throw new Error('服务器未返回可读取的流式响应');
  }

  const reader = res.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  let completed = false;

  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) {
        buffer += decoder.decode();
        break;
      }

      buffer += decoder.decode(value, { stream: true });
      const parsed = parseSSEEvents(buffer);
      buffer = parsed.remaining;
      for (const event of parsed.events) {
        yield event;
      }
    }

    if (buffer.trim()) {
      const parsed = parseSSEEvents(buffer + '\n\n');
      for (const event of parsed.events) {
        yield event;
      }
    }
    completed = true;
  } finally {
    if (!completed) {
      try { await reader.cancel(); } catch { /* stream already closed */ }
    }
  }
}

function handleUnauthorized() {
    clearTokens();
    window.location.hash = '#/login';
}

function createAppError(data, message) {
  const err = new Error(message);
  err.code = data?.code;
  err.responseData = data?.data;
  return err;
}

function createHttpError(res, message, responseData) {
  const err = new Error(message);
  err.code = res.status;
  err.responseData = responseData;
  return err;
}

function buildNonJsonMessage(res, rawText) {
  if (res.status === 401) {
    return '登录已过期，请重新登录';
  }
  if (res.status === 403) {
    return '当前账号无权访问该资源';
  }
  if (res.status >= 500) {
    return '服务器暂时不可用，请稍后重试';
  }
  const compactText = rawText.replace(/\s+/g, ' ').trim();
  return compactText ? compactText.slice(0, 120) : `请求失败（HTTP ${res.status}）`;
}

function parseSseData(rawData) {
  try {
    return JSON.parse(rawData);
  } catch {
    return rawData;
  }
}

async function throwHttpResponse(res) {
  const contentType = res.headers.get('content-type') || '';
  const rawText = await res.text().catch(() => '');
  let message = '';
  let responseData = rawText;

  if (rawText && (contentType.includes('application/json') || contentType.includes('+json'))) {
    try {
      const data = JSON.parse(rawText);
      responseData = data;
      message = data?.message || data?.error || data?.msg || '';
    } catch {
      message = '服务器返回了无法解析的 JSON 响应';
    }
  } else if (rawText) {
    message = buildNonJsonMessage(res, rawText);
  }

  if (res.status === 401) {
    handleUnauthorized();
  }

  throw createHttpError(res, message || `请求失败（HTTP ${res.status}）`, responseData);
}

/**
 * 下载受保护的二进制文件（带 Authorization），触发浏览器另存为。
 */
export async function downloadFile(path, fallbackName) {
  const res = await fetch(BASE_URL + path, {
    headers: token ? { Authorization: 'Bearer ' + token } : {}
  })
  if (res.status === 401) {
    handleUnauthorized()
    throw new Error('登录已过期，请重新登录')
  }
  if (!res.ok) {
    throw new Error('文件下载失败（HTTP ' + res.status + '）')
  }
  // 优先用响应头里的文件名（filename*=UTF-8''xxx）
  let filename = fallbackName || 'download'
  const disposition = res.headers.get('content-disposition') || ''
  const star = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (star) {
    try { filename = decodeURIComponent(star[1]) } catch { /* 用兜底名 */ }
  }
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}

export const api = {
  auth: {
    register: (body) => request('/auth/register', { method: 'POST', body: JSON.stringify(body) }),
    login: (body) => request('/auth/login', { method: 'POST', body: JSON.stringify(body) }),
    sendCaptcha: (body) => request('/auth/captcha/email', { method: 'POST', body: JSON.stringify(body) }),
    me: () => request('/auth/me')
  },
  profile: {
    overview: () => request('/profile/overview'),
    knowledgeMap: (params) => request('/profile/knowledge-map?' + new URLSearchParams(params || {})),
    updateSettings: (body) => request('/profile/settings', { method: 'PUT', body: JSON.stringify(body) }),
    buildNeeded: () => request('/profile/build/needed'),
    buildExtract: (body) => request('/profile/build/extract', { method: 'POST', body: JSON.stringify(body) })
  },
  diagnostic: {
    start: (body) => request('/diagnostic/start', { method: 'POST', body: JSON.stringify(body) }),
    submit: (body) => request('/diagnostic/submit', { method: 'POST', body: JSON.stringify(body) }),
    finish: (body) => request('/diagnostic/finish', { method: 'POST', body: JSON.stringify(body) }),
    history: (params) => request('/diagnostic/history?' + new URLSearchParams(params)),
    detail: (diagnosticId) => request('/diagnostic/result/' + diagnosticId)
  },
  tracing: {
    analyze: (body) => request('/tracing/analyze', { method: 'POST', body: JSON.stringify(body) }),
    detail: (tracingId) => request('/tracing/result/' + tracingId)
  },
  learning: {
    generate: (body) => request('/learning/path/generate', { method: 'POST', body: JSON.stringify(body) }),
    paths: (params) => request('/learning/path?' + new URLSearchParams(params || {})),
    pathDetail: (pathId) => request('/learning/path/' + pathId),
    lesson: (pathId, nodeId) => request('/learning/lesson/' + pathId + '/' + nodeId),
    lessonVideo: (pathId, nodeId) => request('/learning/lesson/' + pathId + '/' + nodeId + '/video'),
    lessonResourceDetails: (pathId, nodeId) => request('/learning/lesson/' + pathId + '/' + nodeId + '/resource-details'),
    lessonAnimation: (pathId, nodeId) => request('/learning/lesson/' + pathId + '/' + nodeId + '/animation'),
    lessonSlides: (pathId, nodeId) => request('/learning/lesson/' + pathId + '/' + nodeId + '/slides'),
    submitExercise: (body) => request('/learning/exercise/submit', { method: 'POST', body: JSON.stringify(body) }),
    submitCheckpoint: (body) => request('/learning/checkpoint/submit', { method: 'POST', body: JSON.stringify(body) })
  },
  chat: {
    history: (params) => request('/chat/history?' + new URLSearchParams(params))
  },
  report: {
    effectiveness: (params) => request('/report/effectiveness?' + new URLSearchParams(params || {})),
    dashboard: () => request('/report/dashboard')
  },
  engagement: {
    missions: (date) => request('/engagement/missions' + (date ? '?date=' + date : '')),
    completeMission: (missionId, body) => request('/engagement/missions/' + missionId + '/complete', { method: 'POST', body: JSON.stringify(body || {}) })
  }
};
