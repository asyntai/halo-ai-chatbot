<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { VButton } from '@halo-dev/components'
import { messages } from '../i18n'
import { load, save, trustedScriptUrl, WIDGET_ID_PATTERN } from '../config'

const t = messages()

// asyntai.com, or a local test server set in localStorage for development.
function asyntaiOrigin(): string {
  let value = ''
  try {
    value = window.localStorage.getItem('asyntai.origin') || ''
  } catch {
    value = ''
  }
  if (/^http:\/\/(127\.0\.0\.1|localhost)(:\d+)?$/.test(value)) return value
  return 'https://asyntai.com'
}
const origin = asyntaiOrigin()

const loading = ref(true)
const busy = ref(false)
const widgetId = ref('')
const accountEmail = ref('')
const enabled = ref(true)
const manualId = ref('')
const alert = ref<{ text: string; ok: boolean } | null>(null)

const connected = computed(() => WIDGET_ID_PATTERN.test(widgetId.value))
const dashboardUrl = `${origin}/dashboard`
const docsUrl = 'https://asyntai.com/documentation/integrations/halo/'

let state = newState()
let pollToken = 0

function newState(): string {
  const bytes = new Uint8Array(8)
  window.crypto.getRandomValues(bytes)
  return 'halo_' + Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('')
}

const stateRef = ref(state)
// The public site address set in Halo. Asyntai reads this site's pages to learn
// the answers. The console address is the fallback.
const siteUrl = ref(window.location.origin)
const authUrl = computed(
  () =>
    `${origin}/wp-auth?platform=halo&state=${encodeURIComponent(stateRef.value)}` +
    `&site_url=${encodeURIComponent(siteUrl.value)}`,
)

async function loadSiteUrl() {
  try {
    const res = await fetch('/actuator/globalinfo', { credentials: 'same-origin' })
    const info = (await res.json()) as { externalUrl?: string }
    if (info.externalUrl && /^https?:\/\//i.test(info.externalUrl)) {
      siteUrl.value = info.externalUrl.replace(/\/+$/, '')
    }
  } catch {
    // Keep the console address.
  }
}

function show(text: string, ok: boolean) {
  alert.value = { text, ok }
}

async function refresh() {
  const { basic, connection } = await load()
  widgetId.value = typeof basic.widgetId === 'string' ? basic.widgetId.trim() : ''
  enabled.value = basic.enabled !== false && String(basic.enabled) !== 'false'
  accountEmail.value = typeof connection.accountEmail === 'string' ? connection.accountEmail : ''
}

async function store(id: string, email: string, scriptUrl: string) {
  busy.value = true
  try {
    await save({
      basic: { widgetId: id, enabled: true },
      connection: { accountEmail: email, scriptUrl: trustedScriptUrl(scriptUrl) },
    })
    await refresh()
    show(t.saved, true)
  } catch (e) {
    show(t.saveFailed + String((e as Error)?.message || e), false)
  } finally {
    busy.value = false
  }
}

function poll() {
  const token = ++pollToken
  const current = stateRef.value
  let attempts = 0

  const check = () => {
    if (token !== pollToken) return
    if (attempts++ > 600) {
      show(t.timedOut, false)
      return
    }
    const callback = `asyntaiHaloCb${Date.now()}${Math.floor(Math.random() * 1000)}`
    const script = document.createElement('script')
    let done = false
    const w = window as unknown as Record<string, unknown>
    w[callback] = (data: { site_id?: string; script_url?: string; account_email?: string } | null) => {
      done = true
      delete w[callback]
      script.remove()
      if (token !== pollToken) return
      if (data && data.site_id && WIDGET_ID_PATTERN.test(data.site_id)) {
        pollToken++
        show(t.saving, true)
        store(data.site_id, data.account_email || '', data.script_url || '')
      } else {
        setTimeout(check, 1000)
      }
    }
    script.onerror = () => {
      if (!done) {
        script.remove()
        setTimeout(check, 2000)
      }
    }
    script.src = `${origin}/connect-status.js?state=${encodeURIComponent(current)}&cb=${callback}`
    document.head.appendChild(script)
  }
  setTimeout(check, 1000)
}

function connect() {
  state = newState()
  stateRef.value = state
  const w = 800
  const h = 720
  const y = window.outerHeight / 2 + window.screenY - h / 2
  const x = window.outerWidth / 2 + window.screenX - w / 2
  const popup = window.open(
    authUrl.value,
    'asyntai_connect',
    `toolbar=no,location=no,status=no,menubar=no,scrollbars=yes,resizable=yes,width=${w},height=${h},top=${y},left=${x}`,
  )
  show(popup ? t.finishInWindow : t.popupBlocked, !!popup)
  poll()
}

function fallbackClicked() {
  show(t.finishInTab, true)
  poll()
}

async function saveManual() {
  const id = manualId.value.trim()
  if (!WIDGET_ID_PATTERN.test(id)) {
    show(t.invalidId, false)
    return
  }
  await store(id, '', '')
  manualId.value = ''
}

async function disconnect() {
  busy.value = true
  pollToken++
  try {
    await save({ basic: { widgetId: '' }, connection: {} })
    await refresh()
    state = newState()
    stateRef.value = state
    show(t.disconnected, true)
  } catch (e) {
    show(t.saveFailed + String((e as Error)?.message || e), false)
  } finally {
    busy.value = false
  }
}

onMounted(async () => {
  loadSiteUrl()
  try {
    await refresh()
  } catch (e) {
    show(t.saveFailed + String((e as Error)?.message || e), false)
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  pollToken++
})
</script>

<template>
  <div id="asyntai-halo" class="asyntai-tab">
    <h2 class="asyntai-title">{{ t.title }}</h2>
    <p>{{ t.intro }}</p>

    <p v-if="loading">{{ t.loading }}</p>

    <template v-else>
      <div v-if="connected" id="asyntai-connected" class="asyntai-box">
        <p>
          <strong>{{ t.statusLabel }}</strong>
          <span class="asyntai-ok">{{ t.connected }}</span>
          <template v-if="accountEmail"> · {{ t.as }}{{ accountEmail }}</template>
          <br />
          <strong>{{ t.widgetId }}</strong>
          <code id="asyntai-widget-id">{{ widgetId }}</code>
        </p>
        <p v-if="!enabled" class="asyntai-note">{{ t.disabledNote }}</p>
        <p>{{ t.connectedHelp }}</p>
        <div class="asyntai-actions">
          <a :href="dashboardUrl" target="_blank" rel="noopener">
            <VButton type="primary">{{ t.openDashboard }}</VButton>
          </a>
          <a :href="docsUrl" target="_blank" rel="noopener">
            <VButton>{{ t.docs }}</VButton>
          </a>
          <VButton id="asyntai-disconnect" type="danger" :loading="busy" @click="disconnect">
            {{ t.disconnect }}
          </VButton>
        </div>
      </div>

      <template v-else>
        <div id="asyntai-connect" class="asyntai-box">
          <p>
            <strong>{{ t.statusLabel }}</strong>{{ t.notConnected }}
          </p>
          <p>{{ t.connectHelp }}</p>
          <div class="asyntai-actions">
            <VButton id="asyntai-connect-btn" type="primary" :loading="busy" @click="connect">
              {{ t.connect }}
            </VButton>
          </div>
          <p class="asyntai-small">
            {{ t.fallback }}
            <a
              id="asyntai-fallback-link"
              :href="authUrl"
              target="_blank"
              rel="noopener"
              @click="fallbackClicked"
              >{{ t.fallbackLink }}</a
            >
          </p>
        </div>

        <div class="asyntai-box">
          <p><strong>{{ t.manualTitle }}</strong></p>
          <p class="asyntai-small">{{ t.manualHelp }}</p>
          <div class="asyntai-actions">
            <input
              id="asyntai-manual-id"
              v-model="manualId"
              class="asyntai-input"
              type="text"
              placeholder="asyntai_..."
              autocomplete="off"
              spellcheck="false"
              @keydown.enter.prevent="saveManual"
            />
            <VButton id="asyntai-save" :loading="busy" @click="saveManual">{{ t.save }}</VButton>
          </div>
        </div>
      </template>
    </template>

    <p v-if="alert" id="asyntai-alert" class="asyntai-alert" :class="{ 'is-ok': alert.ok }">
      {{ alert.text }}
    </p>
  </div>
</template>

<style scoped>
.asyntai-tab {
  max-width: 760px;
  padding: 16px;
  font-size: 14px;
  line-height: 1.6;
  color: #1f2937;
}
.asyntai-title {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
}
.asyntai-tab p {
  margin: 0 0 10px;
}
.asyntai-box {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 16px 20px;
  margin: 16px 0;
}
.asyntai-box p:last-child {
  margin-bottom: 0;
}
.asyntai-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}
.asyntai-ok {
  color: #059669;
  font-weight: 600;
}
.asyntai-small {
  font-size: 13px;
  color: #6b7280;
}
.asyntai-small a {
  color: #0b0d10;
  text-decoration: underline;
}
.asyntai-note {
  color: #b45309;
}
.asyntai-alert {
  padding: 8px 12px;
  border: 1px solid #f59e0b;
  border-radius: 6px;
}
.asyntai-alert.is-ok {
  border-color: #10b981;
}
.asyntai-input {
  min-width: 280px;
  height: 36px;
  padding: 0 10px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
}
code {
  user-select: all;
}
</style>
