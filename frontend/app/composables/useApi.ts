import type { Problem } from '~/types/api'

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
interface ApiOptions {
  method?: HttpMethod
  body?: unknown
  headers?: HeadersInit
  query?: Record<string, string | number | boolean | undefined>
}

export class ApiError extends Error {
  constructor(public problem: Problem, public status: number) {
    super(problem.detail || problem.title || '请求失败')
  }
}

export function useApi() {
  const config = useRuntimeConfig()
  const baseURL = import.meta.server ? config.apiInternalBase : config.public.apiBase
  const requestHeaders = import.meta.server ? useRequestHeaders(['cookie']) : {}
  let csrfToken: string | undefined

  async function csrf() {
    if (csrfToken) return csrfToken
    const result = await $fetch<{ token: string }>('/auth/csrf', {
      baseURL,
      credentials: 'include',
      headers: requestHeaders,
    })
    csrfToken = result.token
    return csrfToken
  }

  async function request<T = unknown>(path: string, options: ApiOptions = {}): Promise<T> {
    const method = String(options.method || 'GET').toUpperCase()
    const headers = new Headers(options.headers as HeadersInit | undefined)
    Object.entries(requestHeaders).forEach(([key, value]) => value && headers.set(key, value))
    if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
      headers.set('X-XSRF-TOKEN', await csrf())
    }
    try {
      const fetcher = $fetch as unknown as (url: string, fetchOptions: Record<string, unknown>) => Promise<T>
      return await fetcher(path, {
        ...options,
        baseURL,
        headers,
        credentials: 'include',
      })
    }
    catch (error: unknown) {
      const failure = error as { response?: { status?: number }, data?: Problem }
      const status = failure.response?.status || 500
      const problem = failure.data || {}
      throw new ApiError(problem, status)
    }
  }

  return { request }
}
