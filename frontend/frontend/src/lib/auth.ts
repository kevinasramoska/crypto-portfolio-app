const TOKEN_STORAGE_KEY = "token";
const PROFILE_MAP_STORAGE_KEY = "userProfiles";
const ACTIVE_PROFILE_STORAGE_KEY = "activeProfile";

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
}

export function getToken() {
  if (!isBrowser()) return null;
  return window.localStorage.getItem(TOKEN_STORAGE_KEY);
}

export function clearToken() {
  if (!isBrowser()) return;
  window.localStorage.removeItem(TOKEN_STORAGE_KEY);
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
  if (!isBrowser()) return;
  clearToken();
  window.localStorage.removeItem(ACTIVE_PROFILE_STORAGE_KEY);
}

export function authHeader() {
  const token = getToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}
