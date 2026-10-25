import { clearAuthSession, getToken, saveToken, subscribeAuth } from "./auth";

describe("session synchronization", () => {
  beforeEach(() => {
    window.localStorage.clear();
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-10-25T12:00:00Z"));
  });
  afterEach(() => vi.useRealTimers());

  it("notifies the same page and clears expired tokens", () => {
    const listener = vi.fn();
    const unsubscribe = subscribeAuth(listener);
    try {
      const token = `header.${btoa(JSON.stringify({ exp: Date.now() / 1000 + 1 }))}.signature`;
      saveToken(token);
      expect(listener).toHaveBeenCalled();
      expect(getToken()).toBe(token);
      vi.advanceTimersByTime(1001);
      expect(getToken()).toBeNull();
    } finally { unsubscribe(); }
  });

  it("responds to another tab clearing the session", () => {
    saveToken("token");
    const listener = vi.fn();
    const unsubscribe = subscribeAuth(listener);
    try {
      window.localStorage.removeItem("token");
      window.dispatchEvent(new StorageEvent("storage", { key: "token" }));
      expect(listener).toHaveBeenCalledTimes(1);
      expect(getToken()).toBeNull();
      clearAuthSession();
      expect(listener).toHaveBeenCalledTimes(2);
    } finally { unsubscribe(); }
  });
});
