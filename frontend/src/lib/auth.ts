const TOKEN_STORAGE_KEY = "token";
const PROFILE_MAP_STORAGE_KEY = "userProfiles";
const ACTIVE_PROFILE_STORAGE_KEY = "activeProfile";
const SESSION_EVENT = "crypto-auth-session-changed";

function notifySession() {
  window.dispatchEvent(new Event(SESSION_EVENT));
}

export function subscribeAuth(listener: () => void) {
  let timeout: ReturnType<typeof setTimeout> | undefined;
  const changed = () => {
    clearTimeout(timeout);
    const token = window.localStorage.getItem(TOKEN_STORAGE_KEY);
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split(".")[1].replaceAll("-", "+").replaceAll("_", "/")));
        if (typeof payload.exp === "number") {
          const expiresAt = payload.exp * 1000;
          timeout = setTimeout(() => {
            if (window.localStorage.getItem(TOKEN_STORAGE_KEY) !== token) return;
            if (Date.now() >= expiresAt) clearAuthSession();
            else changed();
          }, Math.min(Math.max(0, expiresAt - Date.now()), 2147483647));
        }
      } catch { /* Opaque/malformed tokens are rejected by the API. */ }
    }
    listener();
  };
  const storageChanged = (event: StorageEvent) => {
    if (event.key === null || event.key === TOKEN_STORAGE_KEY || event.key === ACTIVE_PROFILE_STORAGE_KEY) changed();
  };
  window.addEventListener(SESSION_EVENT, changed);
  window.addEventListener("storage", storageChanged);
  // Schedule expiry without synchronously updating React state during effect setup.
  const initial = setTimeout(changed, 0);
  return () => {
    clearTimeout(initial);
    clearTimeout(timeout);
    window.removeEventListener(SESSION_EVENT, changed);
    window.removeEventListener("storage", storageChanged);
  };
}

type ProfileMap = Record<string, string>;

export type ActiveProfile = {
  email: string;
  firstName: string;
};

function isBrowser() {
  return typeof window !== "undefined";
}

function sanitizeEmail(email: string) {
  return email.trim().toLowerCase();
}

function readProfileMap(): ProfileMap {
  if (!isBrowser()) return {};
  const raw = window.localStorage.getItem(PROFILE_MAP_STORAGE_KEY);
  if (!raw) return {};
  try {
    const parsed = JSON.parse(raw);
    return parsed && typeof parsed === "object" ? (parsed as ProfileMap) : {};
  } catch {
    return {};
  }
}

function writeProfileMap(map: ProfileMap) {
  if (!isBrowser()) return;
  window.localStorage.setItem(PROFILE_MAP_STORAGE_KEY, JSON.stringify(map));
}

function deriveFirstName(email: string) {
  const localPart = email.split("@")[0] ?? "";
  if (!localPart) return "there";
  const cleaned = localPart.replace(/[\d_\-.]+/g, " ").trim();
  if (!cleaned) return "there";
  const firstWord = cleaned.split(/\s+/)[0];
  return firstWord.charAt(0).toUpperCase() + firstWord.slice(1);
}

export function saveToken(token: string) {
  if (!isBrowser()) return;
  window.localStorage.setItem(TOKEN_STORAGE_KEY, token);
  notifySession();
}

export function getToken() {
  if (!isBrowser()) return null;
  return window.localStorage.getItem(TOKEN_STORAGE_KEY);
}

export function clearToken() {
  if (!isBrowser()) return;
  window.localStorage.removeItem(TOKEN_STORAGE_KEY);
  notifySession();
}

export function clearAuthSession() {
  if (!isBrowser()) return;
  window.localStorage.removeItem(TOKEN_STORAGE_KEY);
  window.localStorage.removeItem(ACTIVE_PROFILE_STORAGE_KEY);
  notifySession();
}

export function setActiveProfile(email: string, firstName?: string) {
  if (!isBrowser()) return;
  const normalizedEmail = sanitizeEmail(email);
  const map = readProfileMap();
  const resolvedFirstName = firstName?.trim() || map[normalizedEmail] || deriveFirstName(email);

  map[normalizedEmail] = resolvedFirstName;
  writeProfileMap(map);

  window.localStorage.setItem(
    ACTIVE_PROFILE_STORAGE_KEY,
    JSON.stringify({ email: normalizedEmail, firstName: resolvedFirstName })
  );
  notifySession();
}

export function getActiveProfile(): ActiveProfile | null {
  if (!isBrowser()) return null;
  const raw = window.localStorage.getItem(ACTIVE_PROFILE_STORAGE_KEY);
  if (!raw) return null;

  try {
    const parsed = JSON.parse(raw);
    if (!parsed?.email) return null;

    const normalizedEmail = sanitizeEmail(parsed.email);
    const map = readProfileMap();
    const firstName = parsed.firstName || map[normalizedEmail] || deriveFirstName(normalizedEmail);

    return { email: normalizedEmail, firstName };
  } catch {
    return null;
  }
}

export function logout() {
  clearAuthSession();
}

export function authHeader() {
  const token = getToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}
