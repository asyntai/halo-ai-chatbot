// Reads and writes this plugin's ConfigMap through the Halo core API.
//
// data.basic       JSON {enabled, widgetId}: the settings form group
// data.connection  JSON {accountEmail, scriptUrl}: saved by the Connect tab

import { axiosInstance } from '@halo-dev/api-client'

export const CONFIG_MAP_NAME = 'asyntai-ai-chatbot-configmap'
const URL = `/api/v1alpha1/configmaps/${CONFIG_MAP_NAME}`

export const WIDGET_ID_PATTERN = /^asyntai_[A-Za-z0-9]+$/

export interface Basic {
  enabled?: boolean
  widgetId?: string
  [key: string]: unknown
}

export interface Connection {
  accountEmail?: string
  scriptUrl?: string
  [key: string]: unknown
}

interface ConfigMap {
  apiVersion: string
  kind: string
  metadata: { name: string; version?: number; [key: string]: unknown }
  data?: Record<string, string>
}

export function parseGroup<T extends object>(raw: string | undefined): T {
  if (!raw) return {} as T
  try {
    const value = JSON.parse(raw)
    return value && typeof value === 'object' && !Array.isArray(value) ? (value as T) : ({} as T)
  } catch {
    return {} as T
  }
}

/** Only an https script on asyntai.com or a subdomain is kept. */
export function trustedScriptUrl(url: unknown): string {
  if (typeof url !== 'string' || !url) return ''
  try {
    const u = new window.URL(url)
    const host = u.hostname.toLowerCase()
    if (u.protocol !== 'https:' || u.username || u.password) return ''
    return host === 'asyntai.com' || host.endsWith('.asyntai.com') ? url : ''
  } catch {
    return ''
  }
}

export async function load(): Promise<{ basic: Basic; connection: Connection }> {
  const map = await fetchMap()
  return {
    basic: parseGroup<Basic>(map?.data?.basic),
    connection: parseGroup<Connection>(map?.data?.connection),
  }
}

/**
 * Applies a change to the two groups and saves the ConfigMap. The other keys
 * in the map stay as they are. A map that does not exist yet is created.
 */
export async function save(change: { basic?: Partial<Basic>; connection?: Connection }) {
  const map = await fetchMap()
  const data = { ...(map?.data || {}) }
  if (change.basic) {
    data.basic = JSON.stringify({ ...parseGroup<Basic>(data.basic), ...change.basic })
  }
  if (change.connection) {
    data.connection = JSON.stringify(change.connection)
  }
  if (map) {
    await axiosInstance.put(URL, { ...map, data })
  } else {
    await axiosInstance.post('/api/v1alpha1/configmaps', {
      apiVersion: 'v1alpha1',
      kind: 'ConfigMap',
      metadata: { name: CONFIG_MAP_NAME },
      data,
    })
  }
}

async function fetchMap(): Promise<ConfigMap | null> {
  try {
    const { data } = await axiosInstance.get<ConfigMap>(URL)
    return data
  } catch (e: unknown) {
    const status = (e as { response?: { status?: number } })?.response?.status
    if (status === 404) return null
    throw e
  }
}
